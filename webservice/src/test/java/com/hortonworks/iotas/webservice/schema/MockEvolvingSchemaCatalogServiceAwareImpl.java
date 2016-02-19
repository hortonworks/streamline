package com.hortonworks.iotas.webservice.schema;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.schema.BadComponentConfigException;
import com.hortonworks.iotas.layout.schema.CatalogServiceAware;
import com.hortonworks.iotas.layout.schema.EvolvingSchema;
import com.hortonworks.iotas.service.CatalogService;

import java.util.HashMap;
import java.util.Map;

public class MockEvolvingSchemaCatalogServiceAwareImpl implements EvolvingSchema, CatalogServiceAware {
    private Map<String, Schema> streamToSchemaMap;
    private CatalogService catalogService;

    public MockEvolvingSchemaCatalogServiceAwareImpl() {
        streamToSchemaMap = new HashMap<>();
        initializeAppliedResult();
    }

    @Override
    public Map<String, Schema> apply(String config, Schema inputSchema) throws BadComponentConfigException {
        if (catalogService == null) {
            throw new RuntimeException("CatalogServiceAware is not respected!");
        }
        return streamToSchemaMap;
    }

    public Map<String, Schema> getStreamToSchemaMap() {
        return streamToSchemaMap;
    }

    @Override
    public void setCatalogService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    private void initializeAppliedResult() {
        Schema schema = new Schema.SchemaBuilder().field(new Schema.Field("field1", Schema.Type.STRING)).build();
        streamToSchemaMap.put("stream1", schema);
    }
}
