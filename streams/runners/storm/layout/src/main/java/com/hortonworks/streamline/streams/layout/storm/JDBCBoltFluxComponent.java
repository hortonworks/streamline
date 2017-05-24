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
package com.hortonworks.streamline.streams.layout.storm;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Flux component for JDBC sinks
 */
@SuppressWarnings("unchecked")
public class JDBCBoltFluxComponent extends AbstractFluxComponent {
    private static final String KEY_TABLE_NAME = "tableName";
    private static final String KEY_COLUMNS = "columns";
    private static final String KEY_DB_TYPE = "dbType";
    private static final String KEY_DRIVER_CLASSNAME = "driverClassName";
    private static final String KEY_JDBC_URL = "jdbcUrl";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String PHOENIX = "phoenix";
    private static final String MYSQL = "mysql";
    private static final String POSTGRESQL = "postgresql";

    @Override
    protected void generateComponent() {
        String boltId = "jdbcInsertBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.streamline.streams.runtime.storm.bolt.jdbc.StreamlineJdbcInsertBolt";

        String dbType = (String) conf.get(KEY_DB_TYPE);
        String tableName = (String) conf.get(KEY_TABLE_NAME);
        List<String> columns = (List<String>) conf.get(KEY_COLUMNS);

        DatabaseUpsertSupport upsertSupport = getDatabaseUpsertSupport(dbType);
        UpsertQueryInfo queryInfo = upsertSupport.buildUpsertQuery(tableName, columns);

        List<Object> constructorArgs = new ArrayList<>();
        List<Map<String, Object>> configMethods = new ArrayList<>();
        String connectionProviderId = getConnectionProvider();
        addArg(constructorArgs, getRefYaml(connectionProviderId));
        addArg(constructorArgs, getRefYaml(getJdbcMapper(connectionProviderId, queryInfo.getColumnList())));

        Map<String, Object> withInsertQuery = new LinkedHashMap<>();
        withInsertQuery.put(StormTopologyLayoutConstants.YAML_KEY_NAME, "withInsertQuery");
        withInsertQuery.put(StormTopologyLayoutConstants.YAML_KEY_ARGS, Arrays.asList(queryInfo.getQuery()));

        configMethods.add(withInsertQuery);
        component = createComponent(boltId, boltClassName, null, constructorArgs, configMethods);
        addParallelismToComponent();
    }

    private DatabaseUpsertSupport getDatabaseUpsertSupport(String dbType) {
        switch (dbType) {
            case MYSQL:
                return new MySQLUpsertSupport();
            case POSTGRESQL:
                return new PostgreSQLUpsertSupport();
            case PHOENIX:
                return new PhoenixUpsertSupport();
            default:
                throw new IllegalArgumentException("Not supported DB type: " + dbType);
        }
    }

