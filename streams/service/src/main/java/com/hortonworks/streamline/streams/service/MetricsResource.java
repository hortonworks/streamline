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
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.ParallelStreamUtil;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyMetricsService;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import static com.hortonworks.streamline.streams.security.Permission.READ;
import static com.hortonworks.streamline.streams.security.Permission.WRITE;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST endpoint for metrics
 */
@Path("/v1/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsResource.class);
    private static final int FORK_JOIN_POOL_PARALLELISM = 10;

    private final ForkJoinPool forkJoinPool = new ForkJoinPool(FORK_JOIN_POOL_PARALLELISM);

    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private final TopologyMetricsService metricsService;

    public MetricsResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService, TopologyMetricsService metricsService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
        this.metricsService = metricsService;
    }

    @GET
    @Path("/topologies/{id}")
    @Timed
    public Response getTopologyMetricsById(@PathParam("id") Long id, @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, id, READ);
        Topology topology = catalogService.getTopology(id);
        if (topology != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            Map<String, TopologyMetrics.ComponentMetric> topologyMetrics = metricsService.getTopologyMetrics(topology, asUser);
            return WSUtils.respondEntity(topologyMetrics, OK);
        }

        throw EntityNotFoundException.byId(id.toString());
    }

    @GET
    @Path("/topologies/{id}/timeseries")
    @Timed
    public Response getTopologyMetricsViaTimeSeriesById(@PathParam("id") Long id,
                                                        @QueryParam("from") Long from,
                                                        @QueryParam("to") Long to,
                                                        @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, id, READ);
        assertTimeRange(from, to);
        Topology topology = catalogService.getTopology(id);
        if (topology != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            TopologyTimeSeriesMetrics.TimeSeriesComponentMetric topologyMetrics =
                    metricsService.getTopologyStats(topology, from, to, asUser);
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
                                       @QueryParam("to") Long to,
                                       @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, id, READ);
        assertTimeRange(from, to);
        Topology topology = catalogService.getTopology(id);
        TopologyComponent topologyComponent = catalogService.getTopologyComponent(id, topologyComponentId);
        if (topology != null && topologyComponent != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            Map<Long, Double> metrics = metricsService.getCompleteLatency(topology, topologyComponent, from, to, asUser);
            return WSUtils.respondEntity(metrics, OK);
        } else if (topology == null) {
            throw EntityNotFoundException.byId("Topology: " + id.toString());
        } else {
            // topologyComponent == null
            throw EntityNotFoundException.byId("TopologyComponent: " + id.toString());
        }
    }

    @GET
    @Path("/topologies/{id}/components/all/component_stats")
    @Timed
    public Response getComponentStats(@PathParam("id") Long id,
                                      @QueryParam("from") Long from,
                                      @QueryParam("to") Long to,
                                      @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, id, READ);
        assertTimeRange(from, to);
        Topology topology = catalogService.getTopology(id);
        if (topology != null) {
            List<TopologyComponent> topologyComponents = new ArrayList<>();
            List<com.hortonworks.registries.common.QueryParam> queryParams = new ArrayList<>();
            queryParams.add(new com.hortonworks.registries.common.QueryParam("topologyId", String.valueOf(topology.getId())));
            queryParams.add(new com.hortonworks.registries.common.QueryParam("versionId", String.valueOf(topology.getVersionId())));

            topologyComponents.addAll(catalogService.listTopologySources(queryParams));
            topologyComponents.addAll(catalogService.listTopologyProcessors(queryParams));
            topologyComponents.addAll(catalogService.listTopologySinks(queryParams));

            Map<String, TopologyTimeSeriesMetrics.TimeSeriesComponentMetric> topologyMetrics = ParallelStreamUtil.execute(() ->
                    topologyComponents.parallelStream()
                            .map(c -> {
                                try {
                                    String asUser = WSUtils.getUserFromSecurityContext(securityContext);
                                    return Pair.of(c, metricsService.getComponentStats(topology, c, from, to, asUser));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .collect(toMap(m -> m.getKey().getId().toString(), m -> m.getValue())), forkJoinPool);

            return WSUtils.respondEntity(topologyMetrics, OK);
        }

        throw EntityNotFoundException.byId(id.toString());
    }

    @GET
    @Path("/topologies/{id}/components/{topologyComponentId}/component_stats")
    @Timed
    public Response getComponentStats(@PathParam("id") Long id,
                                      @PathParam("topologyComponentId") Long topologyComponentId,
                                      @QueryParam("from") Long from,
                                      @QueryParam("to") Long to,
                                      @Context SecurityContext securityContext) throws IOException {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, id, READ);
        assertTimeRange(from, to);
        Topology topology = catalogService.getTopology(id);
        TopologyComponent topologyComponent = catalogService.getTopologyComponent(id, topologyComponentId);
        if (topology != null && topologyComponent != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics =
                    metricsService.getComponentStats(topology, topologyComponent, from, to, asUser);
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
                                         @QueryParam("to") Long to,
                                         @Context SecurityContext securityContext) throws IOException {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, id, READ);
        assertTimeRange(from, to);
        Topology topology = catalogService.getTopology(id);
        TopologyComponent topologyComponent = catalogService.getTopologyComponent(id, topologyComponentId);
        if (topology != null && topologyComponent != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            Map<String, Map<Long, Double>> metrics = metricsService.getKafkaTopicOffsets(topology, topologyComponent, from, to, asUser);
            return WSUtils.respondEntity(metrics, OK);
        } else if (topology == null) {
            throw EntityNotFoundException.byId("Topology: " + id.toString());
        } else {
            // topologyComponent == null
            throw EntityNotFoundException.byId("TopologyComponent: " + id.toString());
        }
    }

    private void assertTimeRange(Long from, Long to) {
        if (from == null) {
            throw BadRequestException.missingParameter("from");
        }
        if (to == null) {
            throw BadRequestException.missingParameter("to");
        }
    }
}
