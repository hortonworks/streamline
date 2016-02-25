package com.hortonworks.iotas.layout.schema;

import com.hortonworks.iotas.service.CatalogService;

public interface CatalogServiceAware {
    void setCatalogService(CatalogService catalogService);
}
