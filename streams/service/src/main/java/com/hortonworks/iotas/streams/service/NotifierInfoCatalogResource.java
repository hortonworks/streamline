package com.hortonworks.iotas.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.util.WSUtils;
import com.hortonworks.iotas.streams.catalog.NotifierInfo;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST endpoint for configuring notifiers
 */
@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class NotifierInfoCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(NotifierInfoCatalogResource.class);

    private final StreamCatalogService catalogService;

    public NotifierInfoCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * List ALL notifiers or the ones matching specific query params.
     */
    @GET
    @Path("/notifiers")
    @Timed
    public Response listNotifiers(@Context UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<NotifierInfo> notifierInfos;
            if (params.isEmpty()) {
                notifierInfos = catalogService.listNotifierInfos();
            } else {
                queryParams = WSUtils.buildQueryParameters(params);
                notifierInfos = catalogService.listNotifierInfos(queryParams);
            }
            if (notifierInfos != null) {
                return WSUtils.respond(notifierInfos, OK, SUCCESS);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/notifiers/{id}")
    @Timed
    public Response getNotifierById(@PathParam("id") Long id) {
        try {
            NotifierInfo result = catalogService.getNotifierInfo(id);
            if (result != null) {
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    @POST
    @Path("/notifiers")
    @Timed
    public Response addNotifier(NotifierInfo notifierInfo) {
        try {
            NotifierInfo created = catalogService.addNotifierInfo(notifierInfo);
            return WSUtils.respond(created, CREATED, SUCCESS);
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/notifiers/{id}")
    @Timed
    public Response removeNotifierInfo(@PathParam("id") Long id) {
        try {
            NotifierInfo removedNotifierInfo = catalogService.removeNotifierInfo(id);
            if (removedNotifierInfo != null) {
                return WSUtils.respond(removedNotifierInfo, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/notifiers/{id}")
    @Timed
    public Response addOrUpdateNotifierInfo(@PathParam("id") Long id, NotifierInfo notifierInfo) {
        try {
            NotifierInfo newNotifierInfo = catalogService.addOrUpdateNotifierInfo(id, notifierInfo);
            return WSUtils.respond(newNotifierInfo, OK, SUCCESS);
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }


}
