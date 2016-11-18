package org.apache.streamline.streams.metrics.service;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.streamline.streams.catalog.CatalogToLayoutConverter;
import org.apache.streamline.streams.catalog.Topology;
import org.apache.streamline.streams.catalog.TopologyComponent;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.metrics.topology.TopologyMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class TopologyMetricsService {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyMetricsService.class);

    private final TopologyMetrics topologyMetrics;
    private final StreamCatalogService catalogService;

    public TopologyMetricsService(TopologyMetrics topologyMetrics, StreamCatalogService catalogService) {
        this.topologyMetrics = topologyMetrics;
        this.catalogService = catalogService;
    }

    public Map<String, TopologyMetrics.ComponentMetric> getTopologyMetrics(Topology topology) throws IOException {
        return this.topologyMetrics.getMetricsForTopology(CatalogToLayoutConverter.getTopologyLayout(topology));
    }

    public Map<Long, Double> getCompleteLatency(Topology topology, TopologyComponent component, long from, long to) throws Exception {
        return this.topologyMetrics.getCompleteLatency(CatalogToLayoutConverter.getTopologyLayout(topology),
                CatalogToLayoutConverter.getComponentLayout(component), from, to);
    }

    public Map<String, Map<Long, Double>> getComponentStats(Topology topology, TopologyComponent component, Long from, Long to) throws IOException {
        return this.topologyMetrics.getComponentStats(CatalogToLayoutConverter.getTopologyLayout(topology),
                CatalogToLayoutConverter.getComponentLayout(component), from, to);
    }

    public Map<String, Map<Long, Double>> getKafkaTopicOffsets(Topology topology, TopologyComponent component, Long from, Long to) throws IOException {
        return this.topologyMetrics.getkafkaTopicOffsets(CatalogToLayoutConverter.getTopologyLayout(topology),
                CatalogToLayoutConverter.getComponentLayout(component), from, to);
    }

    public Map<String, Map<Long, Double>> getMetrics(String metricName, String parameters, Long from, Long to) {
        return this.topologyMetrics.getTimeSeriesQuerier().getRawMetrics(metricName, parameters, from, to);
    }

    public TopologyMetrics.TopologyMetric getTopologyMetric(Topology topology) throws IOException {
        return this.topologyMetrics.getTopologyMetric(CatalogToLayoutConverter.getTopologyLayout(topology));
    }

    public List<Pair<String, Double>> getTopNAndOtherComponentsLatency(Topology topology, int nOfTopN) throws IOException {
        Map<String, TopologyMetrics.ComponentMetric> metricsForTopology = this.topologyMetrics
                .getMetricsForTopology(CatalogToLayoutConverter.getTopologyLayout(topology));

        List<Pair<String, Double>> topNAndOther = new ArrayList<>();

        List<ImmutablePair<String, Double>> latencyOrderedComponents = metricsForTopology.entrySet().stream()
                .map((x) -> new ImmutablePair<>(x.getKey(), x.getValue().getProcessedTime()))
                // reversed sort
                .sorted((c1, c2) -> {
                    if (c2.getValue() == null) {
                        // assuming c1 is bigger
                        return -1;
                    } else {
                        return c2.getValue().compareTo(c1.getValue());
                    }
                })
                .collect(toList());

        latencyOrderedComponents.stream().limit(nOfTopN).forEachOrdered(topNAndOther::add);
        double sumLatencyOthers = latencyOrderedComponents.stream()
                .skip(nOfTopN).filter((x) -> x.getValue() != null)
                .mapToDouble(Pair::getValue).sum();

        topNAndOther.add(new ImmutablePair<>("Others", sumLatencyOthers));

        return topNAndOther;
    }

}
