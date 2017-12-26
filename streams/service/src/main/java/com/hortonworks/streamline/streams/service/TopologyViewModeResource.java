package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.*;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.metrics.storm.topology.StormMappedMetric;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyMetricsService;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import org.jooq.lambda.Unchecked;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.hortonworks.streamline.streams.security.Permission.READ;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyViewModeResource {

    public static final int THRESHOLD_VALID_MINIMUM_METRICS_POINTS = 3;
    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private final TopologyMetricsService metricsService;

    public TopologyViewModeResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService,
                                    TopologyMetricsService metricsService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
        this.metricsService = metricsService;
    }

    @GET
    @Path("/topologies/{topologyId}/metrics")
    @Timed
    public Response getTopology(@PathParam("topologyId") Long topologyId,
                                @QueryParam("from") Long from,
                                @QueryParam("to") Long to,
                                @Context SecurityContext securityContext) throws IOException {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);

        assertTimeRange(from, to);

        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            TopologyTimeSeriesMetrics.TimeSeriesComponentMetric topologyMetrics =
                    metricsService.getTopologyStats(topology, from, to, asUser);

            long prevFrom = from - (to - from);
            long prevTo = from - 1;
            TopologyTimeSeriesMetrics.TimeSeriesComponentMetric prevTopologyMetrics =
                    metricsService.getTopologyStats(topology, prevFrom, prevTo, asUser);

            if (!checkMetricsResponseHasFullRangeOfTime(prevTopologyMetrics, prevFrom, prevTo)) {
                prevTopologyMetrics = null;
            }

            ComponentMetricSummary viewModeComponentMetric = ComponentMetricSummary.convertTopologyMetric(
                    topologyMetrics, prevTopologyMetrics);
            TopologyWithMetric metric = new TopologyWithMetric(topology, viewModeComponentMetric,
                    topologyMetrics);
            return WSUtils.respondEntity(metric, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    private boolean checkMetricsResponseHasFullRangeOfTime(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics,
                                                           long from, long to) {
        if (metrics == null) {
            return false;
        }

        Map<Long, Double> target = metrics.getProcessedTime();
        if (target == null || target.size() == 0) {
            // fail back to see output records
            target = metrics.getOutputRecords();

            if (target == null || target.size() == 0) {
                return false;
            }
        }

        List<Long> sortedTimestamp = target.keySet().stream().sorted().collect(toList());
        if (sortedTimestamp.size() < THRESHOLD_VALID_MINIMUM_METRICS_POINTS) {
            // no granularity to check, or not enough values to use
            return false;
        }

        Long firstTimestamp = sortedTimestamp.get(0);
        Long secondTimestamp = sortedTimestamp.get(1);
        Long lastTimestamp = sortedTimestamp.get(sortedTimestamp.size() - 1);

        long granularity = secondTimestamp - firstTimestamp;

        // assuming that time-series DB will provide the self-aggregated metrics points
        // only with ranges which raw points are available

        // this means time-series DB doesn't have metric points in earlier part of time range
        if (firstTimestamp - from > granularity) {
            return false;
        }

        // this means time-series DB doesn't have metric points in later part of time range
        if (to - lastTimestamp > granularity) {
            return false;
        }

        return true;
    }

    @GET
    @Path("/topologies/{topologyId}/sources/metrics")
    @Timed
    public Response listSources(@PathParam("topologyId") Long topologyId,
                                @QueryParam("from") Long from,
                                @QueryParam("to") Long to,
                                @Context UriInfo uriInfo,
                                @Context SecurityContext securityContext) throws IOException {
        return listComponents(topologyId, from, to, uriInfo, securityContext, TopologySource.class);
    }

    @GET
    @Path("/topologies/{topologyId}/sources/{sourceId}/metrics")
    @Timed
    public Response getSource(@PathParam("topologyId") Long topologyId,
                              @PathParam("sourceId") Long sourceId,
                              @QueryParam("from") Long from,
                              @QueryParam("to") Long to,
                              @Context UriInfo uriInfo,
                              @Context SecurityContext securityContext) throws IOException {
        return getComponent(topologyId, sourceId, from, to, uriInfo, securityContext, TopologySource.class);
    }

    @GET
    @Path("/topologies/{topologyId}/processors/metrics")
    @Timed
    public Response listProcessors(@PathParam("topologyId") Long topologyId,
                                   @QueryParam("from") Long from,
                                   @QueryParam("to") Long to,
                                   @Context UriInfo uriInfo,
                                   @Context SecurityContext securityContext) throws IOException {
        return listComponents(topologyId, from, to, uriInfo, securityContext, TopologyProcessor.class);
    }

    @GET
    @Path("/topologies/{topologyId}/processors/{processorId}/metrics")
    @Timed
    public Response getProcessors(@PathParam("topologyId") Long topologyId,
                                  @PathParam("processorId") Long processorId,
                                  @QueryParam("from") Long from,
                                  @QueryParam("to") Long to,
                                  @Context UriInfo uriInfo,
                                  @Context SecurityContext securityContext) throws IOException {
        return getComponent(topologyId, processorId, from, to, uriInfo, securityContext, TopologyProcessor.class);
    }

    @GET
    @Path("/topologies/{topologyId}/sinks/metrics")
    @Timed
    public Response listSinks(@PathParam("topologyId") Long topologyId,
                              @QueryParam("from") Long from,
                              @QueryParam("to") Long to,
                              @Context UriInfo uriInfo,
                              @Context SecurityContext securityContext) throws IOException {
        return listComponents(topologyId, from, to, uriInfo, securityContext, TopologySink.class);
    }

    @GET
    @Path("/topologies/{topologyId}/sinks/{sinkId}/metrics")
    @Timed
    public Response getSink(@PathParam("topologyId") Long topologyId,
                            @PathParam("sinkId") Long sinkId,
                            @QueryParam("from") Long from,
                            @QueryParam("to") Long to,
                            @Context UriInfo uriInfo,
                            @Context SecurityContext securityContext) throws IOException {
        return getComponent(topologyId, sinkId, from, to, uriInfo, securityContext, TopologySink.class);
    }

    private Response listComponents(Long topologyId, Long from, Long to, UriInfo uriInfo,
                                    SecurityContext securityContext, Class<? extends TopologyComponent> clazz) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);

        assertTimeRange(from, to);

        Long currentVersionId = catalogService.getCurrentVersionId(topologyId);

        List<com.hortonworks.registries.common.QueryParam> queryParams = WSUtils.buildTopologyIdAndVersionIdAwareQueryParams(topologyId, currentVersionId, uriInfo);

        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            Collection<? extends TopologyComponent> components;
            if (clazz.equals(TopologySource.class)) {
                components = catalogService.listTopologySources(queryParams);
            } else if (clazz.equals(TopologyProcessor.class)) {
                components = catalogService.listTopologyProcessors(queryParams);
            } else if (clazz.equals(TopologySink.class)) {
                components = catalogService.listTopologySinks(queryParams);
            } else {
                throw new IllegalArgumentException("Unexpected class in parameter: " + clazz);
            }
            if (components != null) {
                String asUser = WSUtils.getUserFromSecurityContext(securityContext);

                List<TopologyComponentWithMetric> componentsWithMetrics = components.stream()
                        .map(Unchecked.function(s -> {
                            ComponentMetricSummary overviewMetric;
                            TopologyTimeSeriesMetrics.TimeSeriesComponentMetric currentMetric = metricsService.getComponentStats(topology, s, from, to, asUser);
                            TopologyTimeSeriesMetrics.TimeSeriesComponentMetric previousMetric = metricsService.getComponentStats(topology, s, from - (to - from), from - 1, asUser);
                            if (clazz.equals(TopologySource.class)) {
                                overviewMetric = ComponentMetricSummary.convertSourceMetric(
                                        currentMetric, previousMetric);

                            } else {
                                overviewMetric = ComponentMetricSummary.convertNonSourceMetric(
                                        currentMetric, previousMetric);
                            }

                            return new TopologyComponentWithMetric(s, overviewMetric, currentMetric);
                        }))
                        .collect(toList());

                return WSUtils.respondEntities(componentsWithMetrics, OK);
            }

            throw EntityNotFoundException.byFilter(queryParams.toString());
        }

        throw EntityNotFoundException.byName("topology ID " + topologyId);
    }

    private Response getComponent(Long topologyId, Long componentId, Long from, Long to, UriInfo uriInfo,
                                  SecurityContext securityContext, Class<? extends TopologyComponent> clazz)
            throws IOException {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                Topology.NAMESPACE, topologyId, READ);

        assertTimeRange(from, to);

        Topology topology = catalogService.getTopology(topologyId);
        if (topology != null) {
            TopologyComponent component;
            if (clazz.equals(TopologySource.class)) {
                component = catalogService.getTopologySource(topologyId, componentId);
            } else if (clazz.equals(TopologyProcessor.class)) {
                component = catalogService.getTopologyProcessor(topologyId, componentId);
            } else if (clazz.equals(TopologySink.class)) {
                component = catalogService.getTopologySink(topologyId, componentId);
            } else {
                throw new IllegalArgumentException("Unexpected class in parameter: " + clazz);
            }

            if (component != null) {
                String asUser = WSUtils.getUserFromSecurityContext(securityContext);

                ComponentMetricSummary overviewMetric;
                TopologyTimeSeriesMetrics.TimeSeriesComponentMetric currentMetric = metricsService.getComponentStats(topology, component, from, to, asUser);
                TopologyTimeSeriesMetrics.TimeSeriesComponentMetric previousMetric = metricsService.getComponentStats(topology, component, from - (to - from), from - 1, asUser);
                if (clazz.equals(TopologySource.class)) {
                    overviewMetric = ComponentMetricSummary.convertSourceMetric(currentMetric, previousMetric);
                } else {
                    overviewMetric = ComponentMetricSummary.convertNonSourceMetric(currentMetric, previousMetric);
                }

                TopologyComponentWithMetric componentWithMetrics =
                        new TopologyComponentWithMetric(component, overviewMetric, currentMetric);

                return WSUtils.respondEntity(componentWithMetrics, OK);
            }

            throw EntityNotFoundException.byName("component ID " + componentId);
        }

        throw EntityNotFoundException.byName("topology ID " + topologyId);
    }

    private void assertTimeRange(Long from, Long to) {
        if (from == null) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException.missingParameter("from");
        }
        if (to == null) {
            throw BadRequestException.missingParameter("to");
        }
    }

    /**
     * Note: Given that UI of view mode is tied to Apache Storm, using the term of Storm.
     */
    private static class ComponentMetricSummary {
        private static final String METRIC_NAME_ACK_COUNT = "ackedRecords";
        private static final String METRIC_NAME_COMPLETE_LATENCY = "completeLatency";
        private static final String METRIC_NAME_EXECUTE_LATENCY = "executeLatency";

        private final Long emitted;
        private final Long acked;
        private final Long failed;
        private final Double latency;
        private final Double completeLatency;
        private final Double processTime;
        private final Double executeTime;

        private final Long prevEmitted;
        private final Long prevAcked;
        private final Long prevFailed;
        private final Double prevLatency;
        private final Double prevCompleteLatency;
        private final Double prevProcessTime;
        private final Double prevExecuteTime;

        public ComponentMetricSummary(Long emitted, Long acked, Long failed, Double latency, Double completeLatency,
                                      Double processTime, Double executeTime,
                                      Long prevEmitted, Long prevAcked, Long prevFailed, Double prevLatency,
                                      Double prevCompleteLatency, Double prevProcessTime, Double prevExecuteTime) {
            this.emitted = emitted;
            this.acked = acked;
            this.failed = failed;
            this.latency = latency;
            this.completeLatency = completeLatency;
            this.processTime = processTime;
            this.executeTime = executeTime;
            this.prevEmitted = prevEmitted;
            this.prevAcked = prevAcked;
            this.prevFailed = prevFailed;
            this.prevLatency = prevLatency;
            this.prevCompleteLatency = prevCompleteLatency;
            this.prevProcessTime = prevProcessTime;
            this.prevExecuteTime = prevExecuteTime;
        }

        public Long getEmitted() {
            return emitted;
        }

        public Long getAcked() {
            return acked;
        }

        public Long getFailed() {
            return failed;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Double getLatency() {
            return latency;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Double getCompleteLatency() {
            return completeLatency;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Double getProcessTime() {
            return processTime;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Double getExecuteTime() {
            return executeTime;
        }

        public Long getPrevEmitted() {
            return prevEmitted;
        }

        public Long getPrevAcked() {
            return prevAcked;
        }

        public Long getPrevFailed() {
            return prevFailed;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Double getPrevLatency() {
            return prevLatency;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Double getPrevCompleteLatency() {
            return prevCompleteLatency;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Double getPrevProcessTime() {
            return prevProcessTime;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Double getPrevExecuteTime() {
            return prevExecuteTime;
        }

        public static ComponentMetricSummary convertTopologyMetric(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics,
                                                                   TopologyTimeSeriesMetrics.TimeSeriesComponentMetric prevMetrics) {
            Long emitted = aggregateEmitted(metrics);
            Long acked = aggregatedAcked(metrics);
            Double latency = aggregateCompleteLatency(metrics);
            Long failed = aggregateFailed(metrics);

            Long prevEmitted = null;
            Long prevAcked = null;
            Double prevLatency = null;
            Long prevFailed = null;

            // aggregate the value only if it is available
            if (prevMetrics != null) {
                prevEmitted = aggregateEmitted(prevMetrics);
                prevAcked = aggregatedAcked(prevMetrics);
                prevLatency = aggregateCompleteLatency(prevMetrics);
                prevFailed = aggregateFailed(prevMetrics);
            }

            return new ComponentMetricSummary(emitted, acked, failed, latency, null, null, null,
                    prevEmitted, prevAcked, prevFailed, prevLatency, null, null, null);
        }

        public static ComponentMetricSummary convertSourceMetric(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics,
                                                                 TopologyTimeSeriesMetrics.TimeSeriesComponentMetric prevMetrics) {
            Long emitted = aggregateEmitted(metrics);
            Long acked = aggregatedAcked(metrics);
            Double completeLatency = aggregateCompleteLatency(metrics);
            Long failed = aggregateFailed(metrics);

            Long prevEmitted = null;
            Long prevAcked = null;
            Double prevCompleteLatency = null;
            Long prevFailed = null;

            // aggregate the value only if it is available
            if (prevMetrics != null) {
                prevEmitted = aggregateEmitted(prevMetrics);
                prevAcked = aggregatedAcked(prevMetrics);
                prevCompleteLatency = aggregateCompleteLatency(prevMetrics);
                prevFailed = aggregateFailed(prevMetrics);
            }

            return new ComponentMetricSummary(emitted, acked, failed, null, completeLatency, null, null,
                    prevEmitted, prevAcked, prevFailed, null, prevCompleteLatency, null, null);
        }

        public static ComponentMetricSummary convertNonSourceMetric(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics,
                                                                    TopologyTimeSeriesMetrics.TimeSeriesComponentMetric prevMetrics) {
            Long emitted = aggregateEmitted(metrics);
            Long acked = aggregatedAcked(metrics);
            Double processLatency = aggregateProcessLatency(metrics);
            Double executeLatency = aggregateExecuteLatency(metrics);
            Long failed = aggregateFailed(metrics);

            Long prevEmitted = null;
            Long prevAcked = null;
            Double prevProcessLatency = null;
            Double prevExecuteLatency = null;
            Long prevFailed = null;

            // aggregate the value only if it is available
            if (prevMetrics != null) {
                prevEmitted = aggregateEmitted(prevMetrics);
                prevAcked = aggregatedAcked(prevMetrics);
                prevProcessLatency = aggregateProcessLatency(prevMetrics);
                prevExecuteLatency = aggregateExecuteLatency(prevMetrics);
                prevFailed = aggregateFailed(prevMetrics);
            }

            return new ComponentMetricSummary(emitted, acked, failed, null, null, processLatency, executeLatency,
                    prevEmitted, prevAcked, prevFailed, null, null, prevProcessLatency, prevExecuteLatency);
        }

        private static long aggregateFailed(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
            return metrics.getFailedRecords().values().stream().mapToLong(Double::longValue).sum();
        }

        private static double aggregateProcessLatency(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
            return metrics.getProcessedTime().values().stream().mapToDouble(v -> v).average().orElse(0.0d);
        }

        private static double aggregateExecuteLatency(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
            return metrics.getMisc().getOrDefault(StormMappedMetric.executeTime.name(), Collections.emptyMap())
                    .values().stream().mapToDouble(v -> v).average().orElse(0.0d);
        }

        private static double aggregateCompleteLatency(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
            return metrics.getMisc().getOrDefault(StormMappedMetric.completeLatency.name(), Collections.emptyMap())
                    .values().stream().mapToDouble(v -> v).average().orElse(0.0d);
        }

        private static long aggregateEmitted(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
            return metrics.getOutputRecords().values().stream().mapToLong(Double::longValue).sum();
        }

        private static long aggregatedAcked(TopologyTimeSeriesMetrics.TimeSeriesComponentMetric metrics) {
            return metrics.getMisc().getOrDefault(METRIC_NAME_ACK_COUNT, Collections.emptyMap())
                    .values().stream().mapToLong(Double::longValue).sum();
        }
    }

    private static class TopologyWithMetric {
        private final Topology topology;
        private final ComponentMetricSummary overviewMetrics;
        private final TopologyTimeSeriesMetrics.TimeSeriesComponentMetric timeSeriesMetrics;

        public TopologyWithMetric(Topology topology, ComponentMetricSummary overviewMetrics,
                                  TopologyTimeSeriesMetrics.TimeSeriesComponentMetric timeSeriesMetrics) {
            this.topology = topology;
            this.overviewMetrics = overviewMetrics;
            this.timeSeriesMetrics = timeSeriesMetrics;
        }

        public Topology getTopology() {
            return topology;
        }

        public ComponentMetricSummary getOverviewMetrics() {
            return overviewMetrics;
        }

        public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getTimeSeriesMetrics() {
            return timeSeriesMetrics;
        }
    }

    private static class TopologyComponentWithMetric {
        private final TopologyComponent component;
        private final ComponentMetricSummary overviewMetrics;
        private final TopologyTimeSeriesMetrics.TimeSeriesComponentMetric timeSeriesMetrics;

        public TopologyComponentWithMetric(TopologyComponent component,
                                           ComponentMetricSummary overviewMetrics,
                                           TopologyTimeSeriesMetrics.TimeSeriesComponentMetric timeSeriesMetrics) {
            this.component = component;
            this.overviewMetrics = overviewMetrics;
            this.timeSeriesMetrics = timeSeriesMetrics;
        }

        public TopologyComponent getComponent() {
            return component;
        }

        public ComponentMetricSummary getOverviewMetrics() {
            return overviewMetrics;
        }

        public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getTimeSeriesMetrics() {
            return timeSeriesMetrics;
        }
    }
}
