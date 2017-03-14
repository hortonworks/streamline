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

package com.hortonworks.streamline.streams.runtime.storm.spout;

import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.avro.AvroSchemaProvider;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import com.hortonworks.registries.schemaregistry.serdes.avro.kafka.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.layout.storm.KafkaSourceScheme;
import org.apache.storm.shade.com.google.common.base.Preconditions;
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
public class AvroKafkaSpoutScheme implements KafkaSourceScheme {
    private SchemaMetadata schemaMetadata;
    private String schemaRegistryUrl;
    private String dataSourceId;
    private transient volatile AvroStreamsSnapshotDeserializer avroStreamsSnapshotDeserializer;

    public AvroKafkaSpoutScheme() {
    }

    public AvroKafkaSpoutScheme(String dataSourceId, String topicName, String schemaRegistryUrl) {
        this.dataSourceId = dataSourceId;
        this.schemaRegistryUrl = schemaRegistryUrl;
        schemaMetadata = createSchemaMetadata(topicName);
    }

    private SchemaMetadata createSchemaMetadata(String topicName) {
        return new SchemaMetadata.Builder(Utils.getSchemaKey(topicName, false))
                .type(AvroSchemaProvider.TYPE)
                .schemaGroup("kafka")
                .build();
    }

    @Override
    public void init(Map<String, Object> conf) {
        this.dataSourceId = (String) conf.get(KafkaSourceScheme.DATASOURCE_ID_KEY);
        this.schemaRegistryUrl = (String) conf.get(KafkaSourceScheme.SCHEMA_REGISTRY_URL_KEY);
        String topicName = (String) conf.get(KafkaSourceScheme.TOPIC_KEY);
        schemaMetadata = createSchemaMetadata(topicName);
    }

    private AvroStreamsSnapshotDeserializer deserializer() {
        if (avroStreamsSnapshotDeserializer == null) {
            synchronized (this) {
                if (avroStreamsSnapshotDeserializer == null) {
                    AvroStreamsSnapshotDeserializer deserializer = new AvroStreamsSnapshotDeserializer();
                    Map<String, Object> config = new HashMap<>();
                    config.put(SchemaRegistryClient.Configuration.SCHEMA_REGISTRY_URL.name(), schemaRegistryUrl);
                    deserializer.init(config);
                    avroStreamsSnapshotDeserializer = deserializer;
                }
            }
        }
        return avroStreamsSnapshotDeserializer;
    }

    @Override
    public Iterable<List<Object>> deserialize(ByteBuffer byteBuffer) {
        Map<String, Object> keyValues = (Map<String, Object>) deserializer()
                .deserialize(new ByteBufferInputStream(byteBuffer),
                             schemaMetadata,
                             null);

        StreamlineEvent streamlineEvent = StreamlineEventImpl.builder().putAll(keyValues).dataSourceId(dataSourceId).build();
        return Collections.<List<Object>>singletonList(new Values(streamlineEvent));
    }

    @Override
    public Fields getOutputFields() {
        return new Fields(StreamlineEvent.STREAMLINE_EVENT);
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
            Preconditions.checkNotNull(bytes, "Given byte array can not be null");
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
