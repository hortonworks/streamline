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
import com.hortonworks.registries.schemaregistry.avro.AvroSchemaProvider;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import org.apache.storm.shade.com.google.common.base.Preconditions;
import org.apache.storm.spout.MultiScheme;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class AvroKafkaSpoutScheme implements MultiScheme {
    private final AvroStreamsSnapshotDeserializer avroStreamsSnapshotDeserializer;
    private final SchemaMetadata schemaMetadata;
    private java.lang.String dataSourceId;

    public AvroKafkaSpoutScheme(String dataSourceId, String topicName, String schemaRegistryUrl) {
        this.dataSourceId = dataSourceId;
        schemaMetadata = new SchemaMetadata.Builder(topicName).type(AvroSchemaProvider.TYPE).schemaGroup("kafka").build();

        avroStreamsSnapshotDeserializer = new AvroStreamsSnapshotDeserializer();
        Map<String, Object> config = new HashMap<>();
        config.put(SchemaRegistryClient.Options.SCHEMA_REGISTRY_URL, schemaRegistryUrl);
        avroStreamsSnapshotDeserializer.init(config);
    }

    @Override
    public Iterable<List<Object>> deserialize(ByteBuffer byteBuffer) {
        Map<String, Object> keyValues = (Map<String, Object>) avroStreamsSnapshotDeserializer
                .deserialize(new ByteBufferInputStream(byteBuffer),
                             schemaMetadata,
                             null);

        return Collections.<List<Object>>singletonList(new Values(new IotasEventImpl(keyValues, dataSourceId)));
    }

    @Override
    public Fields getOutputFields() {
        return new Fields(IotasEvent.IOTAS_EVENT);
    }

    public static class ByteBufferInputStream extends InputStream {

        private final ByteBuffer buf;

        public ByteBufferInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        public int read() throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        public int read(byte[] bytes, int off, int len) throws IOException {
            Preconditions.checkNotNull(bytes, "Given bytearray can not be null");
            Preconditions.checkPositionIndexes(off, len, bytes.length);

            if (!buf.hasRemaining()) {
                return -1;
            }

            if (len == 0) {
                return 0;
            }

            int end = Math.min(len, buf.remaining());
            buf.get(bytes, off, end);
            return end;
        }
    }

}
