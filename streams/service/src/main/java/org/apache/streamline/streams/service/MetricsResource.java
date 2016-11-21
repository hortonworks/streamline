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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.Topology;
import org.apache.streamline.streams.catalog.TopologyComponent;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.metrics.service.TopologyMetricsService;
import org.apache.streamline.streams.metrics.topology.TopologyMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static org.apache.streamline.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST endpoint for metrics
 */
@Path("/v1/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsResource.class);

    private final StreamCatalogService catalogService;
    private final TopologyMetricsService metricsService;

    public MetricsResource(StreamCatalogService catalogService, TopologyMetricsService metricsService) {
        this.catalogService = catalogService;
        this.metricsService = metricsService;
    }

    @GET
    @Path("/topologies/{id}")
    @Timed
    public Response getTopologyMetricsById(@PathParam("id") Long id) {
        try {
            Topology topology = catalogService.getTopology(id);
            if (topology != null) {
                Map<String, TopologyMetrics.ComponentMetric> topologyMetrics = metricsService.getTopologyMetrics(topology);
                return WSUtils.respond(topologyMetrics, OK, SUCCESS);
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, id.toString());
    }

    @GET
    @Path("/topologies/{id}/components/{topologyComponentId}/complete_latency")
    @Timed
    public Response getCompleteLatency(@PathParam("id") Long id,
                                       @PathParam("topologyComponentId") Long topologyComponentId,
                                       @QueryParam("from") Long from,
                                       @QueryParam("to") Long to) {
        if (from == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "from");
        }
        if (to == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "to");
        }

        try {
            Topology topology = catalogService.getTopology(id);
            TopologyComponent topologyComponent = catalogService.getTopologyComponent(topologyComponentId);
            if (topology != null && topologyComponent != null) {
                Map<Long, Double> metrics = metricsService.getCompleteLatency(topology, topologyComponent, from, to);
                return WSUtils.respond(metrics, OK, SUCCESS);
            } else if (topology == null) {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "Topology: " + id.toString());
            } else {
                // topologyComponent == null
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "TopologyComponent: " + id.toString());
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/topologies/{id}/components/{topologyComponentId}/component_stats")
    @Timed
    public Response getComponentStats(@PathParam("id") Long id,
                                      @PathParam("topologyComponentId") Long topologyComponentId,
                                      @QueryParam("from") Long from,
                                      @QueryParam("to") Long to) {
        if (from == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "from");
        }
        if (to == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "to");
        }

        try {
            Topology topology = catalogService.getTopology(id);
            TopologyComponent topologyComponent = catalogService.getTopologyComponent(topologyComponentId);
            if (topology != null && topologyComponent != null) {
                Map<String, Map<Long, Double>> metrics = metricsService.getComponentStats(topology, topologyComponent, from, to);
                return WSUtils.respond(metrics, OK, SUCCESS);
            } else if (topology == null) {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "Topology: " + id.toString());
            } else {
                // topologyComponent == null
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "TopologyComponent: " + id.toString());
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/topologies/{id}/components/{topologyComponentId}/kafka_topic_offsets")
    @Timed
    public Response getKafkaTopicOffsets(@PathParam("id") Long id,
                                         @PathParam("topologyComponentId") Long topologyComponentId,
                                         @QueryParam("from") Long from,
                                         @QueryParam("to") Long to) {
        if (from == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "from");
        }
        if (to == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "to");
        }

        try {
            Topology topology = catalogService.getTopology(id);
            TopologyComponent topologyComponent = catalogService.getTopologyComponent(topologyComponentId);
            if (topology != null && topologyComponent != null) {
                Map<String, Map<Long, Double>> metrics = metricsService.getKafkaTopicOffsets(topology, topologyComponent, from, to);
                return WSUtils.respond(metrics, OK, SUCCESS);
            } else if (topology == null) {
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "Topology: " + id.toString());
            } else {
                // topologyComponent == null
                return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, "TopologyComponent: " + id.toString());
            }
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @GET
    @Path("/raw")
    @Timed
    public Response getMetrics(@QueryParam("metricName") String metricName,
                               @QueryParam("parameters") String parameters,
                               @QueryParam("from") Long from,
                               @QueryParam("to") Long to) {
        if (metricName == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "metricName");
        }
        if (from == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "from");
        }
        if (to == null) {
            return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "to");
        }

        try {
            Map<String, Map<Long, Double>> metrics = metricsService.getMetrics(metricName, parameters, from, to);
            return WSUtils.respond(metrics, OK, SUCCESS);
        } catch (Exception ex) {
            LOG.error("Got exception", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }
}
