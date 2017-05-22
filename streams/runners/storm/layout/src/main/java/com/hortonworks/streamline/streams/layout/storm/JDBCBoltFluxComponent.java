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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flux component for JDBC sinks
 */
@SuppressWarnings("unchecked")
public class JDBCBoltFluxComponent extends AbstractFluxComponent {
    private static final String KEY_TABLE_NAME = "tableName";
    private static final String KEY_COLUMNS = "columns";
    private static final String KEY_DRIVER_CLASSNAME = "driverClassName";
    private static final String KEY_JDBC_URL = "jdbcUrl";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    @Override
    protected void generateComponent() {
        String boltId = "jdbcInsertBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "org.apache.storm.jdbc.bolt.JdbcInsertBolt";
        List<Object> constructorArgs = new ArrayList<>();
        List<Map<String, Object>> configMethods = new ArrayList<>();
        String connectionProviderId = getConnectionProvider();
        addArg(constructorArgs, getRefYaml(connectionProviderId));
        addArg(constructorArgs, getRefYaml(getJdbcMapper(connectionProviderId)));
        Map<String, Object> withTableName = new LinkedHashMap<>();
        withTableName.put(StormTopologyLayoutConstants.YAML_KEY_NAME, "withTableName");
        String tableName = (String) conf.get(KEY_TABLE_NAME);
        withTableName.put(StormTopologyLayoutConstants.YAML_KEY_ARGS, Arrays.asList(tableName));
        configMethods.add(withTableName);
        component = createComponent(boltId, boltClassName, null, constructorArgs, configMethods);
        addParallelismToComponent();
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

    private String getJdbcMapper(String connectionProviderId) {
        String componentId = "JdbcMapper" + UUID_FOR_COMPONENTS;
        List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, KEY_TABLE_NAME);
        addArg(constructorArgs, getRefYaml(connectionProviderId));
        String columnListId = getColumnList((List<String>) conf.get(KEY_COLUMNS));
        addArg(constructorArgs, getRefYaml(columnListId));
        String className = "com.hortonworks.streamline.streams.runtime.storm.bolt.StreamlineJdbcMapper";
        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }
}
