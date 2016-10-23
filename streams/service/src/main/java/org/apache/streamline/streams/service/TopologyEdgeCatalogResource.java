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
package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.TopologyEdge;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;

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

import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND_FOR_FILTER;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * An edge between two components in an StreamlineTopology
 */
@Path("/api/v1/catalog/topologies/{topologyId}/edges")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyEdgeCatalogResource {
    private final StreamCatalogService catalogService;

    public TopologyEdgeCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * <p>
     * Lists all the edges in the topology or the ones matching specific query params. For example to
     * list all the edges in the topology,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/edges</b>
     * <p>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entities": [{
     *     "id": 1,
     *     "topologyId": 1,
     *     "fromId": 1,
     *     "toId": 1,
     *     "streamGroupings": [
     *       {
     *         "streamId": 1,
     *         "grouping": "SHUFFLE"
     *       }
     *     ]
     *   }]
     * }
     * </pre>
     */
    @GET
    @Timed
    public Response listTopologyEdges(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = WSUtils.buildTopologyIdAwareQueryParams(topologyId, uriInfo);
        try {
            Collection<TopologyEdge> edges = catalogService.listTopologyEdges(queryParams);
            if (edges != null) {
                return WSUtils.respond(edges, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND_FOR_FILTER, queryParams.toString());
    }

    /**
     * <p>
     * Gets a specific topology edge by Id. For example,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/edges/:EDGE_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "fromId": 1,
     *     "toId": 1,
     *     "streamGroupings": [
     *       {
     *         "streamId": 1,
     *         "grouping": "SHUFFLE"
     *       }
     *     ]
     *   }
     * }
     * </pre>
     */
    @GET
    @Path("/{id}")
    @Timed
    public Response getTopologyEdgeById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long edgeId) {
        try {
            TopologyEdge edge = catalogService.getTopologyEdge(edgeId);
            if (edge != null && edge.getTopologyId().equals(topologyId)) {
                return WSUtils.respond(edge, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, buildMessageForCompositeId(topologyId, edgeId));
    }

    /**
     * <p>
     * Creates a topology edge. For example,
     * </p>
     * <b>POST /api/v1/catalog/topologies/:TOPOLOGY_ID/edges</b>
     * <pre>
     * {
     *   "fromId": 1,
     *   "toId": 1,
     *   "streamGroupings": [{"streamId": 1, "grouping": "SHUFFLE"}]
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
     *     "fromId": 1,
     *     "toId": 1,
     *     "streamGroupings": [
     *       {
     *         "streamId": 1,
     *         "grouping": "SHUFFLE",
     *         "fields": ["a", "b"]
     *       }
     *     ]
     *   }
     * }
     * </pre>
     */
    @POST
    @Timed
    public Response addTopologyEdge(@PathParam("topologyId") Long topologyId, TopologyEdge edge) {
        try {
            TopologyEdge createdEdge = catalogService.addTopologyEdge(topologyId, edge);
            return WSUtils.respond(createdEdge, CREATED, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>Updates a topology edge.</p>
     * <p>
     * <b>PUT /api/v1/catalog/topologies/:TOPOLOGY_ID/edges/:EDGE_ID</b>
     * <pre>
     * {
     *   "fromId": 1,
     *   "toId": 2,
     *   "streamGroupings": [{"streamId": 1, "grouping": "SHUFFLE"}]
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
     *     "fromId": 1,
     *     "toId": 2,
     *     "streamGroupings": [
     *       {
     *         "streamId": 1,
     *         "grouping": "SHUFFLE"
     *       }
     *     ]
     *   }
     * }
     * </pre>
     */
    @PUT
    @Path("/{id}")
    @Timed
    public Response addOrUpdateTopologyEdge(@PathParam("topologyId") Long topologyId, @PathParam("id") Long edgeId,
                                                 TopologyEdge edge) {
        try {
            TopologyEdge createdEdge = catalogService.addOrUpdateTopologyEdge(topologyId, edgeId, edge);
            return WSUtils.respond(createdEdge, CREATED, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * <p>
     * Removes a topology edge.
     * </p>
     * <b>DELETE /api/v1/catalog/topologies/:TOPOLOGY_ID/edges/:EDGE_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "fromId": 1,
     *     "toId": 1,
     *     "streamGroupings": [
     *       {
     *         "streamId": 1,
     *         "grouping": "SHUFFLE"
     *       }
     *     ]
     *   }
     * }
     * </pre>
     */
    @DELETE
    @Path("/{id}")
    @Timed
    public Response removeTopologyEdge(@PathParam("topologyId") Long topologyId, @PathParam("id") Long edgeId) {
        try {
            TopologyEdge removedEdge = catalogService.removeTopologyEdge(edgeId);
            if (removedEdge != null) {
                return WSUtils.respond(removedEdge, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, edgeId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private String buildMessageForCompositeId(Long topologyId, Long edgeId) {
        return String.format("topology id <%d>, edge id <%d>", topologyId, edgeId);
    }

}
