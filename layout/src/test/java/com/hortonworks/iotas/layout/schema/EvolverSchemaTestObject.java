package com.hortonworks.iotas.layout.schema;

import com.hortonworks.iotas.common.Schema;

public class EvolverSchemaTestObject {
    public static Schema inputSchema() {
        Schema.SchemaBuilder schemaBuilder = new Schema.SchemaBuilder();

        schemaBuilder.field(new Schema.Field("field1", Schema.Type.STRING));
        schemaBuilder.field(new Schema.Field("field2", Schema.Type.LONG));
        schemaBuilder.field(new Schema.Field("field3", Schema.Type.STRING));

        return schemaBuilder.build();
    }
}
