package com.hortonworks.streamline.streams.runtime.storm.bolt.kafka;

import com.hortonworks.registries.schemaregistry.SchemaVersionInfo;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import com.hortonworks.registries.schemaregistry.errors.SchemaNotFoundException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serializer;
import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.serdes.avro.AvroSnapshotSerializer;
import com.hortonworks.registries.schemaregistry.serdes.avro.kafka.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class StreamlineEventSerializer implements Serializer<StreamlineEvent> {
    protected static final Logger LOG = LoggerFactory.getLogger(StreamlineEventSerializer.class);
    private final AvroSnapshotSerializer avroSnapshotSerializer;
    private SchemaRegistryClient schemaRegistryClient;

    public StreamlineEventSerializer () {
        this.avroSnapshotSerializer = new AvroSnapshotSerializer();
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // ignoring the isKey since this class is expected to be used only as a value serializer for now, value being StreamlineEvent
        avroSnapshotSerializer.init(configs);
        this.schemaRegistryClient = new SchemaRegistryClient(configs);
    }

    @Override
    public byte[] serialize(String topic, StreamlineEvent streamlineEvent) {
        SchemaMetadata schemaMetadata = Utils.getSchemaKey(topic, false);
        SchemaVersionInfo schemaVersionInfo;
        try {
            schemaMetadata = this.schemaRegistryClient.getSchemaMetadataInfo(schemaMetadata.getName()).getSchemaMetadata();
            schemaVersionInfo = this.schemaRegistryClient.getLatestSchemaVersionInfo(schemaMetadata.getName());
        } catch (SchemaNotFoundException e) {
            LOG.error("Exception occured while getting SchemaVersionInfo for " + schemaMetadata, e);
            throw new RuntimeException(e);
        }
        if (streamlineEvent == null || streamlineEvent.isEmpty()) {
            return null;
        } else {
            return avroSnapshotSerializer.serialize(getAvroGenericRecord(streamlineEvent, schemaVersionInfo.getSchemaText()), schemaMetadata);
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

    private static GenericRecord getAvroGenericRecord (Map<String, Object> streamlineEvent, String schemaText) {
        GenericRecord result;
        Schema schema = new Schema.Parser().parse(schemaText);
        result = new GenericData.Record(schema);
        for (Map.Entry<String, Object> entry: streamlineEvent.entrySet()) {
            result.put(entry.getKey(), getAvroValue(entry.getValue(), schema.getField(entry.getKey()).schema()));
        }
        return result;
    }

    private static Object getAvroValue(Object input, Schema schema) {
        if (input instanceof byte[]) {
            return new GenericData.Fixed(schema, (byte[]) input);
        } else if (input instanceof Map && !((Map) input).isEmpty()) {
            return getAvroGenericRecord((StreamlineEvent) input, schema.toString());
        } else if (input instanceof List && !((List) input).isEmpty()) {
            // for array even though we(Schema in streamline registry) support different types of elements in an array, avro expects an array
            // schema to have elements of same type. Hence, for now we will restrict array to have elements of same type. Other option is convert
            // a  streamline Schema Array field to Record in avro. However, with that the issue is that avro Field constructor does not allow a
            // null name. We could potentiall hack it by plugging in a dummy name like arrayfield, but seems hacky so not taking that path
            return new GenericData.Array<Object>(schema, (Collection<Object>) input);
        } else {
            return input;
        }
    }
}
