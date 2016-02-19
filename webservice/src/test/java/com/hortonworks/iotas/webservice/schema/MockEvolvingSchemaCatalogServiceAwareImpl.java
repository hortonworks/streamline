package com.hortonworks.iotas.webservice.schema;

import com.google.common.collect.Sets;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.layout.schema.BadComponentConfigException;
import com.hortonworks.iotas.layout.schema.CatalogServiceAware;
import com.hortonworks.iotas.layout.schema.EvolvingSchema;
import com.hortonworks.iotas.service.CatalogService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MockEvolvingSchemaCatalogServiceAwareImpl implements EvolvingSchema, CatalogServiceAware {
    private Set<Stream> streams;
    private CatalogService catalogService;

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

    private void initializeAppliedResult() {
        Schema schema = new Schema.SchemaBuilder().field(new Schema.Field("field1", Schema.Type.STRING)).build();
        streams.add(new Stream("stream1", schema));
    }
}
