/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.util.WSUtils;
import com.hortonworks.iotas.streams.catalog.StreamInfo;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.*;
import static javax.ws.rs.core.Response.Status.*;

/**
 * Represents output stream from a source or a processor component
 * in an IotasTopology
 */
@Path("/api/v1/catalog/topologies/{topologyId}/streams")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyStreamCatalogResource {
    private StreamCatalogService catalogService;

    public TopologyStreamCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * <p>
     * Lists all the streams in the topology or the ones matching specific query params. For example to
     * list all the streams in the topology,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/streams</b>
     * <p>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entities": [
     *     {
     *       "id": 1,
     *       "topologyId": 1,
     *       "streamId": "default",
     *       "fields": [
     *         {"name": "f1", "type": "STRING", "optional": false},
     *         {"name": "f2", "type": "LONG", "optional": false}
     *       ],
     *       "timestamp": 1463238366216
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
    @Timed
    public Response listStreamInfos(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = WSUtils.buildTopologyIdAwareQueryParams(topologyId, uriInfo);
        try {
            Collection<StreamInfo> streamInfos = catalogService.listStreamInfos(queryParams);
            if (streamInfos != null) {
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
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/streams/:STREAM_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "streamId": "a",
     *     "fields": [
     *       {"name": "f1", "type": "STRING", "optional": false},
     *       {"name": "f2", "type": "LONG", "optional": false}
     *       ],
     *     "timestamp": 1463238366216
     *   }
     * }
     * </pre>
     *
     * @param id the stream id
     * @return the response
     */
    @GET
    @Path("/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStreamInfoById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long id) {
        try {
            StreamInfo streamInfo = catalogService.getStreamInfo(id);
            if (streamInfo != null && streamInfo.getTopologyId().equals(topologyId)) {
                return WSUtils.respond(OK, SUCCESS, streamInfo);
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
     * <b>POST /api/v1/catalog/topologies/:TOPOLOGY_ID/streams</b>
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
     *     "topologyId": 1,
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
     *     ],
     *     "timestamp": 1463238366216
     *   }
     * }
     * </pre>
     */
    @POST
    @Timed
    public Response addStreamInfo(@PathParam("topologyId") Long topologyId, StreamInfo streamInfo) {
        try {
            StreamInfo createdStream = catalogService.addStreamInfo(topologyId, streamInfo);
            return WSUtils.respond(CREATED, SUCCESS, createdStream);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>Updates a stream in the topology.</p>
     * <p>
     * <b>PUT /api/v1/catalog/topologies/:TOPOLOGY_ID/streams/:STREAM_ID</b>
     * <pre>
     * {
     *   "streamId": "default",
     *   "topologyId": 1,
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
     *     "topologyId": 1,
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
     *     ],
     *     "timestamp": 1463238891203
     *   }
     * }
     * </pre>
     *
     * @param id  the id of the stream to be updated
     * @param streamInfo the updated StreamInfo object
     * @return the response
     */
    @PUT
    @Path("/{id}")
    @Timed
    public Response addOrUpdateStreamInfo(@PathParam("topologyId") Long topologyId, @PathParam("id") Long id, StreamInfo streamInfo) {
        try {
            StreamInfo newStreamInfo = catalogService.addOrUpdateStreamInfo(topologyId, id, streamInfo);
            return WSUtils.respond(OK, SUCCESS, newStreamInfo);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>
     * Removes a stream resource.
     * </p>
     * <b>DELETE /api/v1/catalog/topologies/:TOPOLOGY_ID/streams/:STREAM_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "streamId": "default",
     *     "topologyId": 1,
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
     *     ],
     *     "timestamp": 1463238609751
     *   }
     * }
     * </pre>
     */
    @DELETE
    @Path("/{id}")
    @Timed
    public Response removeStreamInfo(@PathParam("topologyId") Long topologyId, @PathParam("id") Long id) {
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
