package org.apache.streamline.streams.schema;

import org.apache.streamline.common.Schema;
import org.apache.streamline.streams.common.StreamlineEventImpl;
import org.apache.streamline.streams.layout.component.Stream;

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

        return new Stream(StreamlineEventImpl.DEFAULT_SOURCE_STREAM, schemaBuilder.build());
    }
}
