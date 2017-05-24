/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/

package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyEdge;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;

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
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.streamline.common.util.WSUtils.buildTopologyIdAndVersionIdAwareQueryParams;
import static com.hortonworks.streamline.streams.security.Permission.READ;
import static com.hortonworks.streamline.streams.security.Permission.WRITE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * An edge between two components in an StreamlineTopology
 */
@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyEdgeCatalogResource {
    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;

    public TopologyEdgeCatalogResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService) {
        this.authorizer = authorizer;
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
    @Path("/topologies/{topologyId}/edges")
    @Timed
    public Response listTopologyEdges(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo,
                                      @Context SecurityContext securityContext) throws Exception {
        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);
        return listTopologyEdges(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, currentVersionId, uriInfo),
                topologyId,
                securityContext);
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/edges")
    @Timed
    public Response listTopologyEdgesForVersion(@PathParam("topologyId") Long topologyId,
                                                @PathParam("versionId") Long versionId,
                                                @Context UriInfo uriInfo,
                                                @Context SecurityContext securityContext) throws Exception {
        return listTopologyEdges(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, versionId, uriInfo),
                topologyId,
                securityContext);
    }

    private Response listTopologyEdges(List<QueryParam> queryParams, Long topologyId, SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        Collection<TopologyEdge> edges = catalogService.listTopologyEdges(queryParams);
        if (edges != null) {
            return WSUtils.respondEntities(edges, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    /**
     * <p>
     * Gets the 'CURRENT' version of specific topology edge by Id. For example,
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
    @Path("/topologies/{topologyId}/edges/{id}")
    @Timed
    public Response getTopologyEdgeById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long edgeId,
                                        @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        TopologyEdge edge = catalogService.getTopologyEdge(topologyId, edgeId);
        if (edge != null) {
            return WSUtils.respondEntity(edge, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(topologyId, edgeId));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/edges/{id}")
    @Timed
    public Response getTopologyEdgeByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                                  @PathParam("id") Long edgeId,
                                                  @PathParam("versionId") Long versionId,
                                                  @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        TopologyEdge edge = catalogService.getTopologyEdge(topologyId, edgeId, versionId);
        if (edge != null) {
            return WSUtils.respondEntity(edge, OK);
        }

        throw EntityNotFoundException.byVersion(buildMessageForCompositeId(topologyId, edgeId),
                versionId.toString());
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
    @Path("/topologies/{topologyId}/edges")
    @Timed
    public Response addTopologyEdge(@PathParam("topologyId") Long topologyId, TopologyEdge edge,
                                    @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologyEdge createdEdge = catalogService.addTopologyEdge(topologyId, edge);
        return WSUtils.respondEntity(createdEdge, CREATED);
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
    @Path("/topologies/{topologyId}/edges/{id}")
    @Timed
    public Response addOrUpdateTopologyEdge(@PathParam("topologyId") Long topologyId, @PathParam("id") Long edgeId,
                                            TopologyEdge edge, @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologyEdge createdEdge = catalogService.addOrUpdateTopologyEdge(topologyId, edgeId, edge);
        return WSUtils.respondEntity(createdEdge, CREATED);
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
    @Path("/topologies/{topologyId}/edges/{id}")
    @Timed
    public Response removeTopologyEdge(@PathParam("topologyId") Long topologyId, @PathParam("id") Long edgeId,
                                       @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologyEdge removedEdge = catalogService.removeTopologyEdge(topologyId, edgeId);
        if (removedEdge != null) {
            return WSUtils.respondEntity(removedEdge, OK);
        }

        throw EntityNotFoundException.byId(edgeId.toString());
    }

    private String buildMessageForCompositeId(Long topologyId, Long edgeId) {
        return String.format("topology id <%d>, edge id <%d>", topologyId, edgeId);
    }

}