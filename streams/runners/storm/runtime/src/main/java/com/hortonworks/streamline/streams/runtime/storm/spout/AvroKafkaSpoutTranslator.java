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

import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.storm.kafka.spout.KafkaTuple;
import org.apache.storm.kafka.spout.RecordTranslator;
import org.apache.storm.shade.com.google.common.base.Preconditions;
import org.apache.storm.tuple.Fields;

import javax.security.auth.Subject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AvroKafkaSpoutTranslator implements RecordTranslator<Object, ByteBuffer> {

    private final String outputStream;
    private final String topic;
    private final String dataSourceId;
    private final String schemaRegistryUrl;
    private final Integer readerSchemaVersion;
    private transient volatile AvroStreamsSnapshotDeserializer avroStreamsSnapshotDeserializer;
    private transient Subject subject;

    public AvroKafkaSpoutTranslator (String outputStream, String topic, String dataSourceId, String schemaRegistryUrl, Integer readerSchemaVersion) {
        this.outputStream = outputStream;
        this.topic = topic;
        this.dataSourceId = dataSourceId;
        this.schemaRegistryUrl = schemaRegistryUrl;
        this.readerSchemaVersion = readerSchemaVersion;
    }
    @Override
    public List<Object> apply (ConsumerRecord<Object, ByteBuffer> consumerRecord) {
        Map < String, Object > keyValues = null;
        try {
            keyValues = Subject.doAs(subject(), new PrivilegedExceptionAction<Map<String, Object>>() {
                @Override
                public Map<String, Object> run () {
                    return (Map<String, Object>) deserializer().deserialize(new ByteBufferInputStream(consumerRecord.value()), readerSchemaVersion);
                }
            });
        } catch (PrivilegedActionException e) {
            throw new RuntimeException(e);
        }
        StreamlineEvent streamlineEvent = StreamlineEventImpl.builder().putAll(keyValues).dataSourceId(dataSourceId).build();
        KafkaTuple kafkaTuple = new KafkaTuple(streamlineEvent);
        kafkaTuple.routedTo(outputStream);
        return kafkaTuple;
    }

    @Override
    public Fields getFieldsFor (String s) {
        return new Fields(StreamlineEvent.STREAMLINE_EVENT);
    }

    @Override
    public List<String> streams () {
        return Collections.singletonList(outputStream);
    }

    private AvroStreamsSnapshotDeserializer deserializer () {
        //initializing deserializer here should be synchronized (using DCL pattern?) when single threaded nature of kafka spout does not hold true anymore
        if (avroStreamsSnapshotDeserializer == null) {
            AvroStreamsSnapshotDeserializer deserializer = new AvroStreamsSnapshotDeserializer();
            Map<String, Object> config = new HashMap<>();
            config.put(SchemaRegistryClient.Configuration.SCHEMA_REGISTRY_URL.name(), schemaRegistryUrl);
            deserializer.init(config);
            avroStreamsSnapshotDeserializer = deserializer;
        }
        return avroStreamsSnapshotDeserializer;
    }

    private Subject subject () {
        //kafka consumer should have called doAs already with KafkaClient principal and hence that subject should be associated with access control's context
        if (subject == null) {
            subject = Subject.getSubject(AccessController.getContext());
        }
        return subject;
    }

    public static class ByteBufferInputStream extends InputStream {

        private final ByteBuffer buf;

        public ByteBufferInputStream (ByteBuffer buf) {
            this.buf = buf;
        }

        public int read () throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        public int read (byte[] bytes, int off, int len) throws IOException {
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
