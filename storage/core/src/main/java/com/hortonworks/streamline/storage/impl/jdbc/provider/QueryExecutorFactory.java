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

package com.hortonworks.streamline.storage.impl.jdbc.provider;

import com.google.common.collect.Lists;
import com.hortonworks.streamline.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.streamline.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.factory.MySqlExecutor;
import com.hortonworks.streamline.storage.impl.jdbc.provider.oracle.factory.OracleExecutor;
import com.hortonworks.streamline.storage.impl.jdbc.provider.postgresql.factory.PostgresqlExecutor;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.factory.QueryExecutor;
import com.hortonworks.streamline.storage.impl.jdbc.util.Util;
import com.zaxxer.hikari.HikariConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

public class QueryExecutorFactory {

    private static final Logger LOG = LoggerFactory.getLogger(QueryExecutorFactory.class);

    private QueryExecutorFactory() {

    }

    public static QueryExecutor get(String type, Map<String, Object> dbProperties) {

        HikariCPConnectionBuilder connectionBuilder = getHikariCPConnnectionBuilder(dbProperties);
        ExecutionConfig executionConfig = getExecutionConfig(dbProperties);

        QueryExecutor queryExecutor = null;
        switch (type) {
            case "mysql":
                queryExecutor = new MySqlExecutor(executionConfig, connectionBuilder);
                break;
            case "postgresql":
                queryExecutor = new PostgresqlExecutor(executionConfig, connectionBuilder);
                break;
            case "oracle":
                queryExecutor = new OracleExecutor(executionConfig, connectionBuilder);
                break;
            default:
                throw new IllegalArgumentException("Unsupported storage provider type: " + type);
        }

        return queryExecutor;
    }

    private static HikariCPConnectionBuilder getHikariCPConnnectionBuilder(Map<String, Object> dbProperties ) {
        Util.validateJDBCProperties(dbProperties, Lists.newArrayList("dataSourceClassName", "dataSource.url"));

        String dataSourceClassName = (String) dbProperties.get("dataSourceClassName");
        LOG.info("data source class: [{}]", dataSourceClassName);

        String jdbcUrl = (String) dbProperties.get("dataSource.url");
        LOG.info("dataSource.url is: [{}] ", jdbcUrl);

        Properties properties = new Properties();
        properties.putAll(dbProperties);
        HikariConfig hikariConfig = new HikariConfig(properties);

        return new HikariCPConnectionBuilder(hikariConfig);
    }

    private static ExecutionConfig getExecutionConfig(Map<String, Object> dbProperties) {
        int queryTimeOutInSecs = -1;
        if (dbProperties.containsKey("queryTimeoutInSecs")) {
            queryTimeOutInSecs = (Integer) dbProperties.get("queryTimeoutInSecs");
            if (queryTimeOutInSecs < 0) {
                throw new IllegalArgumentException("queryTimeoutInSecs property can not be negative");
            }
        }

        return new ExecutionConfig(queryTimeOutInSecs);
    }
}
