package com.hortonworks.iotas.webservice.schema;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.schema.BadComponentConfigException;
import com.hortonworks.iotas.layout.schema.EvolvingSchema;

import java.util.HashMap;
import java.util.Map;

public class MockEvolvingSchemaImpl implements EvolvingSchema {
    private Map<String, Schema> streamToSchemaMap;

    public MockEvolvingSchemaImpl() {
        streamToSchemaMap = new HashMap<>();
        initializeAppliedResult();
    }

    @Override
    public Map<String, Schema> apply(String config, Schema inputSchema) throws BadComponentConfigException {
        return streamToSchemaMap;
    }

    public Map<String, Schema> getStreamToSchemaMap() {
        return streamToSchemaMap;
    }

    private void initializeAppliedResult() {
        Schema schema = new Schema.SchemaBuilder().field(new Schema.Field("field1", Schema.Type.STRING)).build();
        Schema schema2 = new Schema.SchemaBuilder().field(new Schema.Field("field2", Schema.Type.LONG)).build();
        streamToSchemaMap.put("stream1", schema);
        streamToSchemaMap.put("stream2", schema2);
    }
}
