package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.StreamInfo;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.util.WSUtils;

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

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class StreamCatalogResource {
    private CatalogService catalogService;

    public StreamCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * <p>
     * Lists all the streams in the system or the ones matching specific query params. For example to
     * list all the streams in the system,
     * </p>
     * <b>GET /api/v1/catalog/streams</b>
     * <p>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entities": [
     *     {
     *       "id": 1,
     *       "streamId": "default",
     *       "fields": [
     *         {"name": "f1", "type": "STRING", "optional": false},
     *         {"name": "f2", "type": "LONG", "optional": false}
     *       ]
     *     },
     *     {
     *     ..
     *     ..
     *     }
     *   ]
     * }
     * </pre>
     */
    @GET
    @Path("/streams")
    @Timed
    public Response listStreamInfos(@Context UriInfo uriInfo) {
        List<CatalogService.QueryParam> queryParams = new ArrayList<CatalogService.QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            Collection<StreamInfo> streamInfos;
            if (params.isEmpty()) {
                streamInfos = catalogService.listStreamInfos();
            } else {
                queryParams = WSUtils.buildQueryParameters(params);
                streamInfos = catalogService.listStreamInfos(queryParams);
            }
            if (streamInfos != null && !streamInfos.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, streamInfos);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    /**
     * <p>
     * Gets a specific stream by Id. For example,
     * </p>
     * <b>GET /api/v1/catalog/streams/1</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "streamId": "a",
     *     "fields": [
     *       {"name": "f1", "type": "STRING", "optional": false},
     *       {"name": "f2", "type": "LONG", "optional": false}
     *       ]
     *   }
     * }
     * </pre>
     *
     * @param id the stream id
     * @return the response
     */
    @GET
    @Path("/streams/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStreamInfoById(@PathParam("id") Long id) {
        try {
            StreamInfo result = catalogService.getStreamInfo(id);
            if (result != null) {
                return WSUtils.respond(OK, SUCCESS, result);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    /**
     * <p>
     * Creates a stream. For example,
     * </p>
     * <b>POST /api/v1/catalog/streams</b>
     * <pre>
     * {
     *   "streamId": "default",
     *   "fields": [
     *     {"name": "f1", "type": "STRING"},
     *     {"name": "f2", "type": "LONG"}
     *   ]
     * }
     * </pre>
     * <i>Sample success response: </i>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "streamId": "default",
     *     "fields": [
     *       {
     *         "name": "f1",
     *         "type": "STRING",
     *         "optional": false
     *       },
     *       {
     *         "name": "f2",
     *         "type": "LONG",
     *         "optional": false
     *       }
     *     ]
     *   }
     * }
     * </pre>
     */
    @POST
    @Path("/streams")
    @Timed
    public Response addStreamInfo(StreamInfo streamInfo) {
        try {
            StreamInfo createdStream = catalogService.addStreamInfo(streamInfo);
            return WSUtils.respond(CREATED, SUCCESS, createdStream);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>Updates a stream in the system.</p>
     * <p>
     * <b>PUT /api/v1/catalog/streams/1</b>
     * <pre>
     * {
     *   "streamId": "default",
     *   "fields": [
     *     {"name": "f1", "type": "STRING"},
     *     {"name": "f2", "type": "LONG"},
     *     {"name": "f3", "type": "STRING"}
     *   ]
     * }
     * </pre>
     * <i>Sample success response: </i>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "streamId": "default",
     *     "fields": [
     *       {
     *         "name": "f1",
     *         "type": "STRING",
     *         "optional": false
     *       },
     *       {
     *         "name": "f2",
     *         "type": "LONG",
     *         "optional": false
     *       },
     *       {
     *         "name": "f3",
     *         "type": "STRING",
     *         "optional": false
     *       }
     *     ]
     *   }
     * }
     * </pre>
     *
     * @param id  the id of the stream to be updated
     * @param streamInfo the updated StreamInfo object
     * @return the response
     */
    @PUT
    @Path("/streams/{id}")
    @Timed
    public Response addOrUpdateStreamInfo(@PathParam("id") Long id, StreamInfo streamInfo) {
        try {
            StreamInfo newStreamInfo = catalogService.addOrUpdateStreamInfo(id, streamInfo);
            return WSUtils.respond(OK, SUCCESS, newStreamInfo);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>
     * Removes a stream resource.
     * </p>
     * <b>DELETE /api/v1/catalog/streams/1</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "streamId": "default",
     *     "fields": [
     *       {
     *         "name": "f1",
     *         "type": "STRING",
     *         "optional": false
     *       },
     *       {
     *         "name": "f2",
     *         "type": "LONG",
     *         "optional": false
     *       }
     *     ]
     *   }
     * }
     * </pre>
     */
    @DELETE
    @Path("/streams/{id}")
    @Timed
    public Response removeStreamInfo(@PathParam("id") Long id) {
        try {
            StreamInfo removedStream = catalogService.removeStreamInfo(id);
            if (removedStream != null) {
                return WSUtils.respond(OK, SUCCESS, removedStream);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

}
