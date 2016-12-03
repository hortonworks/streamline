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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.Topology;
import org.apache.streamline.streams.catalog.TopologyComponent;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.metrics.topology.TopologyMetrics;
import org.apache.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import org.apache.streamline.streams.service.exception.request.BadRequestException;
import org.apache.streamline.streams.service.exception.request.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST endpoint for metrics
 */
@Path("/v1/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsResource.class);

    private final StreamCatalogService catalogService;

    public MetricsResource(StreamCatalogService service) {
        this.catalogService = service;
    }

    @GET
    @Path("/topologies/{id}")
    @Timed
    public Response getTopologyMetricsById(@PathParam("id") Long id) throws Exception {
        Topology topology = catalogService.getTopology(id);
        if (topology != null) {
            Map<String, TopologyMetrics.ComponentMetric> topologyMetrics = catalogService.getTopologyMetrics(topology);
            return WSUtils.respondEntity(topologyMetrics, OK);
        }

        throw EntityNotFoundException.byId(id.toString());
    }

    @GET
    @Path("/topologies/timeseries/{id}")
    @Timed
    public Response getTopologyMetricsViaTimeSeriesById(@PathParam("id") Long id,
                                                        @QueryParam("from") Long from,
                                                        @QueryParam("to") Long to) throws Exception {
        assertTimeRange(from, to);

        Topology topology = catalogService.getTopology(id);
        if (topology != null) {
            List<TopologyComponent> topologyComponents = new ArrayList<>();
            topologyComponents.addAll(catalogService.listTopologySources());
            topologyComponents.addAll(catalogService.listTopologyProcessors());
            topologyComponents.addAll(catalogService.listTopologySinks());

            Map<String, TopologyTimeSeriesMetrics.TimeSeriesComponentMetric> topologyMetrics = new HashMap<>();
            topologyComponents.stream().map(c -> {
                try {
                    return Pair.of(c, catalogService.getComponentStats(topology, c, from, to));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).forEach(m -> topologyMetrics.put(m.getKey().getId().toString(), m.getValue()));

            return WSUtils.respondEntity(topologyMetrics, OK);
        }

        throw EntityNotFoundException.byId(id.toString());
    }

    @GET
    @Path("/topologies/{id}/components/{topologyComponentId}/complete_latency")
    @Timed
    public Response getCompleteLatency(@PathParam("id") Long id,
                                       @PathParam("topologyComponentId") Long topologyComponentId,
                                       @QueryParam("from") Long from,
                                       @QueryParam("to") Long to) throws Exception {
        assertTimeRange(from, to);

        Topology topology = catalogService.getTopology(id);
        TopologyComponent topologyComponent = catalogService.getTopologyComponent(id, topologyComponentId);
        if (topology != null && topologyComponent != null) {
            Map<Long, Double> metrics = catalogService.getCompleteLatency(topology, topologyComponent, from, to);
            return WSUtils.respondEntity(metrics, OK);
        } else if (topology == null) {
            throw EntityNotFoundException.byId("Topology: " + id.toString());
        } else {
            // topologyComponent == null
            throw EntityNotFoundException.byId("TopologyComponent: " + id.toString());
        }
    }

    @GET
    @Path("/topologies/{id}/components/{topologyComponentId}/component_stats")
    @Timed
    public Response getComponentStats(@PathParam("id") Long id,
                                      @PathParam("topologyComponentId") Long topologyComponentId,
                                      @QueryParam("from") Long from,
                                      @QueryParam("to") Long to) throws IOException {
        assertTimeRange(from, to);

        Topology topology = catalogService.getTopology(id);
        TopologyComponent topologyComponent = catalogService.getTopologyComponent(id, topologyComponentId);
        if (topology != null && topologyComponent != null) {
            TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics =
                    catalogService.getComponentStats(topology, topologyComponent, from, to);
            return WSUtils.respondEntity(metrics, OK);
        } else if (topology == null) {
            throw EntityNotFoundException.byId("Topology: " + id.toString());
        } else {
            // topologyComponent == null
            throw EntityNotFoundException.byId("TopologyComponent: " + id.toString());
        }
    }

    @GET
    @Path("/topologies/{id}/components/{topologyComponentId}/kafka_topic_offsets")
    @Timed
    public Response getKafkaTopicOffsets(@PathParam("id") Long id,
                                         @PathParam("topologyComponentId") Long topologyComponentId,
                                         @QueryParam("from") Long from,
                                         @QueryParam("to") Long to) throws IOException {
        assertTimeRange(from, to);

        Topology topology = catalogService.getTopology(id);
        TopologyComponent topologyComponent = catalogService.getTopologyComponent(id, topologyComponentId);
        if (topology != null && topologyComponent != null) {
            Map<String, Map<Long, Double>> metrics = catalogService.getKafkaTopicOffsets(topology, topologyComponent, from, to);
            return WSUtils.respondEntity(metrics, OK);
        } else if (topology == null) {
            throw EntityNotFoundException.byId("Topology: " + id.toString());
        } else {
            // topologyComponent == null
            throw EntityNotFoundException.byId("TopologyComponent: " + id.toString());
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
            throw BadRequestException.missingParameter("metricName");
        }
        assertTimeRange(from, to);

        Map<String, Map<Long, Double>> metrics = catalogService.getMetrics(metricName, parameters, from, to);
        return WSUtils.respondEntity(metrics, OK);
    }

    private void assertTimeRange(@QueryParam("from") Long from, @QueryParam("to") Long to) {
        if (from == null) {
            throw BadRequestException.missingParameter("from");
        }
        if (to == null) {
            throw BadRequestException.missingParameter("to");
        }
    }
}
