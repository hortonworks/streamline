/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.iotas.storage.impl.jdbc.provider.mysql.factory;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.exception.StorageException;
import com.hortonworks.iotas.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.iotas.storage.impl.jdbc.connection.ConnectionBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.provider.mysql.query.MySqlInsertUpdateDuplicate;
import com.hortonworks.iotas.storage.impl.jdbc.provider.mysql.query.MySqlQueryUtils;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.factory.AbstractQueryExecutor;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query.SqlInsertQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query.SqlQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.statement.PreparedStatementBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.util.Util;
import com.zaxxer.hikari.HikariConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

public class MySqlExecutor extends AbstractQueryExecutor {

    /**
     * @param config Object that contains arbitrary configuration that may be needed for any of the steps of the query execution process
     * @param connectionBuilder Object that establishes the connection to the database
     */
    public MySqlExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder) {
        super(config, connectionBuilder);
    }

    /**
     * @param config Object that contains arbitrary configuration that may be needed for any of the steps of the query execution process
     * @param connectionBuilder Object that establishes the connection to the database
     * @param cacheBuilder Guava cache configuration. The maximum number of entries in cache (open connections)
 *                     must not exceed the maximum number of open database connections allowed
     */
    public MySqlExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder, CacheBuilder<SqlQuery, PreparedStatementBuilder> cacheBuilder) {
        super(config, connectionBuilder, cacheBuilder);
    }

    // ============= Public API methods =============

    @Override
    public void insert(Storable storable) {
        executeUpdate(new SqlInsertQuery(storable));
    }

    @Override
    public void insertOrUpdate(final Storable storable) {
        executeUpdate(new MySqlInsertUpdateDuplicate(storable));
    }

    @Override
    public Long nextId(String namespace) {
        // This only works if the table has auto-increment. The TABLE_SCHEMA part is implicitly specified in the Connection object
        // SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'temp' AND TABLE_SCHEMA = 'test'
        Connection connection = null;
        try {
            connection = getConnection();
            return getNextId(connection, namespace);
        } catch (SQLException e) {
            throw new StorageException(e);
        } finally {
            closeConnection(connection);
        }
    }

    // Protected to be able to override it in the test framework
    protected Long getNextId(Connection connection, String namespace) throws SQLException {
        return MySqlQueryUtils.nextIdMySql(connection, namespace, queryTimeoutSecs);
    }

    public static MySqlExecutor createExecutor(Map<String, Object> jdbcProps) {
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
        return new MySqlExecutor(executionConfig, connectionBuilder);
    }

}
