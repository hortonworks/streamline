package org.apache.streamline.storage.impl.jdbc.provider.postgres.factory;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import org.apache.streamline.storage.Storable;
import org.apache.streamline.storage.impl.jdbc.config.ExecutionConfig;
import org.apache.streamline.storage.impl.jdbc.connection.ConnectionBuilder;
import org.apache.streamline.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import org.apache.streamline.storage.impl.jdbc.provider.mysql.query.MySqlInsertUpdateDuplicate;
import org.apache.streamline.storage.impl.jdbc.provider.sql.factory.AbstractQueryExecutor;
import org.apache.streamline.storage.impl.jdbc.provider.sql.query.SqlInsertQuery;
import org.apache.streamline.storage.impl.jdbc.provider.sql.query.SqlQuery;
import org.apache.streamline.storage.impl.jdbc.provider.sql.statement.PreparedStatementBuilder;
import org.apache.streamline.storage.impl.jdbc.util.Util;
import com.zaxxer.hikari.HikariConfig;

import java.util.Map;
import java.util.Properties;

/**
 * SQL query executor for MySQL DB.
 *
 * To issue the new ID to insert and get auto issued key in concurrent manner, PostgresExecutor utilizes Postgres's
 * SERIAL feature
 *
 * If the value of id is null, we let Postgres issue new ID and get the new ID. If the value of id is not null, we just use that value.
 */
public class PostgresExecutor extends AbstractQueryExecutor {

    /**
     * @param config Object that contains arbitrary configuration that may be needed for any of the steps of the query execution process
     * @param connectionBuilder Object that establishes the connection to the database
     */
    public PostgresExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder) {
        super(config, connectionBuilder);
    }

    /**
     * @param config Object that contains arbitrary configuration that may be needed for any of the steps of the query execution process
     * @param connectionBuilder Object that establishes the connection to the database
     * @param cacheBuilder Guava cache configuration. The maximum number of entries in cache (open connections)
     *                     must not exceed the maximum number of open database connections allowed
     */
    public PostgresExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder, CacheBuilder<SqlQuery, PreparedStatementBuilder> cacheBuilder) {
        super(config, connectionBuilder, cacheBuilder);
    }

    // ============= Public API methods =============

    @Override
    public void insert(Storable storable) {
        insertOrUpdateWithUniqueId(storable, new SqlInsertQuery(storable));
    }

    @Override
    public void insertOrUpdate(final Storable storable) {
        insertOrUpdateWithUniqueId(storable, new MySqlInsertUpdateDuplicate(storable));
    }

    @Override
    public Long nextId(String namespace) {
        // We intentionally return null. Please refer the class javadoc for more details.
        return null;
    }

    public static PostgresExecutor createExecutor(Map<String, Object> jdbcProps) {
        Util.validateJDBCProperties(jdbcProps, Lists.newArrayList("dataSourceClassName", "dataSource.url"));

        String dataSourceClassName = (String) jdbcProps.get("dataSourceClassName");
        log.info("data source class: [{}]", dataSourceClassName);

        String jdbcUrl = (String) jdbcProps.get("dataSource.url");
        log.info("dataSource.url is: [{}] ", jdbcUrl);

        int queryTimeOutInSecs = -1;
        if(jdbcProps.containsKey("queryTimeoutInSecs")) {
            queryTimeOutInSecs = (Integer) jdbcProps.get("queryTimeoutInSecs");
            if(queryTimeOutInSecs < 0) {
                throw new IllegalArgumentException("queryTimeoutInSecs property can not be negative");
            }
        }

        Properties properties = new Properties();
        properties.putAll(jdbcProps);
        HikariConfig hikariConfig = new HikariConfig(properties);

        HikariCPConnectionBuilder connectionBuilder = new HikariCPConnectionBuilder(hikariConfig);
        ExecutionConfig executionConfig = new ExecutionConfig(queryTimeOutInSecs);
        return new PostgresExecutor(executionConfig, connectionBuilder);
    }

    private void insertOrUpdateWithUniqueId(final Storable storable, final SqlQuery sqlQuery) {
        try {
            Long id = storable.getId();
            if (id == null) {
                id = executeUpdateWithReturningGeneratedKey(sqlQuery);
                storable.setId(id);
            } else {
                executeUpdate(sqlQuery);
            }
        } catch (UnsupportedOperationException e) {
            executeUpdate(sqlQuery);
        }
    }

}