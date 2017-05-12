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
package com.hortonworks.streamline.streams.metrics.topology.service;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.hortonworks.streamline.streams.catalog.CatalogToLayoutConverter;
import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.cluster.container.ContainingNamespaceAwareContainer;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.metrics.container.TopologyMetricsContainer;
import com.hortonworks.streamline.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class TopologyMetricsService implements ContainingNamespaceAwareContainer {

    private final EnvironmentService environmentService;
    private final TopologyMetricsContainer topologyMetricsContainer;

    public TopologyMetricsService(EnvironmentService environmentService, Subject subject) {
        this.environmentService = environmentService;
        this.topologyMetricsContainer = new TopologyMetricsContainer(environmentService, subject);
    }

    public Map<String, TopologyMetrics.ComponentMetric> getTopologyMetrics(Topology topology, String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getMetricsForTopology(CatalogToLayoutConverter.getTopologyLayout(topology), asUser);
    }

    public Map<Long, Double> getCompleteLatency(Topology topology, TopologyComponent component, long from, long to, String asUser) throws Exception {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getCompleteLatency(CatalogToLayoutConverter.getTopologyLayout(topology), CatalogToLayoutConverter.getComponentLayout(component), from, to, asUser);
    }

    public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getTopologyStats(Topology topology, Long from, Long to, String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getTopologyStats(CatalogToLayoutConverter.getTopologyLayout(topology), from, to, asUser);
    }

    public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getComponentStats(Topology topology, TopologyComponent component, Long from, Long to, String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getComponentStats(CatalogToLayoutConverter.getTopologyLayout(topology), CatalogToLayoutConverter.getComponentLayout(component), from, to, asUser);
    }

    public Map<String, Map<Long, Double>> getKafkaTopicOffsets(Topology topology, TopologyComponent component, Long from, Long to, String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getkafkaTopicOffsets(CatalogToLayoutConverter.getTopologyLayout(topology), CatalogToLayoutConverter.getComponentLayout(component), from, to, asUser);
    }

    public TopologyMetrics.TopologyMetric getTopologyMetric(Topology topology, String asUser) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getTopologyMetric(CatalogToLayoutConverter.getTopologyLayout(topology), asUser);
    }

    public List<Pair<String, Double>> getTopNAndOtherComponentsLatency(Topology topology, String asUser, int nOfTopN) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        Map<String, TopologyMetrics.ComponentMetric> metricsForTopology = topologyMetrics
                .getMetricsForTopology(CatalogToLayoutConverter.getTopologyLayout(topology), asUser);

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
