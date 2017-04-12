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

import java.io.Serializable;

/**
 * Scheme to be implemented for Kafka sources to deserialize bytebuffer record into tuples.
 */
public interface KafkaSourceScheme extends MultiScheme {

    /**
     * Initializes with the given {@code config}
     *
     * @param config configuration instance
     */
    void init(Config config);

    class Config implements Serializable {
        private String topicName;
        private String schemaRegistryUrl;
        private String dataSourceId;

        public Config(String topicName, String schemaRegistryUrl, String dataSourceId) {
            this.topicName = topicName;
            this.schemaRegistryUrl = schemaRegistryUrl;
            this.dataSourceId = dataSourceId;
        }

        public String getTopicName() {
            return topicName;
        }

        public String getSchemaRegistryUrl() {
            return schemaRegistryUrl;
        }

        public String getDataSourceId() {
            return dataSourceId;
        }

    }

}
