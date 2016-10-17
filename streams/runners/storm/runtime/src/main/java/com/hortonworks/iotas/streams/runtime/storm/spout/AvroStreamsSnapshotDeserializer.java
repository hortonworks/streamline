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
import com.hortonworks.registries.schemaregistry.SchemaVersionKey;
import com.hortonworks.registries.schemaregistry.avro.AvroSnapshotDeserializer;
import com.hortonworks.registries.schemaregistry.serde.SerDesException;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;

import java.io.InputStream;
import java.util.HashMap;
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

        SchemaVersionKey schemaVersionKey = new SchemaVersionKey(schemaMetadata.getName(),
                                                                 readerSchemaVersion != null ? readerSchemaVersion : writerSchemaVersion);

        Map<String, Object> keyValues = new HashMap<>();
        //check for specific-record type and build a map from that
        if(deserializedObj instanceof IndexedRecord) {
            IndexedRecord indexedRecord = (IndexedRecord) deserializedObj;
            List<Schema.Field> fields = indexedRecord.getSchema().getFields();
            for (Schema.Field field : fields) {
                keyValues.put(field.name(), indexedRecord.get(field.pos()));
            }
        } else {
            //check for map/record types to build Map instance
            Schema effSchema = getSchema(schemaVersionKey);
            Schema.Type type = effSchema.getType();
            switch (type) {
                case MAP:
                    Map map = (Map) deserializedObj;
                    keyValues.putAll(map);
                    break;
                case ENUM:
                case FIXED:
                    keyValues.put(effSchema.getName(), deserializedObj);
                    break;
                default:
                    // all the remaining can be considered primitive.
                    keyValues.put(IotasEvent.PRIMITIVE_PAYLOAD_FIELD, deserializedObj);
            }
        }

        return deserializedObj;
    }
}
