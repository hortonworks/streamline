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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.actions.TopologyActions;
import org.apache.streamline.streams.actions.topology.service.TopologyActionsService;
import org.apache.streamline.streams.catalog.CatalogToLayoutConverter;
import org.apache.streamline.streams.catalog.Topology;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.metrics.service.TopologyMetricsService;
import org.apache.streamline.streams.metrics.storm.topology.StormNotReachableException;
import org.apache.streamline.streams.metrics.storm.topology.TopologyNotAliveException;
import org.apache.streamline.streams.metrics.topology.TopologyMetrics;
import org.apache.commons.lang3.StringUtils;
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

import static java.util.stream.Collectors.toList;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;



@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyCatalogResource.class);
    public static final String JAR_FILE_PARAM_NAME = "jarFile";
    public static final String CP_INFO_PARAM_NAME = "customProcessorInfo";
    private static final Integer DEFAULT_N_OF_TOP_N_LATENCY = 3;
    private final StreamCatalogService catalogService;
    private final TopologyActionsService actionsService;
    private final TopologyMetricsService metricsService;
    private final URL SCHEMA = Thread.currentThread().getContextClassLoader()
            .getResource("assets/schemas/topology.json");

    public TopologyCatalogResource(StreamCatalogService catalogService, TopologyActionsService actionsService,
                                   TopologyMetricsService metricsService) {
        this.catalogService = catalogService;
        this.actionsService = actionsService;
        this.metricsService = metricsService;
    }

    @GET
    @Path("/topologies")
    @Timed
    public Response listTopologies (@javax.ws.rs.QueryParam("withMetric") Boolean withMetric,
        @javax.ws.rs.QueryParam("sort") String sortType,
        @javax.ws.rs.QueryParam("latencyTopN") Integer latencyTopN) {
        try {
            Collection<Topology> topologies = catalogService.listTopologies();
            Response response;
            if (topologies != null) {
                if (withMetric == null || !withMetric) {
                    response = WSUtils.respond(topologies, OK, SUCCESS);
                } else {
                    List<TopologyCatalogWithMetric> topologiesWithMetric = enrichMetricToTopologies(
                        topologies, sortType, latencyTopN);
                    response = WSUtils.respond(topologiesWithMetric, OK, SUCCESS);
                }
            } else {
                response = WSUtils.respond(Collections.emptyList(), OK, SUCCESS);
            }

            return response;
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/topologies/{topologyId}")
    @Timed
    public Response getTopologyById (@PathParam("topologyId") Long topologyId,
        @javax.ws.rs.QueryParam("withMetric") Boolean withMetric,
        @javax.ws.rs.QueryParam("latencyTopN") Integer latencyTopN) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                if (withMetric == null || !withMetric) {
                    return WSUtils.respond(result, OK, SUCCESS);
                } else {
                    TopologyCatalogWithMetric topologiesWithMetric = enrichMetricToTopology(
                        result, latencyTopN);
                    return WSUtils.respond(topologiesWithMetric, OK, SUCCESS);
                }
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies")
    @Timed
    public Response addTopology (Topology topology) {
        try {
            if (StringUtils.isEmpty(topology.getName())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, Topology.NAME);
            }
            if (StringUtils.isEmpty(topology.getConfig())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, Topology.CONFIG);
            }
            Topology createdTopology = catalogService.addTopology
                    (topology);
            return WSUtils.respond(createdTopology, CREATED, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @DELETE
    @Path("/topologies/{topologyId}")
    @Timed
    public Response removeTopology (@PathParam("topologyId") Long topologyId) {
        try {
            Topology removedTopology = catalogService.removeTopology
                    (topologyId);
            if (removedTopology != null) {
                return WSUtils.respond(removedTopology, OK, SUCCESS);
            } else {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND,
                        topologyId.toString());
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/topologies/{topologyId}")
    @Timed
    public Response addOrUpdateTopology (@PathParam("topologyId") Long topologyId,
                                        Topology topology) {
        try {
            if (StringUtils.isEmpty(topology.getName())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, Topology.NAME);
            }
            if (StringUtils.isEmpty(topology.getConfig())) {
                return WSUtils.respond(BAD_REQUEST,
                        BAD_REQUEST_PARAM_MISSING, Topology.CONFIG);
            }
            Topology result = catalogService.addOrUpdateTopology
                    (topologyId, topology);
            return WSUtils.respond(topology, OK, SUCCESS);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/topologies/{topologyId}/actions/status")
    @Timed
    public Response topologyStatus (@PathParam("topologyId") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                TopologyActions.Status status = actionsService.topologyStatus(CatalogToLayoutConverter.getTopologyLayout(result));
                return WSUtils.respond(status, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/validate")
    @Timed
    public Response validateTopology (@PathParam("topologyId") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                //catalogService.validateTopology(SCHEMA, topologyId);
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/deploy")
    @Timed
    public Response deployTopology (@PathParam("topologyId") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
//TODO: fix     catalogService.validateTopology(SCHEMA, topologyId);
                actionsService.deployTopology(result);
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            LOG.error("Failed to deploy the topology ", topologyId, ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/kill")
    @Timed
    public Response killTopology (@PathParam("topologyId") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                actionsService.killTopology(CatalogToLayoutConverter.getTopologyLayout(result));
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/suspend")
    @Timed
    public Response suspendTopology (@PathParam("topologyId") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                actionsService.suspendTopology(CatalogToLayoutConverter.getTopologyLayout(result));
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }

    @POST
    @Path("/topologies/{topologyId}/actions/resume")
    @Timed
    public Response resumeTopology (@PathParam("topologyId") Long topologyId) {
        try {
            Topology result = catalogService.getTopology(topologyId);
            if (result != null) {
                actionsService.resumeTopology(CatalogToLayoutConverter.getTopologyLayout(result));
                return WSUtils.respond(result, OK, SUCCESS);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, topologyId.toString());
    }


    private List<TopologyCatalogWithMetric> enrichMetricToTopologies(
        Collection<Topology> topologies, String sortType, Integer latencyTopN) {
        // need to also provide Topology Metric
        List<TopologyCatalogWithMetric> topologiesWithMetric = new ArrayList<>(topologies.size());
        for (Topology topology : topologies) {
            TopologyCatalogWithMetric topologyCatalogWithMetric = enrichMetricToTopology(topology, latencyTopN);
            topologiesWithMetric.add(topologyCatalogWithMetric);
        }

        if (sortType != null) {
            return topologiesWithMetric.stream().sorted((c1, c2) -> {
                switch (TopologySortType.valueOf(sortType.toUpperCase())) {
                case NAME:
                    return c1.getTopology().getName().compareTo(c2.getTopology().getName());
                case STATUS:
                    return c1.getRunning().compareTo(c2.getRunning());
                case LAST_UPDATED:
                    return c1.getTopology().getTimestamp().compareTo(c2.getTopology().getTimestamp());
                default:
                    throw new IllegalStateException("Not supported SortType: " + sortType);
                }
            }).collect(toList());
        } else {
            return topologiesWithMetric;
        }
    }

    private TopologyCatalogWithMetric enrichMetricToTopology(Topology topology, Integer latencyTopN) {
        if (latencyTopN == null) {
            latencyTopN = DEFAULT_N_OF_TOP_N_LATENCY;
        }

        TopologyCatalogWithMetric topologyCatalogWithMetric;
        try {
            TopologyMetrics.TopologyMetric topologyMetric = metricsService.getTopologyMetric(topology);
            List<Pair<String, Double>> latenciesTopN = metricsService.getTopNAndOtherComponentsLatency(topology, latencyTopN);
            topologyCatalogWithMetric = new TopologyCatalogWithMetric(topology, TopologyRunningStatus.RUNNING, topologyMetric, latenciesTopN);
        } catch (TopologyNotAliveException e) {
            topologyCatalogWithMetric = new TopologyCatalogWithMetric(topology, TopologyRunningStatus.NOT_RUNNING, null, null);
        } catch (StormNotReachableException | IOException e) {
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

