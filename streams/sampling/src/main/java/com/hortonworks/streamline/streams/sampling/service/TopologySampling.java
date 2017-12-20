package com.hortonworks.streamline.streams.sampling.service;

import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;

import java.util.Map;

public interface TopologySampling {
    void init(Map<String, Object> conf);

    boolean enableSampling(Topology topology, int pct, String asUser);

    boolean enableSampling(Topology topology, TopologyComponent component, int pct, String asUser);

    boolean disableSampling(Topology topology, String asUser);

    boolean disableSampling(Topology topology, TopologyComponent component, String asUser);

    SamplingStatus getSamplingStatus(Topology topology, String asUser);

    SamplingStatus getSamplingStatus(Topology topology, TopologyComponent component, String asUser);

    interface SamplingStatus {
        Boolean getEnabled();
        Integer getPct();
    }
}
