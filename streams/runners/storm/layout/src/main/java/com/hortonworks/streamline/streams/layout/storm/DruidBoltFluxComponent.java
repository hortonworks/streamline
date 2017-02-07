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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hortonworks.streamline.streams.StreamlineEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DruidBoltFluxComponent extends AbstractFluxComponent {

    public final static String KEY_BATCH_SIZE = "maxBatchSize";
    public final static String KEY_PENDING_BATCHES = "maxPendingBatches";
    public final static String KEY_LINGER_MILLS = "lingerMillis";
    public final static String KEY_BLOCK_ON_FULL = "blockOnFull";
    public final static String KEY_DISCARD_STREAM_ID = "discardStreamId";

    public final static String KEY_INDEX_SERVICE = "indexService";
    public final static String KEY_DISCOVERY_PATH = "discoveryPath";
    public final static String KEY_DATA_SOURCE = "dataSource";
    public final static String KEY_ZK_CONNECT = "tranquilityZKconnect";
    public final static String KEY_DIMENSIONS = "dimensions";
    public final static String KEY_TIMESTAMP_FIELD = "timestampField";
    public final static String KEY_PARTITIONS = "clusterPartitions";
    public final static String KEY_REPLICATION = "clusterReplication";
    public final static String KEY_WINDOW_PERIOD = "windowPeriod";
    public final static String KEY_INDEX_RETRY_PERIOD = "indexRetryPeriod";
    public final static String KEY_SEGMENT_GRANULARITY = "segmentGranularity";
    public final static String KEY_QUERY_GRANULARITY = "queryGranularity";
    public final static String KEY_AGGR_LIST = "aggregatorList";

    @Override
    protected void generateComponent() {
        String boltId = "druidBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "org.apache.storm.druid.bolt.DruidBeamBolt";
        List<Object> boltConstructorArgs = new ArrayList<>();
        boltConstructorArgs.add(getRefYaml(addDruidBeamFactoryComponent()));
        boltConstructorArgs.add(getRefYaml(addTupleDruidEventMapperComponent()));
        boltConstructorArgs.add(getRefYaml(addDruidConfigBuilderComponent()));
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, null);
        addParallelismToComponent();
    }

    private String addDruidBeamFactoryComponent() {
        String componentId = "druidDruidBeamFactory" + UUID_FOR_COMPONENTS;
        String className = "com.hortonworks.streamline.streams.layout.storm.DruidBeamFactoryImpl";

        List<String> constructorArgs = Lists.newArrayList(getJsonString(conf.get(KEY_AGGR_LIST)));

        String[] configMethodNames = {
                "setIndexService", "setDiscoveryPath", "setDataSource",
                "setTranquilityZKconnect", "setDimensions", "setTimestampField",
                "setClusterReplication", "setClusterPartitions", "setWindowPeriod",
                "setIndexRetryPeriod", "setSegmentGranularity", "setQueryGranularity" };

        String[] configKeys = {
                KEY_INDEX_SERVICE,
                KEY_DISCOVERY_PATH,
                KEY_DATA_SOURCE,
                KEY_ZK_CONNECT,
                KEY_DIMENSIONS,
                KEY_TIMESTAMP_FIELD,
                KEY_PARTITIONS,
                KEY_REPLICATION,
                KEY_WINDOW_PERIOD,
                KEY_INDEX_RETRY_PERIOD,
                KEY_SEGMENT_GRANULARITY,
                KEY_QUERY_GRANULARITY
        };

        List configMethods = getConfigMethodsYaml(configMethodNames, configKeys);
        addToComponents(createComponent(componentId, className, null, constructorArgs, configMethods));
        return componentId;
    }

    private String getJsonString(Object object) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new IllegalArgumentException("Exception while parsing the aggregratorInfo");
        }
    }

    private String addTupleDruidEventMapperComponent() {
        String componentId = "druidTupleDruidEventMapper" + UUID_FOR_COMPONENTS;
        String className = "com.hortonworks.streamline.streams.layout.storm.DruidEventMapper";
        List<String> constructorArgs = Lists.newArrayList(StreamlineEvent.STREAMLINE_EVENT);
        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }

    private String addDruidConfigBuilderComponent() {
        String builderComponentId = "druidConfig" + UUID_FOR_COMPONENTS;
        String builderClassName = "org.apache.storm.druid.bolt.DruidConfig$Builder";

        String[] configKeys = {
                KEY_BATCH_SIZE, KEY_PENDING_BATCHES, KEY_LINGER_MILLS,
                KEY_BLOCK_ON_FULL, KEY_DISCARD_STREAM_ID};

        List configMethods = getConfigMethodsYaml(configKeys, configKeys);
        addToComponents(createComponent(builderComponentId, builderClassName, null, null, configMethods));
        return builderComponentId;
    }
}
