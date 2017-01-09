package org.apache.streamline.streams.metrics.topology.service;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.streamline.streams.catalog.CatalogToLayoutConverter;
import org.apache.streamline.streams.catalog.Namespace;
import org.apache.streamline.streams.catalog.Topology;
import org.apache.streamline.streams.catalog.TopologyComponent;
import org.apache.streamline.streams.catalog.container.ContainingNamespaceAwareContainer;
import org.apache.streamline.streams.catalog.service.EnvironmentService;
import org.apache.streamline.streams.metrics.container.TopologyMetricsContainer;
import org.apache.streamline.streams.metrics.topology.TopologyMetrics;
import org.apache.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class TopologyMetricsService implements ContainingNamespaceAwareContainer {

    private final EnvironmentService environmentService;
    private final TopologyMetricsContainer topologyMetricsContainer;

    public TopologyMetricsService(EnvironmentService environmentService) {
        this.environmentService = environmentService;
        this.topologyMetricsContainer = new TopologyMetricsContainer(environmentService);
    }

    public Map<String, TopologyMetrics.ComponentMetric> getTopologyMetrics(Topology topology) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getMetricsForTopology(CatalogToLayoutConverter.getTopologyLayout(topology));
    }

    public Map<Long, Double> getCompleteLatency(Topology topology, TopologyComponent component, long from, long to) throws Exception {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getCompleteLatency(CatalogToLayoutConverter.getTopologyLayout(topology), CatalogToLayoutConverter.getComponentLayout(component), from, to);
    }

    public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getTopologyStats(Topology topology, Long from, Long to) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getTopologyStats(CatalogToLayoutConverter.getTopologyLayout(topology), from, to);
    }

    public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getComponentStats(Topology topology, TopologyComponent component, Long from, Long to) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getComponentStats(CatalogToLayoutConverter.getTopologyLayout(topology), CatalogToLayoutConverter.getComponentLayout(component), from, to);
    }

    public Map<String, Map<Long, Double>> getKafkaTopicOffsets(Topology topology, TopologyComponent component, Long from, Long to) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getkafkaTopicOffsets(CatalogToLayoutConverter.getTopologyLayout(topology), CatalogToLayoutConverter.getComponentLayout(component), from, to);
    }

    public TopologyMetrics.TopologyMetric getTopologyMetric(Topology topology) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getTopologyMetric(CatalogToLayoutConverter.getTopologyLayout(topology));
    }

    public List<Pair<String, Double>> getTopNAndOtherComponentsLatency(Topology topology, int nOfTopN) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        Map<String, TopologyMetrics.ComponentMetric> metricsForTopology = topologyMetrics
                .getMetricsForTopology(CatalogToLayoutConverter.getTopologyLayout(topology));

        List<Pair<String, Double>> topNAndOther = new ArrayList<>();

        List<ImmutablePair<String, Double>> latencyOrderedComponents = metricsForTopology.entrySet().stream()
                .map((x) -> new ImmutablePair<>(x.getValue().getComponentName(), x.getValue().getProcessedTime()))
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

    @Override
    public void invalidateInstance(Long namespaceId) {
        try {
            topologyMetricsContainer.invalidateInstance(namespaceId);
        } catch (Throwable e) {
            // swallow
        }
    }

    private TopologyMetrics getTopologyMetricsInstance(Topology topology) {
        Namespace namespace = environmentService.getNamespace(topology.getNamespaceId());
        if (namespace == null) {
            throw new RuntimeException("Corresponding namespace not found: " + topology.getNamespaceId());
        }

        TopologyMetrics topologyMetrics = topologyMetricsContainer.findInstance(namespace);
        if (topologyMetrics == null) {
            throw new RuntimeException("Can't find Topology Metrics for such namespace " + topology.getNamespaceId());
        }
        return topologyMetrics;
    }
}
