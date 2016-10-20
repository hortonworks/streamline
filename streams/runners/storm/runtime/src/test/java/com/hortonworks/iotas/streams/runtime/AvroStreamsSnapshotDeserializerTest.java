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
package com.hortonworks.iotas.streams.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.streams.runtime.storm.spout.AvroStreamsSnapshotDeserializer;
import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.SchemaVersionKey;
import com.hortonworks.registries.schemaregistry.serdes.avro.AvroSnapshotSerializer;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 *
 */
public class AvroStreamsSnapshotDeserializerTest {
    private static final Logger LOG = LoggerFactory.getLogger(AvroStreamsSnapshotDeserializerTest.class);

    @Test
    public void testAvroPayloadConversions() throws Exception {

        try (InputStream schemaStream = AvroStreamsSnapshotDeserializerTest.class.getResourceAsStream("/avro/complex.avsc")) {

            CustomAvroSerializer customAvroSerializer = new CustomAvroSerializer();
            final Schema schema = new Schema.Parser().parse(schemaStream);
            GenericRecord inputRecord = generateGenericRecord(schema);
            LOG.info("Generated record [{}]", inputRecord);
            byte[] serializedBytes = customAvroSerializer.customSerialize(inputRecord);

            AvroStreamsSnapshotDeserializer avroStreamsSnapshotDeserializer = new AvroStreamsSnapshotDeserializer() {
                @Override
                protected Schema getSchema(SchemaVersionKey schemaVersionKey) {
                    return schema;
                }
            };
            Map<String, String> config = Collections.singletonMap(SchemaRegistryClient.Options.SCHEMA_REGISTRY_URL, "http://localhost:8080/api/v1");
            avroStreamsSnapshotDeserializer.init(config);

            SchemaMetadata schemaMetadata = new SchemaMetadata.Builder("topic-1").type("avro").schemaGroup("kafka").build();
            Object deserializedObject = avroStreamsSnapshotDeserializer.deserialize(new ByteArrayInputStream(serializedBytes), schemaMetadata, 1);

            Map<Object, Object> map = (Map<Object, Object>) deserializedObject;
            String deserializedJson = new ObjectMapper().writeValueAsString(map);
            String inputJson = GenericData.get().toString(inputRecord);

            LOG.info("inputJson #{}# " + inputJson);
            LOG.info("deserializedJson = #{}#" + deserializedJson);

            ObjectMapper objectMapper = new ObjectMapper();
            Assert.assertEquals(objectMapper.readTree(inputJson), objectMapper.readTree(deserializedJson));
        }
    }

    private GenericRecord generateGenericRecord(Schema schema) {
        GenericRecord addressRecord = new GenericData.Record(schema.getField("address").schema());
        long now = System.currentTimeMillis();
        addressRecord.put("streetaddress", "streetaddress:" + now);
        addressRecord.put("city", "city-" + now);
        addressRecord.put("state", "state-" + now);
        addressRecord.put("zip", "zip" + now);

        GenericRecord rootRecord = new GenericData.Record(schema);
        rootRecord.put("xid", now);
        rootRecord.put("name", "name-"+now);
        rootRecord.put("version", 1);
        rootRecord.put("timestamp", now);
        rootRecord.put("suit", "SPADES");
        rootRecord.put("address", addressRecord);

        return rootRecord;
    }

    private static class CustomAvroSerializer extends AvroSnapshotSerializer {
        public byte[] customSerialize(Object input) {
            return doSerialize(input, 1);
        }
    }
}
