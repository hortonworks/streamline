/**
 * Copyright 2017 Hortonworks.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hortonworks.streamline.storage.impl.jdbc.provider.oracle.factory;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.exception.StorageException;
import com.hortonworks.streamline.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.streamline.storage.impl.jdbc.connection.ConnectionBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.provider.oracle.query.OracleDeleteQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.oracle.query.OracleInsertQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.oracle.query.OracleInsertUpdateDuplicate;
import com.hortonworks.streamline.storage.impl.jdbc.provider.oracle.query.OracleSelectQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.oracle.query.OracleSequenceIdQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.oracle.query.OracleUpdateQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.oracle.statement.OracleDataTypeContext;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.factory.AbstractQueryExecutor;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.SqlQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.statement.PreparedStatementBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.util.CaseAgnosticStringSet;
import com.hortonworks.streamline.storage.impl.jdbc.util.Util;
import com.zaxxer.hikari.HikariConfig;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;


public class OracleExecutor extends AbstractQueryExecutor {

    private static final OracleDataTypeContext ORACLE_DATA_TYPE_CONTEXT = new OracleDataTypeContext();

    public OracleExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder) {
        super(config, connectionBuilder, ORACLE_DATA_TYPE_CONTEXT);
    }

    public OracleExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder, CacheBuilder<SqlQuery, PreparedStatementBuilder> cacheBuilder) {
        super(config, connectionBuilder, cacheBuilder, ORACLE_DATA_TYPE_CONTEXT);
    }


    @Override
    public void insert(Storable storable) {
        executeUpdate(new OracleInsertQuery(storable));
    }

    @Override
    public void insertOrUpdate(final Storable storable) {
        executeUpdate(new OracleInsertUpdateDuplicate(storable));
    }

    public void delete(StorableKey storableKey) {
        executeUpdate(new OracleDeleteQuery(storableKey));
    }

    @Override
    public <T extends Storable> Collection<T> select(final String namespace) {
        return executeQuery(namespace, new OracleSelectQuery(namespace));
    }

    @Override
    public <T extends Storable> Collection<T> select(final StorableKey storableKey) {
        OracleSelectQuery oracleSelectQuery = new OracleSelectQuery(storableKey);
        try {
            return executeQuery(storableKey.getNameSpace(), oracleSelectQuery);
        } catch (StorageException e) {
            log.error("Error while running query : \"{}\"", oracleSelectQuery.getParametrizedSql(), e);
            throw e;
        }
    }

    @Override
    public Long nextId(String namespace) {
        OracleSequenceIdQuery oracleSequenceIdQuery = new OracleSequenceIdQuery(namespace, connectionBuilder, queryTimeoutSecs, ORACLE_DATA_TYPE_CONTEXT);
        return oracleSequenceIdQuery.getNextID();
    }

    @Override
    public CaseAgnosticStringSet getColumnNames(String namespace) throws SQLException {
        CaseAgnosticStringSet columns = new CaseAgnosticStringSet();
        try (Connection connection = getConnection()) {
            final ResultSetMetaData rsMetadata = PreparedStatementBuilder.of(connection, new ExecutionConfig(queryTimeoutSecs), ORACLE_DATA_TYPE_CONTEXT,
                    new OracleSelectQuery(namespace)).getMetaData();
            for (int i = 1; i <= rsMetadata.getColumnCount(); i++) {
                columns.add(rsMetadata.getColumnName(i));
            }
            return columns;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static OracleExecutor createExecutor(Map<String, Object> jdbcProps) {
        Util.validateJDBCProperties(jdbcProps, Lists.newArrayList("dataSourceClassName", "dataSource.url"));

        String dataSourceClassName = (String) jdbcProps.get("dataSourceClassName");
        log.info("data source class: [{}]", dataSourceClassName);

        String jdbcUrl = (String) jdbcProps.get("dataSource.url");
        log.info("dataSource.url is: [{}] ", jdbcUrl);

        int queryTimeOutInSecs = -1;
        if (jdbcProps.containsKey("queryTimeoutInSecs")) {
            queryTimeOutInSecs = (Integer) jdbcProps.get("queryTimeoutInSecs");
            if (queryTimeOutInSecs < 0) {
                throw new IllegalArgumentException("queryTimeoutInSecs property can not be negative");
            }
        }

        Properties properties = new Properties();
        properties.putAll(jdbcProps);
        HikariConfig hikariConfig = new HikariConfig(properties);

        HikariCPConnectionBuilder connectionBuilder = new HikariCPConnectionBuilder(hikariConfig);
        ExecutionConfig executionConfig = new ExecutionConfig(queryTimeOutInSecs);
        return new OracleExecutor(executionConfig, connectionBuilder);
    }
}
