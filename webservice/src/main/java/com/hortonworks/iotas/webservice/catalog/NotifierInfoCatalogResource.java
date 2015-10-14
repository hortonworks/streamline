package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.NotifierInfo;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.util.WSUtils;
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

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * REST endpoint for configuring notifiers
 */
@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class NotifierInfoCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(NotifierInfoCatalogResource.class);

    private CatalogService catalogService;

    public NotifierInfoCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * List ALL notifiers or the ones matching specific query params.
     */
    @GET
    @Path("/notifiers")
    @Timed
    public Response listNotifiers(@Context UriInfo uriInfo) {
        List<CatalogService.QueryParam> queryParams = new ArrayList<CatalogService.QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<NotifierInfo> notifierInfos;
            if (params.isEmpty()) {
                notifierInfos = catalogService.listNotifierInfos();
            } else {
                for (String param : params.keySet()) {
                    queryParams.add(new CatalogService.QueryParam(param, params.getFirst(param)));
                }
                notifierInfos = catalogService.listNotifierInfos(queryParams);
            }
            if(notifierInfos != null && ! notifierInfos.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, notifierInfos);
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNotifierById(@PathParam("id") Long id) {
        try {
            NotifierInfo result = catalogService.getNotifierInfo(id);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
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
            return WSUtils.respond(CREATED, SUCCESS, created);
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
                return WSUtils.respond(OK, SUCCESS, removedNotifierInfo);
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
            return WSUtils.respond(OK, SUCCESS, newNotifierInfo);
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }


}
