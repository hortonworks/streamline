package org.apache.streamline.streams.schema;

import com.google.common.collect.Sets;
import org.apache.registries.common.Schema;
import org.apache.streamline.streams.layout.component.Stream;
import org.apache.streamline.streams.schema.exception.BadComponentConfigException;

import java.util.Set;

public class MockEvolvingSchemaImpl implements EvolvingSchema {
    private Set<Stream> streams;

    public MockEvolvingSchemaImpl() {
        streams = Sets.newHashSet();
        initializeAppliedResult();
    }

    @Override
    public Set<Stream> apply(String config, Stream inputStream) throws BadComponentConfigException {
        return streams;
    }

    public Set<Stream> getStreams() {
        return streams;
    }

    private void initializeAppliedResult() {
        Schema schema = new Schema.SchemaBuilder().field(new Schema.Field("field1", Schema.Type.STRING)).build();
        Schema schema2 = new Schema.SchemaBuilder().field(new Schema.Field("field2", Schema.Type.LONG)).build();
        streams.add(new Stream("stream1", schema));
        streams.add(new Stream("stream2", schema2));
    }
}
