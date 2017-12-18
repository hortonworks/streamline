package com.hortonworks.streamline.streams.sampling.service;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import javax.security.auth.Subject;
import java.util.List;

public class TopologySamplingService {
    private final EnvironmentService environmentService;
    private final TopologySamplingContainer topologySamplingContainer;


    public TopologySamplingService(EnvironmentService environmentService, Subject subject) {
        this.environmentService = environmentService;
        this.topologySamplingContainer = new TopologySamplingContainer(environmentService, subject);
    }

    public boolean enableSampling(Topology topology, int pct, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.enableSampling(topology, pct, asUser);
    }

    public boolean enableSampling(Topology topology, TopologyComponent component, int pct, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.enableSampling(topology, component, pct, asUser);
    }

    public boolean disableSampling(Topology topology, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.disableSampling(topology, asUser);
    }

    public boolean disableSampling(Topology topology, TopologyComponent component, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.disableSampling(topology, component, asUser);
    }

    public TopologySampling.SamplingStatus samplingStatus(Topology topology, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.getSamplingStatus(topology, asUser);
    }

    public TopologySampling.SamplingStatus samplingStatus(Topology topology, TopologyComponent component, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.getSamplingStatus(topology, component, asUser);
    }

    public TopologySampling.SampledEvents getSampledEvents(Topology topology, TopologyComponent component, TopologySampling.EventQueryParams qps, String asUser) {
        TopologySampling sampling = getSamplingInstance(topology);
        return sampling.getSampledEvents(topology, component, qps, asUser);
    }

    private TopologySampling getSamplingInstance(Topology topology) {
        Namespace namespace = environmentService.getNamespace(topology.getNamespaceId());
        if (namespace == null) {
            throw new RuntimeException("Corresponding namespace not found: " + topology.getNamespaceId());
        }

        TopologySampling topologySampling = topologySamplingContainer.findInstance(namespace);
        if (topologySampling == null) {
            throw new RuntimeException("Can't find Topology sampling for such namespace " + topology.getNamespaceId());
        }
        return topologySampling;
    }
}
