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
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.exception.service.exception.request.WebserviceAuthorizationException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.TopologyEditorToolbar;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.security.Permission;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.catalog.User;
import com.hortonworks.streamline.streams.security.service.SecurityCatalogService;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import java.security.Principal;
import java.util.EnumSet;

import static com.hortonworks.streamline.streams.security.Permission.READ;
import static com.hortonworks.streamline.streams.security.Permission.WRITE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyEditorToolbarResource {
    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private final SecurityCatalogService securityCatalogService;

    public TopologyEditorToolbarResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService,
                                         SecurityCatalogService securityCatalogService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
        this.securityCatalogService = securityCatalogService;
    }

    @GET
    @Path("/system/topologyeditortoolbar")
    @Timed
    public Response listTopologyEditorToolbar(@Context SecurityContext securityContext) {
        long userId = getUserId(securityContext);
        SecurityUtil.checkPermissions(authorizer, securityContext, TopologyEditorToolbar.NAMESPACE, userId, READ);
        return catalogService.getTopologyEditorToolbar(userId)
                .map(toolbar -> WSUtils.respondEntity(toolbar, OK))
                .orElseThrow(() -> EntityNotFoundException.byId(String.valueOf(userId)));
    }

    /**
     * UI can send the toolbar data as a string. The backend will just map that information
     * with the currently logged in user (or userId -1 if running in non-secure mode).
     *
     * E.g.
     * <pre>
     * {
     *   "data": "{
     *             \"sources\": [
     *                 {\"bundleId\": 1},
     *                 {\"type\":\"folder\", \"name\": \"Source Folder\",\"children\": [{\"bundleId\": 2},{\"bundleId\": 3}]}
     *                 ],
     *             \"processors\": [...],
     *             \"sinks\":[...]
     *            }"
     * }
     * </pre>
     */
    @POST
    @Path("/system/topologyeditortoolbar")
    @Timed
    public Response addTopologyEditorToolbar(TopologyEditorToolbar toolbar, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        long userId = getUserId(securityContext);
        toolbar.setUserId(userId);
        TopologyEditorToolbar added = catalogService.addTopologyEditorToolbar(toolbar);
        SecurityUtil.addAcl(authorizer, securityContext, TopologyEditorToolbar.NAMESPACE, userId, EnumSet.allOf(Permission.class));
        return WSUtils.respondEntity(added, CREATED);
    }

    @DELETE
    @Path("/system/topologyeditortoolbar")
    @Timed
    public Response removeTopologyEditorToolbar(@Context SecurityContext securityContext) {
        long userId = getUserId(securityContext);
        SecurityUtil.checkPermissions(authorizer, securityContext, TopologyEditorToolbar.NAMESPACE, userId, Permission.DELETE);
        return catalogService.removeTopologyEditorToolbar(userId)
                .map(toolbar -> {
                    SecurityUtil.removeAcl(authorizer, securityContext, TopologyEditorToolbar.NAMESPACE, userId);
                    return WSUtils.respondEntity(toolbar, OK);
                })
                .orElseThrow(() -> EntityNotFoundException.byId(String.valueOf(userId)));
    }

    @PUT
    @Path("/system/topologyeditortoolbar")
    @Timed
    public Response addOrUpdateTopologyEditorToolbar(TopologyEditorToolbar toolbar, @Context SecurityContext securityContext) {
        Long userId = getUserId(securityContext);
        if (!userId.equals(toolbar.getUserId())) {
            throw new IllegalArgumentException("User id in the security context: '" + userId + "' does not match user id " +
                    "in the request: '" + toolbar.getUserId() + "'");
        }
        SecurityUtil.checkPermissions(authorizer, securityContext, TopologyEditorToolbar.NAMESPACE, userId, WRITE);
        TopologyEditorToolbar updated = catalogService.addOrUpdateTopologyEditorToolbar(toolbar);
        return WSUtils.respondEntity(updated, OK);
    }

    private long getUserId(SecurityContext securityContext) {
        Principal principal = securityContext.getUserPrincipal();
        String userName = principal != null ? principal.getName() : User.USER_ANONYMOUS;
        User user = securityCatalogService.getUser(userName);
        if (user != null && user.getId() != null) {
            return user.getId();
        }
        throw new IllegalArgumentException("No such user: " + userName);
    }
}
