/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.storage.impl.jdbc.provider.sql.factory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableFactory;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.exception.StorageException;
import com.hortonworks.streamline.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.streamline.storage.impl.jdbc.connection.ConnectionBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.SqlDeleteQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.SqlQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.SqlSelectQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.statement.DefaultStorageDataTypeContext;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.statement.PreparedStatementBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.statement.StorageDataTypeContext;
import com.hortonworks.streamline.storage.impl.jdbc.util.CaseAgnosticStringSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 *
 */
public abstract class AbstractQueryExecutor implements QueryExecutor {

    protected final ExecutionConfig config;
    protected final int queryTimeoutSecs;
    protected final ConnectionBuilder connectionBuilder;
    protected final List<Connection> activeConnections;
    protected final StorageDataTypeContext storageDataTypeContext;

    private final Cache<SqlQuery, PreparedStatementBuilder> cache;
    private StorableFactory storableFactory;

    public AbstractQueryExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder) {
        this(config, connectionBuilder, null, new DefaultStorageDataTypeContext());
    }

    public AbstractQueryExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder, CacheBuilder<SqlQuery, PreparedStatementBuilder> cacheBuilder) {
        this(config, connectionBuilder, cacheBuilder, new DefaultStorageDataTypeContext());
    }

    public AbstractQueryExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder, StorageDataTypeContext storageDataTypeContext) {
        this(config, connectionBuilder, null, storageDataTypeContext);
    }

    public AbstractQueryExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder, CacheBuilder<SqlQuery, PreparedStatementBuilder> cacheBuilder, StorageDataTypeContext storageDataTypeContext) {
        this.connectionBuilder = connectionBuilder;
        this.config = config;
        cache = cacheBuilder != null ? buildCache(cacheBuilder) : null;
        this.queryTimeoutSecs = config.getQueryTimeoutSecs();
        this.storageDataTypeContext = storageDataTypeContext;
        activeConnections = Collections.synchronizedList(new ArrayList<Connection>());
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

    public abstract Long nextId(String namespace);

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

    @Override
    public CaseAgnosticStringSet getColumnNames(String namespace) throws SQLException {
        CaseAgnosticStringSet columns = new CaseAgnosticStringSet();
        try(Connection connection = getConnection()) {
            final ResultSetMetaData rsMetadata = PreparedStatementBuilder.of(connection, new ExecutionConfig(queryTimeoutSecs), storageDataTypeContext,
                    new SqlSelectQuery(namespace)).getMetaData();
            for (int i = 1; i <= rsMetadata.getColumnCount(); i++) {
                columns.add(rsMetadata.getColumnName(i));
            }
            return columns;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
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
                    closeConnection(val.getConnection());
                }
            }
        }).build();
    }

    @Override
    public void setStorableFactory(StorableFactory storableFactory) {
        if(this.storableFactory != null) {
            throw new IllegalStateException("StorableFactory is already set");
        }

        this.storableFactory = storableFactory;
    }


    // =============== Private helper Methods ===============

    protected void executeUpdate(SqlQuery sqlBuilder) {
        getQueryExecution(sqlBuilder).executeUpdate();
    }

    protected Long executeUpdateWithReturningGeneratedKey(SqlQuery sqlBuilder) {
        return getQueryExecution(sqlBuilder).executeUpdateWithReturningGeneratedKey();
    }

    protected <T extends Storable> Collection<T> executeQuery(String namespace, SqlQuery sqlBuilder) {
        return getQueryExecution(sqlBuilder).executeQuery(namespace);
    }

    protected QueryExecution getQueryExecution(SqlQuery sqlQuery) {
        return new QueryExecution(sqlQuery);
    }

    protected class QueryExecution {
        private final SqlQuery sqlBuilder;
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

        Long executeUpdateWithReturningGeneratedKey() {
            try {
                PreparedStatement pstmt = getPreparedStatementWithSetReturningGeneratedKey();
                pstmt.executeUpdate();
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    return null;
                }
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
                preparedStatementBuilder = cache.get(sqlBuilder, new PreparedStatementBuilderCallable(sqlBuilder, false));
            } else {
                connection = getConnection();
                log.debug("sqlBuilder {}", sqlBuilder.toString());
                preparedStatementBuilder = PreparedStatementBuilder.of(connection, config, storageDataTypeContext, sqlBuilder);
            }
            return preparedStatementBuilder.getPreparedStatement(sqlBuilder);
        }

        private PreparedStatement getPreparedStatementWithSetReturningGeneratedKey() throws ExecutionException, SQLException {
            PreparedStatementBuilder preparedStatementBuilder = null;

            if (isCacheEnabled()) {
                preparedStatementBuilder = cache.get(sqlBuilder, new PreparedStatementBuilderCallable(sqlBuilder, true));
            } else {
                connection = getConnection();
                preparedStatementBuilder = PreparedStatementBuilder.supportReturnGeneratedKeys(connection, config, storageDataTypeContext, sqlBuilder);
            }
            return preparedStatementBuilder.getPreparedStatement(sqlBuilder);
        }

        /** This callable is instantiated and called the first time every key:val entry is inserted into the cache */
        private class PreparedStatementBuilderCallable implements Callable<PreparedStatementBuilder> {
            private final SqlQuery sqlBuilder;
            private final boolean returnGeneratedKeys;

            private PreparedStatementBuilderCallable(SqlQuery sqlBuilder, boolean returnGeneratedKeys) {
                this.sqlBuilder = sqlBuilder;
                this.returnGeneratedKeys = returnGeneratedKeys;
            }

            public PreparedStatementBuilderCallable of(SqlQuery sqlBuilder) {
                return new PreparedStatementBuilderCallable(sqlBuilder, false);
            }

            public PreparedStatementBuilderCallable supportReturnGeneratedKeys(SqlQuery sqlBuilder) {
                return new PreparedStatementBuilderCallable(sqlBuilder, true);
            }

            @Override
            public PreparedStatementBuilder call() throws Exception {
                // opens a new connection which remains open for as long as this entry is in the cache
                final PreparedStatementBuilder preparedStatementBuilder;
                if (returnGeneratedKeys) {
                    preparedStatementBuilder = PreparedStatementBuilder.supportReturnGeneratedKeys(getConnection(), config, storageDataTypeContext, sqlBuilder);
                } else {
                    preparedStatementBuilder = PreparedStatementBuilder.of(getConnection(), config, storageDataTypeContext, sqlBuilder);
                }
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
        protected List<Map<String, Object>> getMapsFromResultSet(ResultSet resultSet) {
            List<Map<String, Object>> maps = null;

            try {
                boolean next = resultSet.next();
                if(next) {
                    maps = new LinkedList<>();
                    ResultSetMetaData rsMetadata = resultSet.getMetaData();
                    do {
                        Map<String, Object> map = storageDataTypeContext.getMapWithRowContents(resultSet, rsMetadata);
                        maps.add(map);
                    } while(resultSet.next());
                }
            } catch (SQLException e) {
                log.error("Exception occurred while processing result set.", e);
            }
            return maps;
        }

        private <T extends Storable> T newStorableInstance(String nameSpace) {
            return (T) storableFactory.create(nameSpace);
        }
    }

}
