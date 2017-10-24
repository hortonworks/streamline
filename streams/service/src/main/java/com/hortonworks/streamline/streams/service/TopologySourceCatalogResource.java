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
import com.hortonworks.registries.common.QueryParam;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologySource;
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
 * Source component within an StreamlineTopology
 */
@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologySourceCatalogResource {
    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;

    public TopologySourceCatalogResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
    }

    /**
     * <p>
     * Lists all the sources in the topology or the ones matching specific query params. For example to
     * list all the sources in the topology,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/sources</b>
     * <p>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entities": [{
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "kafkaDataSource",
     *     "config": {
     *       "properties": {
     *         "zkUrl": "localhost:2181",
     *         "zkPath": "/brokers",
     *         "refreshFreqSecs": 60
     *       }
     *     },
     *     "type": "KAFKA",
     *     "outputStreams": [{stream1 data..}, {stream2 data..}]
     *   }]
     * }
     * </pre>
     */
    @GET
    @Path("/topologies/{topologyId}/sources")
    @Timed
    public Response listTopologySources(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo,
                                        @Context SecurityContext securityContext) throws Exception {
        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);
        return listTopologySources(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, currentVersionId, uriInfo),
                topologyId,
                securityContext);
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/sources")
    @Timed
    public Response listTopologySourcesForVersion(@PathParam("topologyId") Long topologyId,
                                                  @PathParam("versionId") Long versionId,
                                                  @Context UriInfo uriInfo,
                                                  @Context SecurityContext securityContext) throws Exception {
        return listTopologySources(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, versionId, uriInfo),
                topologyId,
                securityContext);
    }

    private Response listTopologySources(List<QueryParam> queryParams, Long topologyId, SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        Collection<TopologySource> sources = catalogService.listTopologySources(queryParams);
        if (sources != null) {
            return WSUtils.respondEntities(sources, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    /**
     * <p>
     * Gets the 'CURRENT' version of specific topology source by Id. For example,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/sources/:SOURCE_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "kafkaDataSource",
     *     "config": {
     *       "properties": {
     *         "zkUrl": "localhost:2181",
     *         "zkPath": "/brokers",
     *         "refreshFreqSecs": 60
     *       }
     *     },
     *     "type": "KAFKA",
     *     "outputStreams": [{stream1 data..}, {stream2 data..}]
     *   }
     * }
     * </pre>
     */
    @GET
    @Path("/topologies/{topologyId}/sources/{id}")
    @Timed
    public Response getTopologySourceById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sourceId,
                                          @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        TopologySource source = catalogService.getTopologySource(topologyId, sourceId);
        if (source != null) {
            return WSUtils.respondEntity(source, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(topologyId, sourceId));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/sources/{id}")
    @Timed
    public Response getTopologySourceByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                                    @PathParam("id") Long sourceId,
                                                    @PathParam("versionId") Long versionId,
                                                    @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        TopologySource source = catalogService.getTopologySource(topologyId, sourceId, versionId);
        if (source != null) {
            return WSUtils.respondEntity(source, OK);
        }

        throw EntityNotFoundException.byVersion(buildMessageForCompositeId(topologyId, sourceId),
                versionId.toString());
    }

    /**
     * <p>
     * Creates a topology source. For example,
     * </p>
     * <b>POST /api/v1/catalog/topologies/:TOPOLOGY_ID/sources</b>
     * <pre>
     * {
     *   "name": "kafkaDataSource",
     *   "config": {
     *     "properties": {
     *       "zkUrl": "localhost:2181",
     *       "zkPath": "/brokers",
     *       "refreshFreqSecs": 60
     *     }
     *   },
     *   "type": "KAFKA",
     *
     *   "outputStreamIds": [1]
     *   OR
     *   "outputStreams" : [{stream1 data..}, {stream2 data..}]
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
     *     "name": "kafkaDataSource",
     *     "config": {
     *       "properties": {
     *         "zkUrl": "localhost:2181",
     *         "zkPath": "/brokers",
     *         "refreshFreqSecs": 60
     *       }
     *     },
     *     "type": "KAFKA",
     *     "outputStreamIds": [1] OR "outputStreams" : {..}
     *   }
     * }
     * </pre>
     */
    @POST
    @Path("/topologies/{topologyId}/sources")
    @Timed
    public Response addTopologySource(@PathParam("topologyId") Long topologyId, TopologySource topologySource,
                                      @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologySource createdSource = catalogService.addTopologySource(topologyId, topologySource);
        return WSUtils.respondEntity(createdSource, CREATED);
    }

    /**
     * <p>Updates a topology source.</p>
     * <p>
     * <b>PUT /api/v1/catalog/topologies/:TOPOLOGY_ID/sources/:SOURCE_ID</b>
     * <pre>
     * {
     *   "name": "kafkaDataSource",
     *   "config": {
     *     "properties": {
     *       "zkUrl": "localhost:2181",
     *       "zkPath": "/brokers",
     *       "refreshFreqSecs": 120
     *     }
     *   },
     *   "type": "KAFKA",
     *   "outputStreamIds": [1]
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
     *     "name": "kafkaDataSource",
     *     "config": {
     *       "properties": {
     *         "zkUrl": "localhost:2181",
     *         "zkPath": "/brokers",
     *         "refreshFreqSecs": 120
     *       }
     *     },
     *     "type": "KAFKA",
     *     "outputStreamIds": [1]
     *   }
     * }
     * </pre>
     */
    @PUT
    @Path("/topologies/{topologyId}/sources/{id}")
    @Timed
    public Response addOrUpdateTopologySource(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sourceId,
                                              TopologySource topologySource, @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologySource createdTopologySource = catalogService.addOrUpdateTopologySource(topologyId, sourceId, topologySource);
        return WSUtils.respondEntity(createdTopologySource, CREATED);
    }

    /**
     * <p>
     * Removes a topology source.
     * </p>
     * <b>DELETE /api/v1/catalog/topologies/:TOPOLOGY_ID/sources/:SOURCE_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "kafkaDataSource",
     *     "config": {
     *       "properties": {
     *         "zkUrl": "localhost:2181",
     *         "zkPath": "/brokers",
     *         "refreshFreqSecs": 60
     *       }
     *     },
     *     "type": "KAFKA",
     *     "outputStreams": [{stream1 data..}, {stream2 data..}]
     *   }
     * }
     * </pre>
     */
    @DELETE
    @Path("/topologies/{topologyId}/sources/{id}")
    @Timed
    public Response removeTopologySource(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sourceId,
                                         @javax.ws.rs.QueryParam("removeEdges") boolean removeEdges,
                                         @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologySource topologySource = catalogService.removeTopologySource(topologyId, sourceId, removeEdges);
        if (topologySource != null) {
            return WSUtils.respondEntity(topologySource, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(topologyId, sourceId));
    }

    private String buildMessageForCompositeId(Long topologyId, Long sourceId) {
        return String.format("topology id <%d>, source id <%d>", topologyId, sourceId);
    }
}