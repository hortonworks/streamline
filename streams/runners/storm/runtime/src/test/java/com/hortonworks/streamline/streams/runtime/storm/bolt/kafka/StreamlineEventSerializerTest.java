package com.hortonworks.streamline.streams.runtime.storm.bolt.kafka;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreamlineEventSerializerTest {

    private static final Object[] PRIMITIVE_VALUES = { new Boolean(true), new String("STRINGVALUE"), new Integer(0), new Long(0l), new Double(0.0), "bytes"
            .getBytes() };
    private static final Schema.Type[] SCHEMA_TYPES = { Schema.Type.BOOLEAN, Schema.Type.STRING, Schema.Type.INT, Schema.Type.LONG, Schema.Type.DOUBLE,
            Schema.Type.BYTES};
    private static final String[] NAMES = {"bool", "str", "int", "long", "double", "bytes"};

    @Test
    public void testPrimitives () {
        for (int i = 0; i < PRIMITIVE_VALUES.length; ++i) {
            Map<String, Object> data = new HashMap<>();
            data.put(StreamlineEvent.PRIMITIVE_PAYLOAD_FIELD, PRIMITIVE_VALUES[i]);
            runPrimitiveTest(data, Schema.create(SCHEMA_TYPES[i]), PRIMITIVE_VALUES[i]);
        }
    }

    @Test
    public void testRecordWithFixed () {
        List<Schema.Field> fields = new ArrayList<>();
        Map<String, Object> data = new HashMap<>();
        for (int i = 0; i < PRIMITIVE_VALUES.length; ++i) {
            Schema.Field field = new Schema.Field(NAMES[i], Schema.create(SCHEMA_TYPES[i]), null, null);
            fields.add(field);
            data.put(NAMES[i], PRIMITIVE_VALUES[i]);
        }

        //add fixed to test case
        fields.add(new Schema.Field("fixed", Schema.createFixed("fixedSchema", null, null, 10), null, null));
        data.put("fixed", "bytes".getBytes());

        //add array to test case
        fields.add(new Schema.Field("array", Schema.createArray(Schema.create(Schema.Type.INT)), null, null));
        List<Integer> integerList = new ArrayList<>();
        integerList.add(1);
        integerList.add(2);
        data.put("array", integerList);

        Schema schema = Schema.createRecord(fields);
        GenericRecord expected = new GenericData.Record(schema);
        for (int i = 0; i < PRIMITIVE_VALUES.length; ++i) {
            expected.put(NAMES[i], PRIMITIVE_VALUES[i]);
        }
        expected.put("fixed", new GenericData.Fixed(Schema.createFixed("fixedSchema", null, null, 10), "bytes".getBytes()));
        expected.put("array", new GenericData.Array<Integer>(Schema.createArray(Schema.create(Schema.Type.INT)), integerList));
        StreamlineEvent streamlineEvent = new StreamlineEventImpl(data, "dataSourceId");
        Assert.assertEquals(expected, StreamlineEventSerializer.getAvroRecord(streamlineEvent, schema));
    }

    private void runPrimitiveTest (Map data, Schema schema, Object expectedValue) {
        StreamlineEvent streamlineEvent = new StreamlineEventImpl(data, "dataSourceId");
        Assert.assertEquals(expectedValue, StreamlineEventSerializer.getAvroRecord(streamlineEvent, schema));
    }


}
