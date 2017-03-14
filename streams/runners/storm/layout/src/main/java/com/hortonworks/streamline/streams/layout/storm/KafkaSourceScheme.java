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

import org.apache.storm.spout.MultiScheme;

import java.util.Map;

/**
 * Scheme to be implemented for Kafka sources to deserialize bytebuffer record into tuples.
 */
public interface KafkaSourceScheme extends MultiScheme {

    /**
     * Key for topic property.
     */
    String TOPIC_KEY = "topic";

    /**
     * Key for schema registry url property.
     */
    String SCHEMA_REGISTRY_URL_KEY = "schemaRegistryUrl";

    /**
     * Key for dataSourceId property.
     */
    String DATASOURCE_ID_KEY = "dataSourceId";

    /**
     * Initializes with the given {@code conf}
     *
     * @param conf configuration instance which can include values for keys like {@link #TOPIC_KEY},
     *             {@link #SCHEMA_REGISTRY_URL_KEY}, {@link #DATASOURCE_ID_KEY}
     */
    void init(Map<String, Object> conf);
}
