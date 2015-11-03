package com.hortonworks.iotas.webservice.util;

import com.hortonworks.iotas.catalog.CatalogResponse;
import com.hortonworks.iotas.service.CatalogService;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Utility methods for the webservice.
 */
public class WSUtils {
    private WSUtils() {
    }

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

    public static List<CatalogService.QueryParam> buildQueryParameters(MultivaluedMap<String, String> params) {
        if (params == null || params.isEmpty()) {
            return Collections.<CatalogService.QueryParam>emptyList();
        }

        List<CatalogService.QueryParam> queryParams = new ArrayList<>();
        for (String param : params.keySet()) {
            queryParams.add(new CatalogService.QueryParam(param, params.getFirst(param)));
        }
        return queryParams;
    }


}
