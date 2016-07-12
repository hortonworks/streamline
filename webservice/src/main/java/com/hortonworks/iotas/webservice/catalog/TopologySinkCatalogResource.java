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
package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.streams.catalog.TopologySink;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
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
 * Sink component within an IotasTopology
 */
@Path("/api/v1/catalog/topologies/{topologyId}/sinks")
@Produces(MediaType.APPLICATION_JSON)
public class TopologySinkCatalogResource {
    private StreamCatalogService catalogService;

    public TopologySinkCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * <p>
     * Lists all the sinks in the topology or the ones matching specific query params. For example to
     * list all the sinks in the topology,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/sinks</b>
     * <p>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entities": [{
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "hbasesink",
     *     "config": {
     *       "properties": {
     *         "fsUrl": "hdfs://localhost:9000"
     *       }
     *     },
     *     "type": "HBASE"
     *   }]
     * }
     * </pre>
     */
    @GET
    @Timed
    public Response listTopologySinks(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = WSUtils.buildTopologyIdAwareQueryParams(topologyId, uriInfo);

        try {
            Collection<TopologySink> sinks = catalogService.listTopologySinks(queryParams);
            if (sinks != null && !sinks.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, sinks);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    /**
     * <p>
     * Gets a specific topology sink by Id. For example,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/sources/:SINK_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "hbasesink",
     *     "config": {
     *       "properties": {
     *         "fsUrl": "hdfs://localhost:9000"
     *       }
     *     },
     *     "type": "HBASE"
     *   }
     * }
     * </pre>
     */
    @GET
    @Path("/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopologySinkById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sinkId) {
        try {
            TopologySink sink = catalogService.getTopologySink(sinkId);
            if (sink != null && sink.getTopologyId().equals(topologyId)) {
                return WSUtils.respond(OK, SUCCESS, sink);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(topologyId, sinkId));
    }

    /**
     * <p>
     * Creates a topology sink. For example,
     * </p>
     * <b>POST /api/v1/catalog/topologies/:TOPOLOGY_ID/sinks</b>
     * <pre>
     * {
     *   "name": "hbasesink",
     *   "config": {
     *     "properties": {
     *       "fsUrl": "hdfs://localhost:9000"
     *     }
     *   },
     *   "type": "HBASE"
     * }
     * </pre>
     * <i>Sample success response: </i>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "hbasesink",
     *     "config": {
     *       "properties": {
     *         "fsUrl": "hdfs://localhost:9000"
     *       }
     *     },
     *     "type": "HBASE"
     *   }
     * }
     * </pre>
     */
    @POST
    @Timed
    public Response addTopologySink(@PathParam("topologyId") Long topologyId, TopologySink topologySink) {
        try {
            TopologySink createdSink = catalogService.addTopologySink(topologyId, topologySink);
            return WSUtils.respond(CREATED, SUCCESS, createdSink);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>Updates a topology sink.</p>
     * <p>
     * <b>PUT /api/v1/catalog/topologies/:TOPOLOGY_ID/sources/:SINK_ID</b>
     * <pre>
     * {
     *   "name": "hbasesinkTest",
     *   "config": {
     *     "properties": {
     *       "fsUrl": "hdfs://localhost:9000"
     *     }
     *   },
     *   "type": "HBASE"
     * }
     * </pre>
     * <i>Sample success response: </i>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "hbasesinkTest",
     *     "config": {
     *       "properties": {
     *         "fsUrl": "hdfs://localhost:9000"
     *       }
     *     },
     *     "type": "HBASE"
     *   }
     * }
     * </pre>
     */
    @PUT
    @Path("/{id}")
    @Timed
    public Response addOrUpdateTopologySink(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sinkId,
                                              TopologySink topologySink) {
        try {
            TopologySink createdTopologySink = catalogService.addOrUpdateTopologySink(topologyId, sinkId, topologySink);
            return WSUtils.respond(CREATED, SUCCESS, createdTopologySink);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>
     * Removes a topology sink.
     * </p>
     * <b>DELETE /api/v1/catalog/topologies/:TOPOLOGY_ID/sources/:SINK_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "hbasesink",
     *     "config": {
     *       "properties": {
     *         "fsUrl": "hdfs://localhost:9000"
     *       }
     *     },
     *     "type": "HBASE"
     *   }
     * }
     * </pre>
     */
    @DELETE
    @Path("/{id}")
    @Timed
    public Response removeTopologySink(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sinkId) {
        try {
            TopologySink topologySink = catalogService.removeTopologySink(sinkId);
            if (topologySink != null) {
                return WSUtils.respond(OK, SUCCESS, topologySink);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, sinkId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private String buildMessageForCompositeId(Long topologyId, Long sinkId) {
        return String.format("topology id <%d>, sink id <%d>", topologyId, sinkId);
    }
}
