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
import com.hortonworks.streamline.streams.catalog.TopologyWindow;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * REST resource for managing window rule with aggregate function.
 * <p>
 * A separate endpoint is provided for UI to have a separate endpoint
 * to configure window with aggregate functions.
 */
@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class WindowCatalogResource {
    private final StreamlineAuthorizer authorizer;
    private static final Logger LOG = LoggerFactory.getLogger(WindowCatalogResource.class);

    private final StreamCatalogService catalogService;

    public WindowCatalogResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
    }

    @GET
    @Path("/topologies/{topologyId}/windows")
    @Timed
    public Response listTopologyWindows(@PathParam("topologyId") Long topologyId, @Context UriInfo uriInfo,
                                        @Context SecurityContext securityContext) throws Exception {
        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);
        return listTopologyWindows(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, currentVersionId, uriInfo),
                topologyId,
                securityContext);
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/windows")
    @Timed
    public Response listTopologySourcesForVersion(@PathParam("topologyId") Long topologyId,
                                                  @PathParam("versionId") Long versionId,
                                                  @Context UriInfo uriInfo,
                                                  @Context SecurityContext securityContext) throws Exception {
        return listTopologyWindows(
                buildTopologyIdAndVersionIdAwareQueryParams(topologyId, versionId, uriInfo),
                topologyId,
                securityContext);
    }

    private Response listTopologyWindows(List<QueryParam> queryParams, Long topologyId, SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        Collection<TopologyWindow> topologyWindows = catalogService.listWindows(queryParams);
        if (topologyWindows != null) {
            return WSUtils.respondEntities(topologyWindows, OK);
        }

        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/windows/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopologyWindowById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long windowId,
                                          @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        TopologyWindow topologyWindow = catalogService.getWindow(topologyId, windowId);
        if (topologyWindow != null) {
            return WSUtils.respondEntity(topologyWindow, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(topologyId, windowId));
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/windows/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopologyWindowByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                                    @PathParam("id") Long windowId,
                                                    @PathParam("versionId") Long versionId,
                                                    @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);
        TopologyWindow topologyWindow = catalogService.getWindow(topologyId, windowId, versionId);
        if (topologyWindow != null) {
            return WSUtils.respondEntity(topologyWindow, OK);
        }

        throw EntityNotFoundException.byVersion(buildMessageForCompositeId(topologyId, windowId),
                versionId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/windows")
    @Timed
    public Response addTopologyWindow(@PathParam("topologyId") Long topologyId, TopologyWindow topologyWindow,
                                      @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologyWindow createdTopologyWindow = catalogService.addWindow(topologyId, topologyWindow);
        return WSUtils.respondEntity(createdTopologyWindow, CREATED);
    }

    @PUT
    @Path("/topologies/{topologyId}/windows/{id}")
    @Timed
    public Response addOrUpdateWindow(@PathParam("topologyId") Long topologyId, @PathParam("id") Long ruleId,
                                      TopologyWindow topologyWindow, @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologyWindow createdTopologyWindow = catalogService.addOrUpdateWindow(topologyId, ruleId, topologyWindow);
        return WSUtils.respondEntity(createdTopologyWindow, CREATED);
    }

    @DELETE
    @Path("/topologies/{topologyId}/windows/{id}")
    @Timed
    public Response removeWindowById(@PathParam("topologyId") Long topologyId, @PathParam("id") Long windowId,
                                     @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                Topology.NAMESPACE, topologyId, WRITE);
        TopologyWindow topologyWindow = catalogService.removeWindow(topologyId, windowId);
        if (topologyWindow != null) {
            return WSUtils.respondEntity(topologyWindow, OK);
        }

        throw EntityNotFoundException.byId(buildMessageForCompositeId(topologyId, windowId));
    }

    private String buildMessageForCompositeId(Long topologyId, Long windowId) {
        return String.format("topology id <%d>, window id <%d>", topologyId, windowId);
    }

}