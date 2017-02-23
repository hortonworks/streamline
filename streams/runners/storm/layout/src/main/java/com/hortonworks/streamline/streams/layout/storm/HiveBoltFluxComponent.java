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
package com.hortonworks.streamline.streams.layout.storm;

import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Flux component for Hive sinks
 */
@SuppressWarnings("unchecked")
public class HiveBoltFluxComponent extends AbstractFluxComponent {
    private static final String KEY_FIELDS = "fields";
    private static final String KEY_PARTITION_FIELDS = "partitionFields";
    private static final String KEY_METASTORE_URI = "metaStoreURI";
    private static final String KEY_DATABASE_NAME = "databaseName";
    private static final String KEY_TABLE_NAME = "tableName";
    private static final String KEY_TICK_TUPLE_INTERVAL = "tickTupleInterval";
    private static final String KEY_TXNS_PER_BATCH = "txnsPerBatch";
    private static final String KEY_MAX_OPEN_CONNECTIONS = "maxOpenConnections";
    private static final String KEY_BATCH_SIZE = "batchSize";
    private static final String KEY_IDLE_TIMEOUT = "idleTimeout";
    private static final String KEY_CALL_TIMEOUT = "callTimeout";
    private static final String KEY_HEARTBEAT_INTERVAL = "heartBeatInterval";
    private static final String KEY_AUTOCREATE_PARTITIONS = "autoCreatePartitions";
    private static final String KEY_KERBEROS_KEYTAB = "kerberosKeytab";
    private static final String KEY_KERBEROS_PRINCIPAL = "kerberosPrincipal";

    private static final String KEY_TIMEFORMAT = "timeFormat";
    private static final String KEY_FIELD_DELIMITER = "fieldDelimiter";


    @Override
    protected void generateComponent() {
        String boltId = "hiveBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "org.apache.storm.hive.bolt.HiveBolt";
        List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, getRefYaml(getHiveOptions()));
        component = createComponent(boltId, boltClassName, null, constructorArgs, null);
        addParallelismToComponent();
    }

    private String getHiveOptions() {
        String componentId = "hiveOptions" + UUID_FOR_COMPONENTS;
        List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, KEY_METASTORE_URI);
        addArg(constructorArgs, KEY_DATABASE_NAME);
        addArg(constructorArgs, KEY_TABLE_NAME);
        addArg(constructorArgs, getRefYaml(getHiveMapper()));
        String className = "org.apache.storm.hive.common.HiveOptions";
        String[] configMethodNames = new String[]{
                "withTickTupleInterval",
                "withTxnsPerBatch",
                "withMaxOpenConnections",
                "withBatchSize",
                "withIdleTimeout",
                "withCallTimeout",
                "withHeartBeatInterval",
                "withAutoCreatePartitions",
                "withKerberosKeytab",
                "withKerberosPrincipal",
        };

        String[] configKeys = new String[]{
                KEY_TICK_TUPLE_INTERVAL,
                KEY_TXNS_PER_BATCH,
                KEY_MAX_OPEN_CONNECTIONS,
                KEY_BATCH_SIZE,
                KEY_IDLE_TIMEOUT,
                KEY_IDLE_TIMEOUT,
                KEY_CALL_TIMEOUT,
                KEY_HEARTBEAT_INTERVAL,
                KEY_AUTOCREATE_PARTITIONS,
                KEY_KERBEROS_KEYTAB,
                KEY_KERBEROS_PRINCIPAL
        };
        addToComponents(createComponent(componentId, className, null, constructorArgs,
                getConfigMethodsYaml(configMethodNames, configKeys)));
        return componentId;
    }

    private void add(List<Map<String, Object>> configMethods, String column) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(StormTopologyLayoutConstants.YAML_KEY_NAME, "add");
        map.put(StormTopologyLayoutConstants.YAML_KEY_ARGS, Collections.singletonList(column));
        configMethods.add(map);
    }

    private String getColumnList(List<String> columns) {
        String componentId = "ColumnList" + UUID.randomUUID();
        List<Map<String, Object>> configMethods = new ArrayList<>();
        columns.forEach(
                column -> add(configMethods, column)
        );
        String className = "java.util.ArrayList";
        addToComponents(createComponent(componentId, className, null, null, configMethods));
        return componentId;
    }

    private String getHiveMapper() {
        String componentId = "HiveMapper" + UUID_FOR_COMPONENTS;
        List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, getRefYaml(getColumnList((List<String>) conf.get(KEY_FIELDS))));
        addArg(constructorArgs, getRefYaml(getColumnList((List<String>) conf.get(KEY_PARTITION_FIELDS))));
        String[] configMethodNames = {"setTimeFormat", "setFieldDelimiter"};
        String[] configKeys = {KEY_TIMEFORMAT, KEY_FIELD_DELIMITER};
        List configMethods = getConfigMethodsYaml(configMethodNames, configKeys);
        String className = "com.hortonworks.streamline.streams.runtime.storm.bolt.StreamlineHiveMapper";
        addToComponents(createComponent(componentId, className, null, constructorArgs, configMethods));
        return componentId;
    }

}
