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
import com.hortonworks.streamline.streams.catalog.TopologyProcessor;
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
 * Processor component within an StreamlineTopology
 */
@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyProcessorCatalogResource {
    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;

    public TopologyProcessorCatalogResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
    }

    /**
     * <p>
     * Lists all the processors in the topology or the ones matching specific query params. For example to
     * list all the processors in the topology,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/processors</b>
     * <p>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entities": [{
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "ParserProcessor",
     *     "config": {
     *       "properties": {
     *         "parallelism": 1
     *       }
     *     },
     *     "type": "PARSER",
     *     "outputStreams": [{stream1 data..}, {stream2 data..}]
     *   }]
     * }
     * </pre>
     */
    @GET
    @Path("/topologies/{topologyId}/processors")
    @Timed
    public Response listTopologyProcessors(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo,
                                           @Context SecurityContext securityContext) throws Exception {
        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);
        return listTopologyProcessors(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, currentVersionId, uriInfo),
                topologyId,
                securityContext);
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/processors")
    @Timed
    public Response listTopologyProcessorsForVersion(@PathParam("topologyId") Long topologyId,
                                                     @PathParam("versionId") Long versionId,
                                                     @Context UriInfo uriInfo,
                                                     @Context SecurityContext securityContext) throws Exception {
        return listTopologyProcessors(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, versionId, uriInfo),
                topologyId,
                securityContext);
    }

    private Response listTopologyProcessors(List<QueryParam> queryParams, Long topologyId, SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        Collection<TopologyProcessor> sources = catalogService.listTopologyProcessors(queryParams);
        if (sources != null) {
            return WSUtils.respondEntities(sources, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    /**
     * <p>
     * Gets the 'CURRENT' version of specific topology processor by Id. For example,
     * </p>
     * <b>GET /api/v1/catalog/topologies/:TOPOLOGY_ID/processors/:PROCESSOR_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "ParserProcessor",
     *     "config": {
     *       "properties": {
     *         "parallelism": 1
     *       }
     *     },
     *     "type": "PARSER",
     *     "outputStreams": [{stream1 data..}, {stream2 data..}]
     *   }
     * }
     * </pre>
     */
    @GET
    @Path("/topologies/{topologyId}/processors/{id}")
    @Timed
    public Response getTopologyProcessorById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long processorId,
                                             @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        TopologyProcessor source = catalogService.getTopologyProcessor(topologyId, processorId);
        if (source != null) {
            return WSUtils.respondEntity(source, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(topologyId, processorId));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/processors/{id}")
    @Timed
    public Response getTopologyProcessorByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                                       @PathParam("id") Long processorId,
                                                       @PathParam("versionId") Long versionId,
                                                       @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        TopologyProcessor processor = catalogService.getTopologyProcessor(topologyId, processorId, versionId);
        if (processor != null) {
            return WSUtils.respondEntity(processor, OK);
        }

        throw EntityNotFoundException.byVersion(buildMessageForCompositeId(topologyId, processorId),
                versionId.toString());
    }
    /**
     * <p>
     * Creates a topology processor. For example,
     * </p>
     * <b>POST /api/v1/catalog/topologies/:TOPOLOGY_ID/processors</b>
     * <pre>
     * {
     *   "name": "ParserProcessor",
     *   "config": {
     *     "properties": {
     *       "parallelism": 1
     *     }
     *   },
     *   "type": "PARSER",
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
     *     "name": "ParserProcessor",
     *     "config": {
     *       "properties": {
     *         "parallelism": 1
     *       }
     *     },
     *     "type": "PARSER",
     *     "outputStreams": [{stream1 data..}, {stream2 data..}]
     *   }
     * }
     * </pre>
     */
    @POST
    @Path("/topologies/{topologyId}/processors")
    @Timed
    public Response addTopologyProcessor(@PathParam("topologyId") Long topologyId, TopologyProcessor topologyProcessor,
                                         @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologyProcessor createdProcessor = catalogService.addTopologyProcessor(topologyId, topologyProcessor);
        return WSUtils.respondEntity(createdProcessor, CREATED);
    }

    /**
     * <p>Updates a topology processor.</p>
     * <p>
     * <b>PUT /api/v1/catalog/topologies/:TOPOLOGY_ID/processors/:PROCESSOR_ID</b>
     * <pre>
     * {
     *   "name": "ParserProcessor",
     *   "config": {
     *     "properties": {
     *       "parallelism": 5
     *     }
     *   },
     *   "type": "PARSER",
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
     *     "name": "ParserProcessor",
     *     "config": {
     *       "properties": {
     *         "parallelism": 5
     *       }
     *     },
     *     "type": "PARSER",
     *     "outputStreamIds": [1]
     *   }
     * }
     * </pre>
     */
    @PUT
    @Path("/topologies/{topologyId}/processors/{id}")
    @Timed
    public Response addOrUpdateTopologyProcessor(@PathParam("topologyId") Long topologyId, @PathParam("id") Long processorId,
                                                 TopologyProcessor topologyProcessor, @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologyProcessor createdTopologyProcessor = catalogService.addOrUpdateTopologyProcessor(
                topologyId, processorId, topologyProcessor);
        return WSUtils.respondEntity(createdTopologyProcessor, CREATED);
    }

    /**
     * <p>
     * Removes a topology processor.
     * </p>
     * <b>DELETE /api/v1/catalog/topologies/:TOPOLOGY_ID/processors/:PROCESSOR_ID</b>
     * <pre>
     * {
     *   "responseCode": 1000,
     *   "responseMessage": "Success",
     *   "entity": {
     *     "id": 1,
     *     "topologyId": 1,
     *     "name": "ParserProcessor",
     *     "config": {
     *       "properties": {
     *         "parallelism": 5
     *       }
     *     },
     *     "type": "PARSER",
     *     "outputStreamIds": [1]
     *   }
     * }
     * </pre>
     */
    @DELETE
    @Path("/topologies/{topologyId}/processors/{id}")
    @Timed
    public Response removeTopologyProcessor(@PathParam("topologyId") Long topologyId, @PathParam("id") Long processorId,
                                            @javax.ws.rs.QueryParam("removeEdges") boolean removeEdges,
                                            @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologyProcessor topologyProcessor = catalogService.removeTopologyProcessor(topologyId, processorId, removeEdges);
        if (topologyProcessor != null) {
            return WSUtils.respondEntity(topologyProcessor, OK);
        }

        throw EntityNotFoundException.byId(processorId.toString());
    }

    private String buildMessageForCompositeId(Long topologyId, Long processorId) {
        return String.format("topology id <%d>, processor id <%d>", topologyId, processorId);
    }
}