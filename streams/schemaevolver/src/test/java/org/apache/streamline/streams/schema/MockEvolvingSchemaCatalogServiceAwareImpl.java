package org.apache.streamline.streams.schema;

import com.google.common.collect.Sets;
import org.apache.streamline.common.Schema;
import org.apache.streamline.streams.catalog.service.CatalogService;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.layout.component.Stream;
import org.apache.streamline.streams.schema.exception.BadComponentConfigException;

import java.util.Set;

public class MockEvolvingSchemaCatalogServiceAwareImpl implements EvolvingSchema, CatalogServiceAware {
    private Set<Stream> streams;
    private CatalogService catalogService;
    private StreamCatalogService streamcatalogService;

    public MockEvolvingSchemaCatalogServiceAwareImpl() {
        streams = Sets.newHashSet();
        initializeAppliedResult();
    }

    @Override
    public Set<Stream> apply(String config, Stream inputStream) throws BadComponentConfigException {
        if (catalogService == null) {
            throw new RuntimeException("CatalogServiceAware is not respected!");
        }
        return streams;
    }

    public Set<Stream> getStreams() {
        return streams;
    }

    @Override
    public void setCatalogService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public void setStreamCatalogService(StreamCatalogService catalogService) {
        this.streamcatalogService = catalogService;
    }


    private void initializeAppliedResult() {
        Schema schema = new Schema.SchemaBuilder().field(new Schema.Field("field1", Schema.Type.STRING)).build();
        streams.add(new Stream("stream1", schema));
    }
}
