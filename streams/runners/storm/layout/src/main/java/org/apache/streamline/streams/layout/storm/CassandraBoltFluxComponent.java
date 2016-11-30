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
package org.apache.streamline.streams.layout.storm;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sample Json for cassandra configuration
 * {
 * "flushFrequencyInMilliSecs": 1000,
 * "tableName": "temperature",
 * "columns": [
 * {
 * "columnName": "weather_station_id",
 * "fieldName": "weather_station_id"
 * },
 * {
 * "columnName": "event_time",
 * "fieldName": "event_time"
 * },
 * {
 * "columnName": "temperature",
 * "fieldName": "temperature"
 * }
 * ]
 * }
 */
public class CassandraBoltFluxComponent extends AbstractFluxComponent {

    @Override
    protected void generateComponent() {
        String boltClassId = "StreamlineCassandraBolt-" + UUID_FOR_COMPONENTS;
        String boltClassName = "org.apache.streamline.streams.runtime.storm.cassandra.StreamlineCassandraBolt";

        List<Object> constructorArgs = new ArrayList<>();
        constructorArgs.add(getRefYaml(createCassandraCqlMapperBuilder()));
        constructorArgs.add(conf.get("flushFrequencyInMilliSecs"));

        Map configMethod = getConfigMethodWithRefArgs("withCassandraConfig", Arrays.asList(addCassandraConfig()));

        component = createComponent(boltClassId, boltClassName, null, constructorArgs, Arrays.asList(configMethod));

        addParallelismToComponent();
    }

    private String addCassandraConfig() {
        String mapClassId = "map-"+UUID_FOR_COMPONENTS;
        Map<String, Object> cassandraEndpointConfig = (Map<String, Object>) conf.get("cassandraEndpointConfig");
        List<Map<String, Object>> configMethods = new ArrayList<>();
        for (Map.Entry<String, Object> entry : cassandraEndpointConfig.entrySet()) {
            Map<String, Object> configMethod = new LinkedHashMap<>();
            configMethod.put(StormTopologyLayoutConstants.YAML_KEY_NAME, "put");
            configMethod.put(StormTopologyLayoutConstants.YAML_KEY_ARGS, Arrays.asList(entry.getKey(), entry.getValue()));
            configMethods.add(configMethod);
        }
        addToComponents(createComponent(mapClassId, "java.util.HashMap", null, null, configMethods));

        return mapClassId;
    }

    private String createCassandraCqlMapperBuilder() {
        String mapperClassId = "boundCQLStatementMapperBuilder-" + UUID_FOR_COMPONENTS;
        String mapperClass = "org.apache.storm.cassandra.query.builder.BoundCQLStatementMapperBuilder";

        String tableName = (String) conf.get("tableName");
        List<Map<String, String>> columns = (List<Map<String, String>>) conf.get("columns");
        List<String> columnNames = columns.stream().map(column -> column.get("columnName")).collect(Collectors.toList());
        List<String> fieldSelectorIds = createFieldSelectors(columns);

        String cql = createInsertToCql(tableName, columnNames);
        Map configMethod = getConfigMethodWithRefArgs("bind", fieldSelectorIds);
        addToComponents(createComponent(mapperClassId, mapperClass, null, Arrays.asList(cql), Arrays.asList(configMethod)));

        return mapperClassId;
    }

    private Map<String, Object> getConfigMethodWithRefArgs(String configMethodName, List<String> refIds) {
        Map<String, Object> configMethod = new LinkedHashMap<>();
        configMethod.put(StormTopologyLayoutConstants.YAML_KEY_NAME, configMethodName);

        List<Map<String, Object>> methodArgs = new ArrayList<>();
        for (String refId : refIds) {
            Map<String, Object> refMap = new HashMap<>();
            refMap.put(StormTopologyLayoutConstants.YAML_KEY_REF, refId);
            methodArgs.add(refMap);
        }
        configMethod.put(StormTopologyLayoutConstants.YAML_KEY_ARGS, methodArgs);

        return configMethod;
    }

    private String createInsertToCql(String tableName, List<String> columnNames) {
        return String.format("INSERT INTO %s (%s) VALUES (%s)",
                             tableName,
                             Joiner.on(", ").join(columnNames),
                             Joiner.on(", ").join(columnNames.stream()
                                                         .map(x -> "?")
                                                         .collect(Collectors.toList()))
        );
    }

    private List<String> createFieldSelectors(List<Map<String, String>> columns) {
        String fieldSelectorClass = "org.apache.streamline.streams.runtime.storm.cassandra.StreamlineFieldSelector";
        String idStr = "FieldSelector-" + UUID_FOR_COMPONENTS + "-";
        int ct = 0;
        List<String> fieldSelectorIds = new ArrayList<>(columns.size());
        for (Map<String, String> column : columns) {
            String fieldSelectorId = idStr + (++ct);
            List<String> constructorArgs = Arrays.asList(column.get("columnName"), column.get("fieldName"));
            addToComponents(createComponent(fieldSelectorId, fieldSelectorClass, null, constructorArgs, null));
            fieldSelectorIds.add(fieldSelectorId);
        }
        return fieldSelectorIds;
    }

    private String createCqlMapper() {
        return null;
    }
}
