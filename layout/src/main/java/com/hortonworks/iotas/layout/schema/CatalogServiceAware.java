package com.hortonworks.iotas.layout.schema;


import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;

/**
 * An interface that support injection CatalogService instance.
 * Injection will be held after initializing EvolvingSchema instance.
 */
public interface CatalogServiceAware {
    /**
     * Injection point. This method will be called after initializing EvolvingSchema instance.
     *
     * @param catalogService CatalogService instance
     */
    void setCatalogService(CatalogService catalogService);

    /**
     * Injection point. This method will be called after initializing EvolvingSchema instance.
     *
     * @param catalogService CatalogService instance
     */
    void setStreamCatalogService(StreamCatalogService catalogService);

}
