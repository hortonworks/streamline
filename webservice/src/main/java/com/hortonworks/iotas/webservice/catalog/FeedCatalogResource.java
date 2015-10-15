package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.util.WSUtils;

import javax.ws.rs.*;
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

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class FeedCatalogResource {
    private CatalogService catalogService;

    public FeedCatalogResource(CatalogService service) {
        this.catalogService = service;
    }

    /**
     * List ALL data feeds or the ones matching specific query params.
     */
    @GET
    @Path("/feeds")
    @Timed
    public Response listDataFeeds(@Context UriInfo uriInfo) {
        List<CatalogService.QueryParam> queryParams = new ArrayList<CatalogService.QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<DataFeed> dataFeeds;
            if (params.isEmpty()) {
                dataFeeds = catalogService.listDataFeeds();
            } else {
                for (String param : params.keySet()) {
                    queryParams.add(new CatalogService.QueryParam(param, params.getFirst(param)));
                }
                dataFeeds = catalogService.listDataFeeds(queryParams);
            }
            if(dataFeeds != null && ! dataFeeds.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, dataFeeds);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    @GET
    @Path("/feeds/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataFeedById(@PathParam("id") Long dataFeedId) {
        try {
            DataFeed result = catalogService.getDataFeed(dataFeedId);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataFeedId.toString());
    }

    @POST
    @Path("/feeds")
    @Timed
    public Response addDataFeed(DataFeed feed) {
        try {
            DataFeed addedFeed = catalogService.addDataFeed(feed);
            return WSUtils.respond(CREATED, SUCCESS, addedFeed);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/feeds/{id}")
    @Timed
    public Response removeDatafeed(@PathParam("id") Long dataFeedId) {
        try {
            DataFeed removedDatafeed = catalogService.removeDataFeed(dataFeedId);
            if (removedDatafeed != null) {
                return WSUtils.respond(OK, SUCCESS, removedDatafeed);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataFeedId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/feeds/{id}")
    @Timed
    public Response addOrUpdateDataFeed(@PathParam("id") Long dataFeedId, DataFeed feed) {
        try {
            DataFeed newDataFeed = catalogService.addOrUpdateDataFeed(dataFeedId, feed);
            return WSUtils.respond(OK, SUCCESS, feed);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }
}
