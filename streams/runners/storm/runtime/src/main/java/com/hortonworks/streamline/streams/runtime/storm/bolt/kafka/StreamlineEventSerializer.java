package com.hortonworks.streamline.streams.runtime.storm.bolt.kafka;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serializer;
import com.hortonworks.registries.schemaregistry.SchemaCompatibility;
import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.serdes.avro.AvroSnapshotSerializer;
import com.hortonworks.registries.schemaregistry.serdes.avro.kafka.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreamlineEventSerializer implements Serializer<StreamlineEvent> {

    private static final String SCHEMA_KEY = "schema";
    private static final String VALUE_KEY = "value";
    private static final String SCHEMA_NAMESPACE = "com.hortonworks.registries";
    private final AvroSnapshotSerializer avroSnapshotSerializer;
    private SchemaCompatibility compatibility;

    public StreamlineEventSerializer () {
        this.avroSnapshotSerializer = new AvroSnapshotSerializer();
    }
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // ignoring the isKey since this class is expected to be used only as a value serializer for now, value being StreamlineEvent
        compatibility = (SchemaCompatibility) configs.get("schema.compatibility");
        avroSnapshotSerializer.init(configs);
    }

    @Override
    public byte[] serialize(String topic, StreamlineEvent streamlineEvent) {
        if (streamlineEvent == null || streamlineEvent.isEmpty()) {
            return null;
        } else {
            return avroSnapshotSerializer.serialize(getAvroGenericRecord(streamlineEvent, topic), createSchemaMetadata(topic));
        }
    }

    @Override
    public void close() {
        try {
            avroSnapshotSerializer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SchemaMetadata createSchemaMetadata(String topic) {
        SchemaMetadata schemaMetadata = getSchemaKey(topic, false);
        String description = "Schema registered by KafkaAvroSerializer for topic: [" + topic + "] iskey: [" + false + "]";
        return new SchemaMetadata.Builder(schemaMetadata).description(description).compatibility(compatibility).build();
    }

    protected SchemaMetadata getSchemaKey(String topic, boolean isKey) {
        return Utils.getSchemaKey(topic, isKey);
    }

    private static GenericRecord getAvroGenericRecord (Map<String, Object> streamlineEvent, String topic) {
        GenericRecord result = null;
        if (streamlineEvent != null && !streamlineEvent.isEmpty()) {
            result = (GenericRecord) getAvroSchemaAndValue(streamlineEvent, SCHEMA_NAMESPACE, topic).get(VALUE_KEY);
        }
        return result;
    }

    private static Map<String, Object> getAvroSchemaAndValue (Object input, String namespace, String schemaName) {
        Map<String, Object> result = new HashMap<>();
        if (input == null) {
            result.put(SCHEMA_KEY, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.NULL));
            result.put(VALUE_KEY, input);
        } else if (input instanceof Boolean) {
            result.put(SCHEMA_KEY, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.BOOLEAN));
            result.put(VALUE_KEY, input);
        } else if ((input instanceof Byte) || (input instanceof Short) || (input instanceof Integer)) {
            result.put(SCHEMA_KEY, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT));
            result.put(VALUE_KEY, input);
        } else if (input instanceof Long) {
            result.put(SCHEMA_KEY, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.LONG));
            result.put(VALUE_KEY, input);
        } else if (input instanceof Float) {
            result.put(SCHEMA_KEY, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.FLOAT));
            result.put(VALUE_KEY, input);
        } else if (input instanceof Double) {
            result.put(SCHEMA_KEY, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.DOUBLE));
            result.put(VALUE_KEY, input);
        } else if (input instanceof ByteBuffer) {
            result.put(SCHEMA_KEY, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.BYTES));
            result.put(VALUE_KEY, input);
        } else if (input instanceof byte[]) {
            org.apache.avro.Schema fixedSchema = org.apache.avro.Schema.create(Schema.Type.FIXED);
            result.put(SCHEMA_KEY, fixedSchema);
            result.put(VALUE_KEY, new GenericData.Fixed(fixedSchema, (byte[]) input));
        } else if (input instanceof String) {
            result.put(SCHEMA_KEY, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING));
            result.put(VALUE_KEY, input);
        } else if (input instanceof Map && !((Map) input).isEmpty()) {
            List<Schema.Field> fields = new ArrayList<>();
            List values = new ArrayList<>();
            for (Map.Entry<String, Object> entry: ((Map<String, Object>) input).entrySet()) {
                Map<String, Object> fieldResult = getAvroSchemaAndValue(entry.getValue(), namespace + "." + schemaName, entry.getKey());
                fields.add(new org.apache.avro.Schema.Field(entry.getKey(), (org.apache.avro.Schema) fieldResult.get(SCHEMA_KEY), null, null));
                values.add(fieldResult.get(VALUE_KEY));
            }
            org.apache.avro.Schema recordSchema = org.apache.avro.Schema.createRecord(schemaName, null, namespace, false);
            recordSchema.setFields(fields);
            result.put(SCHEMA_KEY, recordSchema);
            GenericRecord genericRecord = new GenericData.Record(recordSchema);
            for (int i = 0; i < fields.size(); ++i) {
                genericRecord.put(fields.get(i).name(), values.get(i));
            }
            result.put(VALUE_KEY, genericRecord);
        } else if (input instanceof List && !((List) input).isEmpty()) {
            // for array even though we(Schema in streamline registry) support different types of elements in an array, avro expects an array
            // schema to have elements of same type. Hence, for now we will restrict array to have elements of same type. Other option is convert
            // a  streamline Schema Array field to Record in avro. However, with that the issue is that avro Field constructor does not allow a
            // null name. We could potentiall hack it by plugging in a dummy name like arrayfield, but seems hacky so not taking that path
            List<Map<String, Object>> elementResults = new ArrayList<>();
            for (Object inputValue: (List) input) {
                elementResults.add(getAvroSchemaAndValue(inputValue, namespace, schemaName));
            }
            org.apache.avro.Schema arraySchema = org.apache.avro.Schema.createArray((org.apache.avro.Schema) elementResults.get(0).get(SCHEMA_KEY));
            result.put(SCHEMA_KEY, arraySchema);
            List values = new ArrayList<>();
            for (Map<String, Object> elementResult: elementResults) {
                values.add(elementResult.get(VALUE_KEY));
            }
            result.put(VALUE_KEY, new GenericData.Array<Object>(arraySchema, values));
        }
        return result;
    }
}
