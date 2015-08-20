package com.hortonworks.iotas.webservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.catalog.CatalogResponse;
import com.hortonworks.iotas.storage.Storable;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;

/**
 * Utility methods for the webservice.
 */
public class WSUtils {
    private WSUtils() {}

    public static Response respond(Response.Status status, CatalogResponse.ResponseMessage msg, Collection<? extends Storable> storable, String... formatArgs) {
        return Response.status(status)
                .entity(CatalogResponse.newResponse(msg).entities(storable).format(formatArgs))
                .build();
    }

    public static Response respond(Response.Status status, CatalogResponse.ResponseMessage msg, Storable storable, String... formatArgs) {
        return Response.status(status)
                .entity(CatalogResponse.newResponse(msg).entity(storable).format(formatArgs))
                .build();
    }

    public static Response respond(Response.Status status, CatalogResponse.ResponseMessage msg, String... formatArgs) {
        return Response.status(status)
                .entity(CatalogResponse.newResponse(msg).entity(null).format(formatArgs))
                .build();
    }

}
