package com.hortonworks.iotas.layout.schema;

import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.Stream;

/**
 * Test support class of EvolvingSchema
 */
public class EvolvingSchemaTestObject {
    /**
     * Creates test instance of 'Stream'.
     *
     * @return Stream instance.
     */
    public static Stream inputStream() {
        Schema.SchemaBuilder schemaBuilder = new Schema.SchemaBuilder();

        schemaBuilder.field(new Schema.Field("field1", Schema.Type.STRING));
        schemaBuilder.field(new Schema.Field("field2", Schema.Type.LONG));
        schemaBuilder.field(new Schema.Field("field3", Schema.Type.STRING));

        return new Stream(IotasEventImpl.DEFAULT_SOURCE_STREAM, schemaBuilder.build());
    }
}
