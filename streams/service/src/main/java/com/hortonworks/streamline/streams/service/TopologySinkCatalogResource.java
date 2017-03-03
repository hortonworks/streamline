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
import com.hortonworks.streamline.streams.catalog.TopologySink;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
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
 * Sink component within an StreamlineTopology
 */
@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologySinkCatalogResource {
    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;

    public TopologySinkCatalogResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService) {
        this.authorizer = authorizer;
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
    @Path("/topologies/{topologyId}/sinks")
    @Timed
    public Response listTopologySinks(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo,
                                      @Context SecurityContext securityContext) throws Exception {
        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);
        return listTopologySinks(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, currentVersionId, uriInfo),
                topologyId,
                securityContext);
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/sinks")
    @Timed
    public Response listTopologySinksForVersion(@PathParam("topologyId") Long topologyId,
                                                @PathParam("versionId") Long versionId,
                                                @Context UriInfo uriInfo,
                                                @Context SecurityContext securityContext) throws Exception {
        return listTopologySinks(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, versionId, uriInfo),
                topologyId,
                securityContext);
    }

    private Response listTopologySinks(List<QueryParam> queryParams, Long topologyId, SecurityContext securityContext) throws Exception {
        SecurityUtil.checkPermissions(authorizer, securityContext, Topology.NAMESPACE, topologyId, READ);
        Collection<TopologySink> sinks = catalogService.listTopologySinks(queryParams);
        if (sinks != null) {
            return WSUtils.respondEntities(sinks, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }


    /**
     * <p>
     * Gets the 'CURRENT' version of specific topology sink by Id. For example,
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
    @Path("/topologies/{topologyId}/sinks/{id}")
    @Timed
    public Response getTopologySinkById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sinkId,
                                        @Context SecurityContext securityContext) {
        SecurityUtil.checkPermissions(authorizer, securityContext, Topology.NAMESPACE, topologyId, READ);
        TopologySink sink = catalogService.getTopologySink(topologyId, sinkId);
        if (sink != null) {
            return WSUtils.respondEntity(sink, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(topologyId, sinkId));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/sinks/{id}")
    @Timed
    public Response getTopologySinkByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                                  @PathParam("id") Long sourceId,
                                                  @PathParam("versionId") Long versionId,
                                                  @Context SecurityContext securityContext) {
        SecurityUtil.checkPermissions(authorizer, securityContext, Topology.NAMESPACE, topologyId, READ);
        TopologySink sink = catalogService.getTopologySink(topologyId, sourceId, versionId);
        if (sink != null) {
            return WSUtils.respondEntity(sink, OK);
        }

        throw EntityNotFoundException.byVersion(buildMessageForCompositeId(topologyId, sourceId),
                versionId.toString());
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
    @Path("/topologies/{topologyId}/sinks")
    @Timed
    public Response addTopologySink(@PathParam("topologyId") Long topologyId, TopologySink topologySink,
                                    @Context SecurityContext securityContext) {
        SecurityUtil.checkPermissions(authorizer, securityContext, Topology.NAMESPACE, topologyId, WRITE);
        TopologySink createdSink = catalogService.addTopologySink(topologyId, topologySink);
        return WSUtils.respondEntity(createdSink, CREATED);
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
    @Path("/topologies/{topologyId}/sinks/{id}")
    @Timed
    public Response addOrUpdateTopologySink(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sinkId,
                                            TopologySink topologySink, @Context SecurityContext securityContext) {
        SecurityUtil.checkPermissions(authorizer, securityContext, Topology.NAMESPACE, topologyId, WRITE);
        TopologySink createdTopologySink = catalogService.addOrUpdateTopologySink(topologyId, sinkId, topologySink);
        return WSUtils.respondEntity(createdTopologySink, CREATED);
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
    @Path("/topologies/{topologyId}/sinks/{id}")
    @Timed
    public Response removeTopologySink(@PathParam("topologyId") Long topologyId, @PathParam("id") Long sinkId,
                                       @javax.ws.rs.QueryParam("removeEdges") boolean removeEdges,
                                       @Context SecurityContext securityContext) {
        SecurityUtil.checkPermissions(authorizer, securityContext, Topology.NAMESPACE, topologyId, WRITE);
        TopologySink topologySink = catalogService.removeTopologySink(topologyId, sinkId, removeEdges);
        if (topologySink != null) {
            return WSUtils.respondEntity(topologySink, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(topologyId, sinkId));
    }

    private String buildMessageForCompositeId(Long topologyId, Long sinkId) {
        return String.format("topology id <%d>, sink id <%d>", topologyId, sinkId);
    }
}