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
package com.hortonworks.iotas.streams.runtime.storm.spout;

import com.hortonworks.iotas.streams.IotasEvent;
import com.hortonworks.iotas.streams.common.IotasEventImpl;
import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import org.apache.storm.spout.MultiScheme;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class StreamsKafkaSpoutScheme implements MultiScheme {

    private volatile StreamsJsonSnapshotDeserializer deserializer;
    private final SchemaMetadata schemaMetadata;
    private final String schemaRegistryUrl;
    private String dataSourceId;

    public StreamsKafkaSpoutScheme(String dataSourceId, String schemaName, String schemaRegistryUrl) {
        this.dataSourceId = dataSourceId;
        schemaMetadata = new SchemaMetadata.Builder(schemaName).type("streams").schemaGroup("kafka").build();
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    @Override
    public Iterable<List<Object>> deserialize(ByteBuffer byteBuffer) {
        Map<String, Object> keyValues = deserializer().deserialize(getInputStream(byteBuffer), schemaMetadata, null);
        return Collections.<List<Object>>singletonList(new Values(new IotasEventImpl(keyValues, dataSourceId)));
    }

    private synchronized StreamsJsonSnapshotDeserializer deserializer() {
        if(deserializer == null) {
            deserializer = new StreamsJsonSnapshotDeserializer();
            deserializer.init(Collections.singletonMap(SchemaRegistryClient.Options.SCHEMA_REGISTRY_URL, schemaRegistryUrl));
            return deserializer;
        }

        return deserializer;
    }

    private InputStream getInputStream(ByteBuffer byteBuffer) {
        byte[] byteArray = new byte[byteBuffer.remaining()];
        byteBuffer.get(byteArray, 0, byteArray.length);
        return new ByteArrayInputStream(byteArray);
    }

    @Override
    public Fields getOutputFields() {
        return new Fields(IotasEvent.IOTAS_EVENT);
    }
}
