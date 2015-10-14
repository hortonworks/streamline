package com.hortonworks.iotas.storage.impl.jdbc.provider.sql.factory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.Device;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.exception.NonIncrementalColumnException;
import com.hortonworks.iotas.storage.exception.StorageException;
import com.hortonworks.iotas.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.iotas.storage.impl.jdbc.connection.ConnectionBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query.SqlDeleteQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query.SqlInsertQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query.SqlQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query.SqlSelectQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.statement.PreparedStatementBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.util.Util;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 *
 */
public class ProviderQueryExecutor implements QueryExecutor {

    protected final ExecutionConfig config;
    protected final int queryTimeoutSecs;
    protected final ConnectionBuilder connectionBuilder;
    protected final List<Connection> activeConnections;

    private final Cache<SqlQuery, PreparedStatementBuilder> cache;

    public ProviderQueryExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder) {
        this(config, connectionBuilder, null);
    }

    public ProviderQueryExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder, CacheBuilder<SqlQuery, PreparedStatementBuilder> cacheBuilder) {
        this.connectionBuilder = connectionBuilder;
        this.config = config;
        cache = cacheBuilder != null ? buildCache(cacheBuilder) : null;
        this.queryTimeoutSecs = config.getQueryTimeoutSecs();
        activeConnections = Collections.synchronizedList(new ArrayList<Connection>());
    }

    @Override
    public void insert(Storable storable) {
        executeUpdate(new SqlInsertQuery(storable));
    }

    @Override
    public void insertOrUpdate(Storable storable) {
        throw new UnsupportedOperationException("insert or update operation is not supported.");
    }

    @Override
    public void delete(StorableKey storableKey) {
        executeUpdate(new SqlDeleteQuery(storableKey));
    }

    @Override
    public <T extends Storable> Collection<T> select(final String namespace) {
        return executeQuery(namespace, new SqlSelectQuery(namespace));
    }

    @Override
    public <T extends Storable> Collection<T> select(final StorableKey storableKey){
        return executeQuery(storableKey.getNameSpace(), new SqlSelectQuery(storableKey));
    }

    @Override
    public Long nextId(String namespace) {
        throw new NonIncrementalColumnException();
    }

    public ExecutionConfig getConfig() {
        return config;
    }

    @Override
    public Connection getConnection() {
        Connection connection = connectionBuilder.getConnection();
        log.debug("Opened connection {}", connection);
        activeConnections.add(connection);
        return connection;
    }

    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                log.debug("Closed connection {}", connection);
                activeConnections.remove(connection);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to close connection", e);
            }
        }
    }

    public void cleanup() {
        if (isCacheEnabled()) {
            cache.invalidateAll();
        } else {
            closeAllOpenConnections();
        }
    }

    private boolean isCacheEnabled() {
        return cache != null;
    }


    private void closeAllOpenConnections() {
        for(Iterator<Connection> iter = activeConnections.iterator(); iter.hasNext(); ) {
            Connection connection = iter.next();
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    iter.remove();
                    log.debug("Closed connection {}", connection);
                }
            } catch (SQLException e) {
                log.error("Failed to close connection [{}]", connection, e);
            }
        }
    }

    private Cache<SqlQuery, PreparedStatementBuilder> buildCache(CacheBuilder<SqlQuery, PreparedStatementBuilder> cacheBuilder) {
        return cacheBuilder.removalListener(new RemovalListener<SqlQuery, PreparedStatementBuilder>() {
            /** Closes and removes the database connection when the entry is removed from cache */
            @Override
            public void onRemoval(RemovalNotification<SqlQuery, PreparedStatementBuilder> notification) {
                final PreparedStatementBuilder val = notification.getValue();
                log.debug("Removing entry from cache and closing connection [key:{}, val: {}]", notification.getKey(), val);
                log.debug("Cache size: {}", cache.size());
                if (val != null) {
                    closeConnection(val.getConnection());;
                }
            }
        }).build();
    }


    // =============== Private helper Methods ===============

    protected void executeUpdate(SqlQuery sqlBuilder) {
        new QueryExecution(sqlBuilder).executeUpdate();
    }

    protected <T extends Storable> Collection<T> executeQuery(String namespace, SqlQuery sqlBuilder) {
        return new QueryExecution(sqlBuilder).executeQuery(namespace);
    }

    protected class QueryExecution {
        private SqlQuery sqlBuilder;
        private Connection connection;

        public QueryExecution(SqlQuery sqlBuilder) {
            this.sqlBuilder = sqlBuilder;
        }

        <T extends Storable> Collection<T> executeQuery(String namespace) {
            Collection<T> result;
            try {
                ResultSet resultSet = getPreparedStatement().executeQuery();
                result = getStorablesFromResultSet(resultSet, namespace);
            } catch (SQLException | ExecutionException e) {
                throw new StorageException(e);
            } finally {
                // Close every opened connection if not using cache. If using cache, cache expiry manages connections
                if (!isCacheEnabled()) {
                    closeConn();
                }
            }
            return result;
        }

        void executeUpdate() {
            try {
                getPreparedStatement().executeUpdate();
            } catch (SQLException | ExecutionException e) {
                throw new StorageException(e);
            } finally {
                // Close every opened connection if not using cache. If using cache, cache expiry manages connections
                if (!isCacheEnabled()) {
                    closeConn();
                }
            }
        }

        void closeConn() {
            closeConnection(connection);
        }

        // ====== private helper methods ======

        private PreparedStatement getPreparedStatement() throws ExecutionException, SQLException {
            PreparedStatementBuilder preparedStatementBuilder = null;

            if (isCacheEnabled()) {
                preparedStatementBuilder = cache.get(sqlBuilder, new PreparedStatementBuilderCallable(sqlBuilder));
            } else {
                connection = getConnection();
                preparedStatementBuilder = new PreparedStatementBuilder(connection, config, sqlBuilder);
            }
            return preparedStatementBuilder.getPreparedStatement(sqlBuilder);
        }

        /** This callable is instantiated and called the first time every key:val entry is inserted into the cache */
        private class PreparedStatementBuilderCallable implements Callable<PreparedStatementBuilder> {
            private final SqlQuery sqlBuilder;

            public PreparedStatementBuilderCallable(SqlQuery sqlBuilder) {
                this.sqlBuilder = sqlBuilder;
            }

            @Override
            public PreparedStatementBuilder call() throws Exception {
                // opens a new connection which remains open for as long as this entry is in the cache
                final PreparedStatementBuilder preparedStatementBuilder =
                        new PreparedStatementBuilder(getConnection(), config, sqlBuilder);
                log.debug("Loading cache with [key: {}, val: {}]", sqlBuilder, preparedStatementBuilder);
                return preparedStatementBuilder;
            }
        }

        private <T extends Storable> Collection<T> getStorablesFromResultSet(ResultSet resultSet, String nameSpace) {
            final Collection<T> storables = new ArrayList<>();
            // maps contains the data to populate the state of Storable objects
            final List<Map<String, Object>> maps = getMapsFromResultSet(resultSet);
            if (maps != null && !maps.isEmpty()) {
                for (Map<String, Object> map : maps) {
                    if (map != null) {
                        T storable = newStorableInstance(nameSpace);
                        storable.fromMap(map);      // populates the Storable object state
                        storables.add(storable);
                    }
                }
            }
            return storables;
        }

        // returns null for empty ResultSet or ResultSet with no rows
        private List<Map<String, Object>> getMapsFromResultSet(ResultSet resultSet) {
            List<Map<String, Object>> maps = null;

            try {
                boolean next = resultSet.next();
                if(next) {
                    maps = new LinkedList<>();
                    ResultSetMetaData rsMetadata = resultSet.getMetaData();
                    do {
                        Map<String, Object> map = newMapWithRowContents(resultSet, rsMetadata);
                        maps.add(map);
                    } while(resultSet.next());
                }
            } catch (SQLException e) {
                log.error("Exception occurred while processing result set.", e);
            }
            return maps;
        }

        private <T extends Storable> T newStorableInstance(String nameSpace) {
            switch (nameSpace) {
                case(DataFeed.NAME_SPACE):
                    return (T) new DataFeed();
                case(DataSource.NAME_SPACE):
                    return (T) new DataSource();
                case(Device.NAME_SPACE):
                    return (T) new Device();
                case(ParserInfo.NAME_SPACE):
                    return (T) new ParserInfo();
                default:
                    throw new RuntimeException("Unsupported Storable type");
            }
        }

        private Map<String, Object> newMapWithRowContents(ResultSet resultSet, ResultSetMetaData rsMetadata) throws SQLException {
            final Map<String, Object> map = new HashMap<>();
            final int columnCount = rsMetadata.getColumnCount();

            for (int i = 1 ; i <= columnCount; i++) {
                final String columnLabel = rsMetadata.getColumnLabel(i);
                final int columnType = rsMetadata.getColumnType(i);
                final Class columnJavaType = Util.getJavaType(columnType);

                if (columnJavaType.equals(String.class)) {
                    map.put(columnLabel, resultSet.getString(columnLabel));
                } else if (columnJavaType.equals(Integer.class)) {
                    map.put(columnLabel, resultSet.getInt(columnLabel));
                } else if (columnJavaType.equals(Double.class)) {
                    map.put(columnLabel, resultSet.getDouble(columnLabel));
                } else if (columnJavaType.equals(Float.class)) {
                    map.put(columnLabel, resultSet.getFloat(columnLabel));
                } else if (columnJavaType.equals(Short.class)) {
                    map.put(columnLabel, resultSet.getShort(columnLabel));
                } else if (columnJavaType.equals(Boolean.class)) {
                    map.put(columnLabel, resultSet.getBoolean(columnLabel));
                } else if (columnJavaType.equals(byte[].class)) {
                    map.put(columnLabel, resultSet.getBytes(columnLabel));
                } else if (columnJavaType.equals(Long.class)) {
                    map.put(columnLabel, resultSet.getLong(columnLabel));
                } else if (columnJavaType.equals(Date.class)) {
                    map.put(columnLabel, resultSet.getDate(columnLabel));
                } else if (columnJavaType.equals(Time.class)) {
                    map.put(columnLabel, resultSet.getTime(columnLabel));
                } else if (columnJavaType.equals(Timestamp.class)) {
                    map.put(columnLabel, resultSet.getTimestamp(columnLabel));
                } else {
                    throw new StorageException("type =  [" + columnType + "] for column [" + columnLabel + "] not supported.");
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Row for ResultSet [{}] with metadata [{}] generated Map [{}]", resultSet, rsMetadata, map);
            }
            return map;
        }
    }

}
