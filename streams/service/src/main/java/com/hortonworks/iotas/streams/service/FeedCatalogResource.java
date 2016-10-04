package com.hortonworks.iotas.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.common.util.WSUtils;
import com.hortonworks.iotas.registries.parser.ParserInfo;
import com.hortonworks.iotas.streams.catalog.DataFeed;
import com.hortonworks.iotas.streams.catalog.service.CatalogService;

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
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.PARSER_SCHEMA_FOR_ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

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
        List<QueryParam> queryParams = new ArrayList<QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<DataFeed> dataFeeds;
            if (params.isEmpty()) {
                dataFeeds = catalogService.listDataFeeds();
            } else {
                queryParams = WSUtils.buildQueryParameters(params);
                dataFeeds = catalogService.listDataFeeds(queryParams);
            }
            if (dataFeeds != null && !dataFeeds.isEmpty()) {
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

    @GET
    @Path("/feeds/{id}/schema")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getParserSchemaForDatafeed(@PathParam("id") Long dataFeedId) {
        try {
            DataFeed dataFeed = catalogService.getDataFeed(dataFeedId);
            if (dataFeed != null) {
                ParserInfo parserInfo = catalogService.getParserInfo(dataFeed.getParserId());
                Schema result = parserInfo.getParserSchema();
                if (result != null) {
                    return WSUtils.respond(OK, SUCCESS, result);
                }
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, PARSER_SCHEMA_FOR_ENTITY_NOT_FOUND, dataFeedId.toString());
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
