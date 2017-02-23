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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.hortonworks.streamline.common.exception.DuplicateEntityException;
import com.hortonworks.streamline.common.exception.service.exception.request.TopologyAlreadyExistsOnCluster;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import com.hortonworks.streamline.common.util.ParallelStreamUtil;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyVersionInfo;
import com.hortonworks.streamline.streams.catalog.service.EnvironmentService;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.catalog.topology.TopologyData;
import com.hortonworks.streamline.streams.storm.common.TopologyNotAliveException;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyMetricsService;
import com.hortonworks.streamline.streams.storm.common.StormNotReachableException;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static com.hortonworks.streamline.streams.catalog.TopologyVersionInfo.VERSION_PREFIX;
import static com.hortonworks.streamline.streams.service.TopologySortType.LAST_UPDATED;


@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyCatalogResource.class);
    private static final Integer DEFAULT_N_OF_TOP_N_LATENCY = 3;
    private static final String DEFAULT_SORT_TYPE = LAST_UPDATED.name();
    private static final Boolean DEFAULT_SORT_ORDER_ASCENDING = false;

    private static final int FORK_JOIN_POOL_PARALLELISM = 10;

    private final ForkJoinPool forkJoinPool = new ForkJoinPool(FORK_JOIN_POOL_PARALLELISM);

    private final URL SCHEMA = Thread.currentThread().getContextClassLoader()
            .getResource("assets/schemas/topology.json");

    private final StreamCatalogService catalogService;
    private final EnvironmentService environmentService;
    private final TopologyActionsService actionsService;
    private final TopologyMetricsService metricsService;

    public TopologyCatalogResource(StreamCatalogService catalogService, EnvironmentService environmentService,
                                   TopologyActionsService actionsService, TopologyMetricsService metricsService) {
        this.catalogService = catalogService;
        this.environmentService = environmentService;
        this.actionsService = actionsService;
        this.metricsService = metricsService;
    }

    @GET
    @Path("/topologies")
    @Timed
    public Response listTopologies (@javax.ws.rs.QueryParam("detail") Boolean detail,
                                    @javax.ws.rs.QueryParam("sort") String sortType,
                                    @javax.ws.rs.QueryParam("ascending") Boolean ascending,
                                    @javax.ws.rs.QueryParam("latencyTopN") Integer latencyTopN) {
        Collection<Topology> topologies = catalogService.listTopologies();
        Response response;
        if (topologies != null) {
            if (detail == null || !detail) {
                response = WSUtils.respondEntities(topologies, OK);
            } else {
                if (sortType == null) {
                    sortType = DEFAULT_SORT_TYPE;
                }
                if (ascending == null) {
                    ascending = DEFAULT_SORT_ORDER_ASCENDING;
                }

                List<TopologyDetailedResponse> detailedTopologies = enrichTopologies(topologies, sortType, ascending,
                        latencyTopN);
                response = WSUtils.respondEntities(detailedTopologies, OK);
            }
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }

        return response;
    }

    @GET
    @Path("/topologies/{topologyId}")
    @Timed
    public Response getTopologyById(@PathParam("topologyId") Long topologyId,
                                    @javax.ws.rs.QueryParam("detail") Boolean detail,
                                    @javax.ws.rs.QueryParam("latencyTopN") Integer latencyTopN) {
        Response response = getTopologyByIdAndVersionId(topologyId,
                catalogService.getCurrentVersionId(topologyId),
                detail, latencyTopN);
        if (response != null) {
            return response;
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}")
    @Timed
    public Response getTopologyByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                              @PathParam("versionId") Long versionId,
                                              @javax.ws.rs.QueryParam("detail") Boolean detail,
                                              @javax.ws.rs.QueryParam("latencyTopN") Integer latencyTopN) {
        Response response = getTopologyByIdAndVersionId(topologyId, versionId, detail, latencyTopN);
        if (response != null) {
            return response;
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    private Response getTopologyByIdAndVersionId(Long topologyId, Long versionId, Boolean detail,
                                                 Integer latencyTopN) {
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            if (detail == null || !detail) {
                return WSUtils.respondEntity(result, OK);
            } else {
                TopologyDetailedResponse topologyDetailed = enrichTopology(result, latencyTopN);
                return WSUtils.respondEntity(topologyDetailed, OK);
            }
        }
        return null;
    }

    @GET
    @Path("/topologies/{topologyId}/versions")
    @Timed
    public Response listTopologyVersions(@PathParam("topologyId") Long topologyId) {
        Collection<TopologyVersionInfo> versionInfos = catalogService.listTopologyVersionInfos(
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
    public Response addTopology(Topology topology) {
        if (StringUtils.isEmpty(topology.getName())) {
            throw BadRequestException.missingParameter(Topology.NAME);
        }
        if (StringUtils.isEmpty(topology.getConfig())) {
            throw BadRequestException.missingParameter(Topology.CONFIG);
        }
        Topology createdTopology = catalogService.addTopology(topology);
        return WSUtils.respondEntity(createdTopology, CREATED);
    }

    @DELETE
    @Path("/topologies/{topologyId}")
    @Timed
    public Response removeTopology(@PathParam("topologyId") Long topologyId,
                                   @javax.ws.rs.QueryParam("onlyCurrent") boolean onlyCurrent) {
        if (onlyCurrent) {
            return removeCurrentTopologyVersion(topologyId);
        } else {
            return removeAllTopologyVersions(topologyId);
        }
    }

    private Response removeAllTopologyVersions(Long topologyId) {
        Collection<TopologyVersionInfo> versions = catalogService.listTopologyVersionInfos(
                WSUtils.topologyVersionsQueryParam(topologyId));
        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);
        Topology res = null;
        for (TopologyVersionInfo version : versions) {
            Topology removed = catalogService.removeTopology(topologyId, version.getId(), true);
            if (removed != null && removed.getVersionId().equals(currentVersionId)) {
                res = removed;
            }
        }
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
                                         Topology topology) {
        if (StringUtils.isEmpty(topology.getName())) {
            throw BadRequestException.missingParameter(Topology.NAME);
        }
        if (StringUtils.isEmpty(topology.getConfig())) {
            throw BadRequestException.missingParameter(Topology.CONFIG);
        }
        Topology result = catalogService.addOrUpdateTopology
                (topologyId, topology);
        return WSUtils.respondEntity(topology, OK);
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
    public Response saveTopologyVersion(@PathParam("topologyId") Long topologyId, TopologyVersionInfo versionInfo) {
        Optional<TopologyVersionInfo> currentVersion = Optional.empty();
        try {
            currentVersion = catalogService.getCurrentTopologyVersionInfo(topologyId);
            if (!currentVersion.isPresent()) {
                throw new IllegalArgumentException("Current version is not available for topology id: " + topologyId);
            }
            if (versionInfo == null) {
                versionInfo = new TopologyVersionInfo();
            }
            // update the current version with the new version info.
            versionInfo.setTopologyId(topologyId);
            Optional<TopologyVersionInfo> latest = catalogService.getLatestVersionInfo(topologyId);
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
            TopologyVersionInfo savedVersion = catalogService.addOrUpdateTopologyVersionInfo(
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
                                            @PathParam("versionId") Long versionId) {
        Optional<TopologyVersionInfo> currentVersionInfo = catalogService.getCurrentTopologyVersionInfo(topologyId);
        if (currentVersionInfo.isPresent() && currentVersionInfo.get().getId().equals(versionId)) {
            throw new IllegalArgumentException("Version id " + versionId + " is already the current version");
        }
        TopologyVersionInfo savedVersion = catalogService.getTopologyVersionInfo(versionId);
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
    @Path("/topologies/{topologyId}/actions/status")
    @Timed
    public Response topologyStatus (@PathParam("topologyId") Long topologyId) throws Exception {
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            TopologyActions.Status status = actionsService.topologyStatus(result);
            return WSUtils.respondEntity(status, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/status")
    @Timed
    public Response topologyStatusVersion(@PathParam("topologyId") Long topologyId,
                                          @PathParam("versionId") Long versionId) throws Exception {
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            TopologyActions.Status status = actionsService.topologyStatus(result);
            return WSUtils.respondEntity(status, OK);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/validate")
    @Timed
    public Response validateTopology (@PathParam("topologyId") Long topologyId) {
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            //catalogService.validateTopology(SCHEMA, topologyId);
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/validate")
    @Timed
    public Response validateTopologyVersion(@PathParam("topologyId") Long topologyId,
                                            @PathParam("versionId") Long versionId) {
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            //catalogService.validateTopology(SCHEMA, topologyId);
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/deploy")
    @Timed
    public Response deployTopology (@PathParam("topologyId") Long topologyId) throws Exception {
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            try {
                actionsService.deployTopology(result);
                return WSUtils.respondEntity(result, OK);
            } catch (TopologyAlreadyExistsOnCluster ex) {
                return ex.getResponse();
            }
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/deploy")
    @Timed
    public Response deployTopologyVersion(@PathParam("topologyId") Long topologyId,
                                          @PathParam("versionId") Long versionId) throws Exception {
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            try {
                actionsService.deployTopology(result);
                return WSUtils.respondEntity(result, OK);
            } catch (TopologyAlreadyExistsOnCluster ex) {
                return ex.getResponse();
            }
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/kill")
    @Timed
    public Response killTopology (@PathParam("topologyId") Long topologyId) throws Exception {
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            actionsService.killTopology(result);
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/kill")
    @Timed
    public Response killTopologyVersion(@PathParam("topologyId") Long topologyId,
                                        @PathParam("versionId") Long versionId) throws Exception {
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            actionsService.killTopology(result);
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/suspend")
    @Timed
    public Response suspendTopology (@PathParam("topologyId") Long topologyId) throws Exception {
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            actionsService.suspendTopology(result);
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/suspend")
    @Timed
    public Response suspendTopologyVersion(@PathParam("topologyId") Long topologyId,
                                           @PathParam("versionId") Long versionId) throws Exception {
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            actionsService.suspendTopology(result);
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/resume")
    @Timed
    public Response resumeTopology (@PathParam("topologyId") Long topologyId) throws Exception {
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            actionsService.resumeTopology(result);
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/versions/{versionId}/actions/resume")
    @Timed
    public Response resumeTopologyVersion(@PathParam("topologyId") Long topologyId,
                                          @PathParam("versionId") Long versionId) throws Exception {
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            actionsService.resumeTopology(result);
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/actions/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public Response exportTopology(@PathParam("topologyId") Long topologyId) throws Exception {
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
    public Response cloneTopology(@PathParam("topologyId") Long topologyId, @QueryParam("namespaceId") Long namespaceId) throws Exception {
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
                                   @FormDataParam("topologyName") final String topologyName) throws Exception {
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

    private List<TopologyDetailedResponse> enrichTopologies(Collection<Topology> topologies, String sortType, Boolean ascending, Integer latencyTopN) {
        LOG.debug("[START] enrichTopologies");
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            List<TopologyDetailedResponse> responses = ParallelStreamUtil.execute(() ->
                    topologies.parallelStream()
                            .map(t -> enrichTopology(t, latencyTopN))
                            .sorted((c1, c2) -> {
                                int compared;

                                switch (TopologySortType.valueOf(sortType.toUpperCase())) {
                                    case NAME:
                                        compared = c1.getTopology().getName().compareTo(c2.getTopology().getName());
                                        break;
                                    case STATUS:
                                        compared = c1.getRunning().compareTo(c2.getRunning());
                                        break;
                                    case LAST_UPDATED:
                                        compared = c1.getTopology().getVersionTimestamp().compareTo(c2.getTopology().getVersionTimestamp());
                                        break;
                                    default:
                                        throw new IllegalStateException("Not supported SortType: " + sortType);
                                }

                                return ascending ? compared : (compared * -1);
                            })
                            .collect(toList()), forkJoinPool);

            LOG.debug("[END] enrichTopologies - elapsed: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

            return responses;
        } finally {
            stopwatch.stop();
        }
    }

    private TopologyDetailedResponse enrichTopology(Topology topology, Integer latencyTopN) {
        LOG.debug("[START] enrichTopology - topology id: {}", topology.getId());
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            if (latencyTopN == null) {
                latencyTopN = DEFAULT_N_OF_TOP_N_LATENCY;
            }

            TopologyDetailedResponse detailedResponse;

            String namespaceName = null;
            Namespace namespace = environmentService.getNamespace(topology.getNamespaceId());
            if (namespace != null) {
                namespaceName = namespace.getName();
            }

            try {
                String runtimeTopologyId = actionsService.getRuntimeTopologyId(topology);
                TopologyMetrics.TopologyMetric topologyMetric = metricsService.getTopologyMetric(topology);
                List<Pair<String, Double>> latenciesTopN = metricsService.getTopNAndOtherComponentsLatency(topology, latencyTopN);

                detailedResponse = new TopologyDetailedResponse(topology, TopologyRunningStatus.RUNNING, namespaceName);
                detailedResponse.setRuntime(new TopologyRuntimeResponse(runtimeTopologyId, topologyMetric, latenciesTopN));
            } catch (TopologyNotAliveException e) {
                LOG.debug("Topology {} is not alive", topology.getId());
                detailedResponse = new TopologyDetailedResponse(topology, TopologyRunningStatus.NOT_RUNNING, namespaceName);
            } catch (StormNotReachableException | IOException e) {
                LOG.error("Storm is not reachable or fail to operate", e);
                detailedResponse = new TopologyDetailedResponse(topology, TopologyRunningStatus.UNKNOWN, namespaceName);
            } catch (Exception e) {
                LOG.error("Unhandled exception occurs while operate with Storm", e);
                detailedResponse = new TopologyDetailedResponse(topology, TopologyRunningStatus.UNKNOWN, namespaceName);
            }

            LOG.debug("[END] enrichTopology - topology id: {}, elapsed: {} ms", topology.getId(),
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));

            return detailedResponse;
        } finally {
            stopwatch.stop();
        }
    }

    private enum TopologyRunningStatus {
        RUNNING, NOT_RUNNING, UNKNOWN
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private class TopologyDetailedResponse {
        private final Topology topology;
        private final TopologyRunningStatus running;
        private final String namespaceName;
        private TopologyRuntimeResponse runtime;

        public TopologyDetailedResponse(Topology topology, TopologyRunningStatus running, String namespaceName) {
            this.topology = topology;
            this.running = running;
            this.namespaceName = namespaceName;
        }

        public void setRuntime(TopologyRuntimeResponse runtime) {
            this.runtime = runtime;
        }

        public Topology getTopology() {
            return topology;
        }

        public TopologyRunningStatus getRunning() {
            return running;
        }

        public String getNamespaceName() {
            return namespaceName;
        }

        public TopologyRuntimeResponse getRuntime() {
            return runtime;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private class TopologyRuntimeResponse {
        private final String runtimeTopologyId;
        private final TopologyMetrics.TopologyMetric metric;
        private final List<Pair<String, Double>> latencyTopN;

        public TopologyRuntimeResponse(String runtimeTopologyId, TopologyMetrics.TopologyMetric metric, List<Pair<String, Double>> latencyTopN) {
            this.runtimeTopologyId = runtimeTopologyId;
            this.metric = metric;
            this.latencyTopN = latencyTopN;
        }

        public String getRuntimeTopologyId() {
            return runtimeTopologyId;
        }

        public TopologyMetrics.TopologyMetric getMetric() {
            return metric;
        }

        public List<Pair<String, Double>> getLatencyTopN() {
            return latencyTopN;
        }
    }
}