    private String getConnectionProvider() {
        String componentId = "ConnectionProvider" + UUID_FOR_COMPONENTS;
        List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, getRefYaml(getHikariCPConfigMap()));
        String className = "org.apache.storm.jdbc.common.HikariCPConnectionProvider";
        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }

    private String getHikariCPConfigMap() {
        String componentId = "HikariCpConfigMap" + UUID_FOR_COMPONENTS;
        List<Map<String, Object>> configMethods = new ArrayList<>();
        put(configMethods, KEY_DRIVER_CLASSNAME);
        put(configMethods, KEY_JDBC_URL);
        put(configMethods, KEY_USERNAME);
        put(configMethods, KEY_PASSWORD);
        String className = "java.util.HashMap";
        addToComponents(createComponent(componentId, className, null, null, configMethods));
        return componentId;
    }

    private void put(List<Map<String, Object>> configMethods, String key) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(StormTopologyLayoutConstants.YAML_KEY_NAME, "put");
        String val = (String) conf.get(key);
        map.put(StormTopologyLayoutConstants.YAML_KEY_ARGS, Arrays.asList(key, val));
        configMethods.add(map);
    }

    private void add(List<Map<String, Object>> configMethods, String column) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(StormTopologyLayoutConstants.YAML_KEY_NAME, "add");
        map.put(StormTopologyLayoutConstants.YAML_KEY_ARGS, Collections.singletonList(column));
        configMethods.add(map);
    }

    private String getColumnList(List<String> columns) {
        String componentId = "ColumnList" + UUID_FOR_COMPONENTS;
        List<Map<String, Object>> configMethods = new ArrayList<>();
        columns.forEach(
            column -> add(configMethods, column)
        );
        String className = "java.util.ArrayList";
        addToComponents(createComponent(componentId, className, null, null, configMethods));
        return componentId;
    }

    private String getJdbcMapper(String connectionProviderId, List<String> columns) {
        String componentId = "JdbcMapper" + UUID_FOR_COMPONENTS;
        List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, KEY_TABLE_NAME);
        addArg(constructorArgs, getRefYaml(connectionProviderId));
        String columnListId = getColumnList(columns);
        addArg(constructorArgs, getRefYaml(columnListId));
        String className = "com.hortonworks.streamline.streams.runtime.storm.bolt.StreamlineJdbcMapper";
        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }

    static class UpsertQueryInfo {
        private final String query;
        private final List<String> columnList;

        UpsertQueryInfo(String query, List<String> columnList) {
            this.query = query;
            this.columnList = columnList;
        }

        String getQuery() {
            return query;
        }

        List<String> getColumnList() {
            return columnList;
        }
    }

    interface DatabaseUpsertSupport {
        UpsertQueryInfo buildUpsertQuery(String tableName, List<String> columns);

        /**
         * @param num number of times to repeat the pattern
         * @return bind variables repeated num times
         */
        default String getBindVariables(String pattern, int num) {
            return StringUtils.chop(StringUtils.repeat(pattern, num));
        }

        /**
         * if formatter != null applies the formatter to the column names. Examples of output are:
         * <p/>
         * formatter == null ==> [colName1, colName2]
         * <p/>
         * formatter == "%s = ?" ==> [colName1 = ?, colName2 = ?]
         */
        default Collection<String> getColumnNames(List<String> columns, final String formatter) {
            return columns.stream()
                    .map(column -> formatter == null ? column : String.format(formatter, column))
                    .collect(Collectors.toList());
        }
    }

    static class MySQLUpsertSupport implements DatabaseUpsertSupport {

        @Override
        public UpsertQueryInfo buildUpsertQuery(String tableName, List<String> columns) {
            String sql = "INSERT INTO `" + tableName + "` ("
                    + String.join(", ", getColumnNames(columns, "`%s`"))
                    + ") VALUES(" + getBindVariables("?,", columns.size()) + ")"
                    + " ON DUPLICATE KEY UPDATE "
                    + String.join(", ", getColumnNames(columns, "`%s` = ?"));

            List<String> columnsForUpsert = new ArrayList<>();
            columnsForUpsert.addAll(columns);
            columnsForUpsert.addAll(columns);

            return new UpsertQueryInfo(sql, columnsForUpsert);
        }
    }

    static class PostgreSQLUpsertSupport implements DatabaseUpsertSupport {

        @Override
        public UpsertQueryInfo buildUpsertQuery(String tableName, List<String> columns) {
            Collection<String> columnNames = getColumnNames(columns, "\"%s\"");
            String sql = "INSERT INTO \"" + tableName + "\" ("
                    + String.join(", ", columnNames)
                    + ") VALUES(" + getBindVariables("?,", columnNames.size()) + ")"
                    + " ON CONFLICT ON CONSTRAINT " + tableName + "_pkey"
                    + " DO UPDATE SET " + String.join(", ", getColumnNames(columns, "\"%s\" = ?"));

            List<String> columnsForUpsert = new ArrayList<>();
            columnsForUpsert.addAll(columns);
            columnsForUpsert.addAll(columns);

            return new UpsertQueryInfo(sql, columnsForUpsert);
        }
    }

    static class PhoenixUpsertSupport implements DatabaseUpsertSupport {

        @Override
        public UpsertQueryInfo buildUpsertQuery(String tableName, List<String> columns) {
            String sql = "UPSERT INTO \"" + tableName + "\" ("
                    + String.join(", ", getColumnNames(columns, "\"%s\""))
                    + ") VALUES( " + getBindVariables("?,", columns.size()) + ")";

            return new UpsertQueryInfo(sql, columns);
        }
    }
}
