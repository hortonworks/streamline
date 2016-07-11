package com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.factory;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.iotas.storage.impl.jdbc.connection.ConnectionBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.JdbcClient;
import com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.query.PhoenixDeleteQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.query.PhoenixSequenceIdQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.query.PhoenixSelectQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.query.PhoenixUpsertQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.factory.AbstractQueryExecutor;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query.SqlQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.statement.PreparedStatementBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.util.Util;
import com.zaxxer.hikari.HikariConfig;

import java.util.Collection;
import java.util.Map;

/**
 * SQL query executor for Phoenix
 */
public class PhoenixExecutor extends AbstractQueryExecutor {

    public PhoenixExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder) {
        super(config, connectionBuilder);
    }

    public PhoenixExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder, CacheBuilder<SqlQuery, PreparedStatementBuilder> cacheBuilder) {
        super(config, connectionBuilder, cacheBuilder);
    }

    @Override
    public void insert(Storable storable) {
        insertOrUpdate(storable);
    }

    @Override
    public void insertOrUpdate(Storable storable) {
        executeUpdate(new PhoenixUpsertQuery(storable));
    }

    @Override
    public <T extends Storable> Collection<T> select(String namespace) {
        return executeQuery(namespace, new PhoenixSelectQuery(namespace));
    }

    @Override
    public <T extends Storable> Collection<T> select(StorableKey storableKey) {
        return executeQuery(storableKey.getNameSpace(), new PhoenixSelectQuery(storableKey));
    }

    @Override
    public void delete(StorableKey storableKey) {
        executeUpdate(new PhoenixDeleteQuery(storableKey));
    }

    @Override
    public Long nextId(String namespace) {
        PhoenixSequenceIdQuery phoenixSequenceIdQuery = new PhoenixSequenceIdQuery(namespace, connectionBuilder, queryTimeoutSecs);
        return phoenixSequenceIdQuery.getNextID();
    }

    public static PhoenixExecutor createExecutor(Map<String, Object> jdbcProps) throws Exception {
        Util.validateJDBCProperties(jdbcProps, Lists.newArrayList("jdbcDriverClass", "jdbcUrl"));

        String driverClassName = (String) jdbcProps.get("jdbcDriverClass");
        log.info("jdbc driver class: [{}]", driverClassName);
        Class.forName(driverClassName);

        String jdbcUrl = (String) jdbcProps.get("jdbcUrl");
        log.info("jdbc url is: [{}] ", jdbcUrl);

        int queryTimeOutInSecs = -1;
        if(jdbcProps.containsKey("queryTimeoutInSecs")) {
            queryTimeOutInSecs = (Integer) jdbcProps.get("queryTimeoutInSecs");
            if(queryTimeOutInSecs < 0) {
                throw new IllegalArgumentException("queryTimeoutInSecs property can not be negative");
            }
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);

        JdbcClient jdbcClient = new JdbcClient(jdbcUrl);
        log.info("creating tables");
        String createPath = "phoenix/create_tables.sql";
        jdbcClient.runScript(createPath);

        final HikariCPConnectionBuilder connectionBuilder = new HikariCPConnectionBuilder(hikariConfig);
        final ExecutionConfig executionConfig = new ExecutionConfig(queryTimeOutInSecs);
        CacheBuilder cacheBuilder = null;
        if(jdbcProps.containsKey("cacheSize")) {
            cacheBuilder = CacheBuilder.newBuilder().maximumSize((Integer)jdbcProps.get("cacheSize"));
        }
        return new PhoenixExecutor(executionConfig, connectionBuilder, cacheBuilder);
    }

}
