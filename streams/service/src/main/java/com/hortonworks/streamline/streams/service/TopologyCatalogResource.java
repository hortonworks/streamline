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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.exception.service.exception.server.StreamingEngineNotReachableException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyVersion;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.catalog.topology.TopologyData;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import com.hortonworks.streamline.streams.logsearch.LogSearchCriteria;
import com.hortonworks.streamline.streams.logsearch.topology.service.TopologyLogSearchService;
import com.hortonworks.streamline.streams.security.Permission;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import com.hortonworks.streamline.streams.storm.common.StormNotReachableException;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static com.hortonworks.streamline.streams.catalog.Topology.NAMESPACE;
import static com.hortonworks.streamline.streams.catalog.TopologyVersion.VERSION_PREFIX;
import static com.hortonworks.streamline.streams.security.Permission.DELETE;
import static com.hortonworks.streamline.streams.security.Permission.EXECUTE;
import static com.hortonworks.streamline.streams.security.Permission.READ;
import static com.hortonworks.streamline.streams.security.Permission.WRITE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;


@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyCatalogResource.class);

    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private final TopologyActionsService actionsService;

    public TopologyCatalogResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService,
                                   TopologyActionsService actionsService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
        this.actionsService = actionsService;
    }

    @GET
    @Path("/topologies")
    @Timed
    public Response listTopologies (@Context SecurityContext securityContext) {
        Collection<Topology> topologies = catalogService.listTopologies();
        boolean topologyUser = SecurityUtil.hasRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER);
        if (topologyUser) {
            LOG.debug("Returning all topologies since user has role: {}", Roles.ROLE_TOPOLOGY_USER);
        } else {
            topologies = SecurityUtil.filter(authorizer, securityContext, NAMESPACE, topologies, READ);
        }
        Response response;
        if (topologies != null) {
            response = WSUtils.respondEntities(topologies, OK);
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }

        return response;
    }

    @GET
    @Path("/topologies/{topologyId}")
    @Timed
    public Response getTopologyById(@PathParam("topologyId") Long topologyId,
                                    @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);

        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}")
    @Timed
    public Response getTopologyByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                              @PathParam("versionId") Long versionId,
                                              @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);

        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/versions")
    @Timed
    public Response listTopologyVersions(@PathParam("topologyId") Long topologyId, @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);
        Collection<TopologyVersion> versionInfos = catalogService.listTopologyVersionInfos(
                WSUtils.buildTopologyIdAwareQueryParams(topologyId, null));
        Response response;
        if (versionInfos != null) {
            response = WSUtils.respondEntities(versionInfos, OK);
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }
        return response;
    }

    @POST
    @Path("/topologies")
    @Timed
    public Response addTopology(Topology topology, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        if (StringUtils.isEmpty(topology.getName())) {
            throw BadRequestException.missingParameter(Topology.NAME);
        }
        if (StringUtils.isEmpty(topology.getConfig())) {
            throw BadRequestException.missingParameter(Topology.CONFIG);
        }
        Topology createdTopology = catalogService.addTopology(topology);
        SecurityUtil.addAcl(authorizer, securityContext, NAMESPACE, createdTopology.getId(),
                EnumSet.allOf(Permission.class));
        return WSUtils.respondEntity(createdTopology, CREATED);
    }

    @DELETE
    @Path("/topologies/{topologyId}")
    @Timed
    public Response removeTopology(@PathParam("topologyId") Long topologyId,
                                   @javax.ws.rs.QueryParam("onlyCurrent") boolean onlyCurrent,
                                   @javax.ws.rs.QueryParam("force") boolean force,
                                   @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                NAMESPACE, topologyId, DELETE);

        if (!force) {
            Topology result = catalogService.getTopology(topologyId);
            if (result == null) {
                throw EntityNotFoundException.byId(topologyId.toString());
            }
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            try {
                String runtimeTopologyId = actionsService.getRuntimeTopologyId(result, asUser);
                if (StringUtils.isNotEmpty(runtimeTopologyId)) {
                    throw BadRequestException.message("Can't remove topology while Topology is running - topology id: " + topologyId);
                }
            } catch (TopologyNotAliveException e) {
                // OK to continue
            } catch (StormNotReachableException | IOException e) {
                // We don't know whether topology is running or not
                // users need to make a request with force parameter on
                throw new StreamingEngineNotReachableException(e.getMessage(), e);
            }
        }

        Response response;
        if (onlyCurrent) {
            response = removeCurrentTopologyVersion(topologyId);
        } else {
            response = removeAllTopologyVersions(topologyId);
        }
        SecurityUtil.removeAcl(authorizer, securityContext, NAMESPACE, topologyId);
        return response;
    }

    private Response removeAllTopologyVersions(Long topologyId) {
        Collection<TopologyVersion> versions = catalogService.listTopologyVersionInfos(
                WSUtils.topologyVersionsQueryParam(topologyId));
        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);
        Topology res = null;
        for (TopologyVersion version : versions) {
            Topology removed = catalogService.removeTopology(topologyId, version.getId(), true);
            if (removed != null && removed.getVersionId().equals(currentVersionId)) {
                res = removed;
            }
        }
        // remove topology state information
        catalogService.removeTopologyState(topologyId);
        if (res != null) {
            return WSUtils.respondEntity(res, OK);
        } else {
            throw EntityNotFoundException.byId(topologyId.toString());
        }
    }

    private Response removeCurrentTopologyVersion(Long topologyId) {
        Topology removedTopology = catalogService.removeTopology(topologyId, true);
        if (removedTopology != null) {
            return WSUtils.respondEntity(removedTopology, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @PUT
    @Path("/topologies/{topologyId}")
    @Timed
    public Response addOrUpdateTopology (@PathParam("topologyId") Long topologyId,
                                         Topology topology, @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                NAMESPACE, topologyId, WRITE);
        if (StringUtils.isEmpty(topology.getName())) {
            throw BadRequestException.missingParameter(Topology.NAME);
        }
        if (StringUtils.isEmpty(topology.getConfig())) {
            throw BadRequestException.missingParameter(Topology.CONFIG);
        }
        Topology result = catalogService.addOrUpdateTopology(topologyId, topology);
        return WSUtils.respondEntity(result, OK);
    }

    /**
     * {
     *     "name": "v2",
     *     "description": "saved before prod deployment"
     * }
     */
    @POST
    @Path("/topologies/{topologyId}/versions/save")
    @Timed
    public Response saveTopologyVersion(@PathParam("topologyId") Long topologyId, TopologyVersion versionInfo,
                                        @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);
        Optional<TopologyVersion> currentVersion = Optional.empty();
        try {
            currentVersion = catalogService.getCurrentTopologyVersionInfo(topologyId);
            if (!currentVersion.isPresent()) {
                throw new IllegalArgumentException("Current version is not available for topology id: " + topologyId);
            }
            if (versionInfo == null) {
                versionInfo = new TopologyVersion();
            }
            // update the current version with the new version info.
            versionInfo.setTopologyId(topologyId);
            Optional<TopologyVersion> latest = catalogService.getLatestVersionInfo(topologyId);
            int suffix;
            if (latest.isPresent()) {
                suffix = latest.get().getVersionNumber() + 1;
            } else {
                suffix = 1;
            }
            versionInfo.setName(VERSION_PREFIX + suffix);
            if (versionInfo.getDescription() == null) {
                versionInfo.setDescription("");
            }
            TopologyVersion savedVersion = catalogService.addOrUpdateTopologyVersionInfo(
                    currentVersion.get().getId(), versionInfo);
            catalogService.cloneTopologyVersion(topologyId, savedVersion.getId());
            return WSUtils.respondEntity(savedVersion, CREATED);
        } catch (Exception ex) {
            // restore the current version
            if (currentVersion.isPresent()) {
                catalogService.addOrUpdateTopologyVersionInfo(currentVersion.get().getId(), currentVersion.get());
            }
            throw ex;
        }
    }

    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/activate")
    @Timed
    public Response activateTopologyVersion(@PathParam("topologyId") Long topologyId,
                                            @PathParam("versionId") Long versionId,
                                            @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);
        Optional<TopologyVersion> currentVersionInfo = catalogService.getCurrentTopologyVersionInfo(topologyId);
        if (currentVersionInfo.isPresent() && currentVersionInfo.get().getId().equals(versionId)) {
            throw new IllegalArgumentException("Version id " + versionId + " is already the current version");
        }
        TopologyVersion savedVersion = catalogService.getTopologyVersionInfo(versionId);
        if (savedVersion != null) {
            catalogService.cloneTopologyVersion(topologyId, savedVersion.getId());
             /*
             * successfully cloned and set a new current version,
             * remove the old current version of topology and version info
             */
            if (currentVersionInfo.isPresent()) {
                catalogService.removeTopology(topologyId, currentVersionInfo.get().getId(), true);
            }
            return WSUtils.respondEntity(savedVersion, CREATED);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/deploymentstate")
    @Timed
    public Response topologyDeploymentState(@PathParam("topologyId") Long topologyId) throws Exception {
        return Optional.ofNullable(catalogService.getTopology(topologyId))
                .flatMap(t -> catalogService.getTopologyState(t.getId()))
                .map(s -> WSUtils.respondEntity(s, OK))
                .orElseThrow(() -> EntityNotFoundException.byId(topologyId.toString()));
    }

    @GET
    @Path("/topologies/{topologyId}/actions/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public Response exportTopology(@PathParam("topologyId") Long topologyId, @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            String exportedTopology = catalogService.exportTopology(topology);
            if (!StringUtils.isEmpty(exportedTopology)) {
                InputStream is = new ByteArrayInputStream(exportedTopology.getBytes(StandardCharsets.UTF_8));
                return Response.status(OK)
                        .entity(is)
                        .header("Content-Disposition", "attachment; filename=\"" + topology.getName() + ".json\"")
                        .build();
            }
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/clone")
    @Timed
    public Response cloneTopology(@PathParam("topologyId") Long topologyId,
                                  @QueryParam("namespaceId") Long namespaceId,
                                  @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_SUPER_ADMIN,
                NAMESPACE, topologyId, READ, EXECUTE);
        Topology originalTopology = catalogService.getTopology(topologyId);
        if (originalTopology != null) {
            Topology clonedTopology = catalogService.cloneTopology(namespaceId, originalTopology);
            return WSUtils.respondEntity(clonedTopology, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    /**
     * curl -X POST 'http://localhost:8080/api/v1/catalog/topologies/actions/import' -F file=@/tmp/topology.json -F namespaceId=1
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/topologies/actions/import")
    @Timed
    public Response importTopology(@FormDataParam("file") final InputStream inputStream,
                                   @FormDataParam("namespaceId") final Long namespaceId,
                                   @FormDataParam("topologyName") final String topologyName,
                                   @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_ADMIN);
        if (namespaceId == null) {
            throw new IllegalArgumentException("Missing namespaceId");
        }
        TopologyData topologyData = new ObjectMapper().readValue(inputStream, TopologyData.class);
        if (topologyName != null && !topologyName.isEmpty()) {
            topologyData.setTopologyName(topologyName);
        }
        Topology importedTopology = catalogService.importTopology(namespaceId, topologyData);
        return WSUtils.respondEntity(importedTopology, OK);
    }

    @GET
    @Path("/topologies/{topologyId}/reconfigure")
    @Timed
    public Response getComponentsToReconfigure(@PathParam("topologyId") Long topologyId,
                                  @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);
        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            return WSUtils.respondEntity(catalogService.getComponentsToReconfigure(topology), OK);
        }
        throw EntityNotFoundException.byId(topologyId.toString());
    }

}

