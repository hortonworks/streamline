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
package com.hortonworks.streamline.streams.layout.storm;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is used for generating respective flux component for given Cassandra sink configuration.
 *
 * Sample Json for cassandra sink configuration
 * <pre>
 * {@code
 *    {
 *     "flushFrequencyInMilliSecs": 1000,
 *     "tableName": "temperature",
 *     "columns": [
 *       {
 *         "columnName": "weather_station_id",
 *         "fieldName": "weatherStationId"
 *       },
 *       {
 *         "columnName": "event_time",
 *         "fieldName": "eventTime"
 *       },
 *       {
 *         "columnName": "temperature",
 *         "fieldName": "temperature"
 *       }
 *     ],
 *     "cassandraEndpointConfig" : {
 *       "cassandra.username" :"sato",
 *       "cassandra.password" :"passwd",
 *       "cassandra.keyspace" :"ks",
 *       "cassandra.nodes" :"cassandra-srv-1, cassandra-srv-2",
 *       "cassandra.port" :9042,
 *       "cassandra.batch.size.rows" :1000,
 *       "cassandra.retryPolicy" :"com.datastax.driver.core.policies.DefaultRetryPolicy",
 *       "cassandra.output.consistencyLevel" :"QUORUM",
 *       "cassandra.reconnectionPolicy.baseDelayMs" :1000,
 *       "cassandra.reconnectionPolicy.maxDelayMs" :6000
 *     }
 *    }
 * }
 * </pre>
 */
public class CassandraBoltFluxComponent extends AbstractFluxComponent {

    private static final String CASSANDRA_BOLT_CLASS = "com.hortonworks.streamline.streams.runtime.storm.cassandra.StreamlineCassandraBolt";
    private static final String MAPPER_BUILDER_CLASS = "org.apache.storm.cassandra.query.builder.BoundCQLStatementMapperBuilder";
    private static final String FIELD_SELECTOR_CLASS_NAME = "com.hortonworks.streamline.streams.runtime.storm.cassandra.StreamlineFieldSelector";

    private static final String COLUMN_NAME_KEY = "columnName";
    private static final String FIELD_NAME_KEY = "fieldName";
    private static final String TABLE_NAME_KEY = "tableName";
    private static final String COLUMNS_KEY = "columns";

    @Override
    protected void generateComponent() {
        String boltClassId = "StreamlineCassandraBolt-" + UUID_FOR_COMPONENTS;

        List<Object> constructorArgs = new ArrayList<>();
        constructorArgs.add(getRefYaml(addCassandraCqlMapperBuilder()));

        // we should handle null value since Flux throws error while finding method with null in arguments
        // pass flushFrequencyInMilliSecs only if it's available
        Object flushFrequencyInSecs = conf.get("flushFrequencyInMilliSecs");
        if (flushFrequencyInSecs != null) {
            constructorArgs.add(flushFrequencyInSecs);
        }

        Map configMethod = getConfigMethodWithRefArgs("withCassandraConfig", Collections.singletonList(addCassandraConfig()));

        component = createComponent(boltClassId, CASSANDRA_BOLT_CLASS, null, constructorArgs, Collections.singletonList(configMethod));

        addParallelismToComponent();
    }

    private String addCassandraConfig() {
        String mapClassId = "map-" + UUID_FOR_COMPONENTS;
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

    private String addCassandraCqlMapperBuilder() {
        String mapperClassId = "boundCQLStatementMapperBuilder-" + UUID_FOR_COMPONENTS;

        String tableName = (String) conf.get(TABLE_NAME_KEY);
        List<Map<String, String>> columns = (List<Map<String, String>>) conf.get(COLUMNS_KEY);
        List<String> columnNames = columns.stream().map(column -> column.get(COLUMN_NAME_KEY)).collect(Collectors.toList());
        List<String> fieldSelectorIds = createFieldSelectors(columns);

        String cql = createInsertToCql(tableName, columnNames);
        Map configMethod = getConfigMethodWithRefListArg("bind", fieldSelectorIds);
        addToComponents(createComponent(mapperClassId, MAPPER_BUILDER_CLASS, null, Arrays.asList(cql), Arrays.asList(configMethod)));

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

    private Map<String, Object> getConfigMethodWithRefListArg(String configMethodName, List<String> refIds) {
        Map<String, Object> configMethod = new LinkedHashMap<>();
        configMethod.put(StormTopologyLayoutConstants.YAML_KEY_NAME, configMethodName);

        List<Map<String, Object>> methodArgs = new ArrayList<>();
        Map<String, Object> refMap = new HashMap<>();
        refMap.put(StormTopologyLayoutConstants.YAML_KEY_REF_LIST, refIds);
        methodArgs.add(refMap);
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
        String idStr = "FieldSelector-" + UUID_FOR_COMPONENTS + "-";
        int ct = 0;
        List<String> fieldSelectorIds = new ArrayList<>(columns.size());
        for (Map<String, String> column : columns) {
            String fieldSelectorId = idStr + (++ct);
            List<String> constructorArgs = Arrays.asList(column.get(FIELD_NAME_KEY), column.get(COLUMN_NAME_KEY));
            addToComponents(createComponent(fieldSelectorId, FIELD_SELECTOR_CLASS_NAME, null, constructorArgs, null));
            fieldSelectorIds.add(fieldSelectorId);
        }
        return fieldSelectorIds;
    }

}
