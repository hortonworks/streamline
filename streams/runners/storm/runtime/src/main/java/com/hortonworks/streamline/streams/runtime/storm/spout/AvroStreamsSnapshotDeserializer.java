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
package com.hortonworks.streamline.streams.runtime.storm.spout;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.serdes.avro.AvroSnapshotDeserializer;
import com.hortonworks.registries.schemaregistry.serde.SerDesException;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericFixed;
import org.apache.avro.generic.IndexedRecord;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class AvroStreamsSnapshotDeserializer extends AvroSnapshotDeserializer {

    @Override
    protected Object doDeserialize(InputStream payloadInputStream, SchemaMetadata schemaMetadata,
                                   Integer writerSchemaVersion, Integer readerSchemaVersion) throws SerDesException {
        Object deserializedObj = super.doDeserialize(payloadInputStream, schemaMetadata, writerSchemaVersion, readerSchemaVersion);

        Map<String, Object> keyValues = new LinkedHashMap<>();
        Object values = convertValue(deserializedObj);
        if (values instanceof Map) {
            keyValues.putAll((Map) values);
        } else {
            keyValues.put(StreamlineEvent.PRIMITIVE_PAYLOAD_FIELD, values);
        }

        return keyValues;
    }

    private Object convertValue(Object deserializedObj) {
        Object value;

        //check for specific-record type and build a map from that
        if (deserializedObj instanceof IndexedRecord) { // record
            IndexedRecord indexedRecord = (IndexedRecord) deserializedObj;
            List<Schema.Field> fields = indexedRecord.getSchema().getFields();
            Map<String, Object> keyValues = new LinkedHashMap<>();
            for (Schema.Field field : fields) {
                keyValues.put(field.name(), convertValue(indexedRecord.get(field.pos())));
            }
            value = new StreamlineEventImpl(keyValues, null);

        } else if (deserializedObj instanceof ByteBuffer) { // byte array representation
            ByteBuffer byteBuffer = (ByteBuffer) deserializedObj;
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            value = bytes;

        } else if (deserializedObj instanceof GenericEnumSymbol) { //enums
            GenericEnumSymbol symbol = (GenericEnumSymbol) deserializedObj;
            value = symbol.toString();

        } else if (deserializedObj instanceof CharSequence) { // symbols
            value = deserializedObj.toString();

        } else if (deserializedObj instanceof Map) { // type of map
            Map<Object, Object> map = (Map<Object, Object>) deserializedObj;
            Map<String, Object> keyValues = new LinkedHashMap<>();
            for (Map.Entry entry : map.entrySet()) {
                keyValues.put(entry.getKey().toString(), convertValue(entry.getValue()));
            }
            value = new StreamlineEventImpl(keyValues, null);

        } else if (deserializedObj instanceof Collection) { // type of array
            Collection<Object> collection = (Collection<Object>) deserializedObj;
            List<Object> values = new ArrayList<>(collection.size());
            for (Object obj : collection) {
                values.add(convertValue(obj));
            }
            value = values;

        } else if (deserializedObj instanceof GenericFixed) { // fixed type
            GenericFixed genericFixed = (GenericFixed) deserializedObj;
            value = genericFixed.bytes();

        } else { // other primitive types
            value = deserializedObj;
        }

        return value;
    }

}
