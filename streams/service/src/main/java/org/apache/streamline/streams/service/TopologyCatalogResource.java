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
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.Topology;
import org.apache.streamline.streams.catalog.TopologyVersionInfo;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.layout.component.TopologyActions;
import org.apache.streamline.streams.metrics.storm.topology.TopologyNotAliveException;
import org.apache.streamline.streams.metrics.topology.TopologyMetrics;
import org.apache.streamline.streams.service.exception.request.BadRequestException;
import org.apache.streamline.streams.service.exception.request.EntityNotFoundException;
import org.apache.streamline.streams.storm.common.StormNotReachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.streamline.streams.catalog.TopologyVersionInfo.VERSION_PREFIX;
import static org.apache.streamline.streams.service.TopologySortType.LAST_UPDATED;


@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyCatalogResource.class);
    public static final String JAR_FILE_PARAM_NAME = "jarFile";
    public static final String CP_INFO_PARAM_NAME = "customProcessorInfo";
    private static final Integer DEFAULT_N_OF_TOP_N_LATENCY = 3;
    private static final String DEFAULT_SORT_TYPE = LAST_UPDATED.name();
    private static final Boolean DEFAULT_SORT_ORDER_ASCENDING = false;

    private final URL SCHEMA = Thread.currentThread().getContextClassLoader()
            .getResource("assets/schemas/topology.json");

    private final StreamCatalogService catalogService;

    public TopologyCatalogResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/topologies")
    @Timed
    public Response listTopologies (@javax.ws.rs.QueryParam("withMetric") Boolean withMetric,
                                    @javax.ws.rs.QueryParam("sort") String sortType,
                                    @javax.ws.rs.QueryParam("ascending") Boolean ascending,
                                    @javax.ws.rs.QueryParam("latencyTopN") Integer latencyTopN) {
        Collection<Topology> topologies = catalogService.listTopologies();
        Response response;
        if (topologies != null) {
            if (withMetric == null || !withMetric) {
                response = WSUtils.respondEntities(topologies, OK);
            } else {
                if (sortType == null) {
                    sortType = DEFAULT_SORT_TYPE;
                }
                if (ascending == null) {
                    ascending = DEFAULT_SORT_ORDER_ASCENDING;
                }

                List<TopologyCatalogWithMetric> topologiesWithMetric = enrichMetricToTopologies(
                        topologies, sortType, ascending, latencyTopN);
                response = WSUtils.respondEntities(topologiesWithMetric, OK);
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
                                    @javax.ws.rs.QueryParam("withMetric") Boolean withMetric,
                                    @javax.ws.rs.QueryParam("latencyTopN") Integer latencyTopN) {
        Response response = getTopologyByIdAndVersionId(topologyId,
                catalogService.getCurrentVersionId(topologyId),
                withMetric, latencyTopN);
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
                                              @javax.ws.rs.QueryParam("withMetric") Boolean withMetric,
                                              @javax.ws.rs.QueryParam("latencyTopN") Integer latencyTopN) {
        Response response = getTopologyByIdAndVersionId(topologyId, versionId, withMetric, latencyTopN);
        if (response != null) {
            return response;
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    private Response getTopologyByIdAndVersionId(Long topologyId, Long versionId, Boolean withMetric,
                                                 Integer latencyTopN) {
        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            if (withMetric == null || !withMetric) {
                return WSUtils.respondEntity(result, OK);
            } else {
                TopologyCatalogWithMetric topologiesWithMetric = enrichMetricToTopology(
                        result, latencyTopN);
                return WSUtils.respondEntity(topologiesWithMetric, OK);
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
                                   @javax.ws.rs.QueryParam("recurse") boolean recurse,
                                   @javax.ws.rs.QueryParam("allVersions") boolean allVersions) {
        if (allVersions) {
            return removeAllTopologyVersions(topologyId, recurse);
        } else {
            return removeCurrentTopologyVersion(topologyId, recurse);
        }
    }

    private Response removeAllTopologyVersions(Long topologyId, boolean recurse) {
        Collection<TopologyVersionInfo> versions = catalogService.listTopologyVersionInfos(
                WSUtils.topologyVersionsQueryParam(topologyId));
        List<Topology> removed = new ArrayList<>();
        for (TopologyVersionInfo version : versions) {
            removed.add(catalogService.removeTopology(topologyId, version.getId(), recurse));
        }
        return WSUtils.respondEntities(removed, OK);
    }

    private Response removeCurrentTopologyVersion(Long topologyId, boolean recurse) {
        Topology removedTopology = catalogService.removeTopology(topologyId, recurse);
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
            TopologyActions.Status status = catalogService.topologyStatus(result);
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
            TopologyActions.Status status = catalogService.topologyStatus(result);
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
//TODO: fix     catalogService.validateTopology(SCHEMA, topologyId);
            catalogService.deployTopology(result);
            return WSUtils.respondEntity(result, OK);
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
//TODO: fix     catalogService.validateTopology(SCHEMA, topologyId);
            catalogService.deployTopology(result);
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/kill")
    @Timed
    public Response killTopology (@PathParam("topologyId") Long topologyId) throws Exception {
        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            catalogService.killTopology(result);
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
            catalogService.killTopology(result);
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
            catalogService.suspendTopology(result);
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
            catalogService.suspendTopology(result);
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
            catalogService.resumeTopology(result);
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
            catalogService.resumeTopology(result);
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    private List<TopologyCatalogWithMetric> enrichMetricToTopologies(
            Collection<Topology> topologies, String sortType, Boolean ascending, Integer latencyTopN) {
        // need to also provide Topology Metric
        List<TopologyCatalogWithMetric> topologiesWithMetric = new ArrayList<>(topologies.size());
        for (Topology topology : topologies) {
            TopologyCatalogWithMetric topologyCatalogWithMetric = enrichMetricToTopology(topology, latencyTopN);
            topologiesWithMetric.add(topologyCatalogWithMetric);
        }

        return topologiesWithMetric.stream().sorted((c1, c2) -> {
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
        }).collect(toList());
    }

    private TopologyCatalogWithMetric enrichMetricToTopology(Topology topology, Integer latencyTopN) {
        if (latencyTopN == null) {
            latencyTopN = DEFAULT_N_OF_TOP_N_LATENCY;
        }

        TopologyCatalogWithMetric topologyCatalogWithMetric;
        try {
            TopologyMetrics.TopologyMetric topologyMetric = catalogService.getTopologyMetric(topology);
            List<Pair<String, Double>> latenciesTopN = catalogService.getTopNAndOtherComponentsLatency(topology, latencyTopN);
            topologyCatalogWithMetric = new TopologyCatalogWithMetric(topology, TopologyRunningStatus.RUNNING, topologyMetric, latenciesTopN);
        } catch (TopologyNotAliveException e) {
            topologyCatalogWithMetric = new TopologyCatalogWithMetric(topology, TopologyRunningStatus.NOT_RUNNING, null, null);
        } catch (StormNotReachableException | IOException e) {
            topologyCatalogWithMetric = new TopologyCatalogWithMetric(topology, TopologyRunningStatus.UNKNOWN, null, null);
        } catch (Exception e ) {
            topologyCatalogWithMetric = new TopologyCatalogWithMetric(topology, TopologyRunningStatus.UNKNOWN, null, null);
        }
        return topologyCatalogWithMetric;
    }

    private enum TopologyRunningStatus {
        RUNNING, NOT_RUNNING, UNKNOWN
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class TopologyCatalogWithMetric {
        private final Topology topology;
        private final TopologyRunningStatus running;
        private final TopologyMetrics.TopologyMetric metric;
        private final List<Pair<String, Double>> latencyTopN;

        public TopologyCatalogWithMetric(Topology topology, TopologyRunningStatus running,
                                         TopologyMetrics.TopologyMetric metric, List<Pair<String, Double>> latencyTopN) {
            this.topology = topology;
            this.running = running;
            this.metric = metric;
            this.latencyTopN = latencyTopN;
        }

        public Topology getTopology() {
            return topology;
        }

        public String getRunning() {
            return running.name();
        }

        public TopologyMetrics.TopologyMetric getMetric() {
            return metric;
        }

        public List<Pair<String, Double>> getLatencyTopN() {
            return latencyTopN;
        }
    }
}

