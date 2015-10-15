package com.hortonworks.iotas.webservice.util;

import com.hortonworks.iotas.catalog.CatalogResponse;

import javax.ws.rs.core.Response;
import java.util.Collection;

/**
 * Utility methods for the webservice.
 */
public class WSUtils {
    private WSUtils() {}

    public static Response respond(Response.Status status, CatalogResponse.ResponseMessage msg, Collection<? extends Object> entities, String... formatArgs) {
        return Response.status(status)
                .entity(CatalogResponse.newResponse(msg).entities(entities).format(formatArgs))
                .build();
    }

    public static Response respond(Response.Status status, CatalogResponse.ResponseMessage msg, Object entity, String... formatArgs) {
        return Response.status(status)
                .entity(CatalogResponse.newResponse(msg).entity(entity).format(formatArgs))
                .build();
    }

    public static Response respond(Response.Status status, CatalogResponse.ResponseMessage msg, String... formatArgs) {
        return Response.status(status)
                .entity(CatalogResponse.newResponse(msg).entity(null).format(formatArgs))
                .build();
    }

}
