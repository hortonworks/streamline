package com.hortonworks.streamline.streams.sampling.service;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TopologySampling {
    void init(Map<String, Object> conf);

    boolean enableSampling(Topology topology, int pct, String asUser);

    boolean enableSampling(Topology topology, TopologyComponent component, int pct, String asUser);

    boolean disableSampling(Topology topology, String asUser);

    boolean disableSampling(Topology topology, TopologyComponent component, String asUser);

    SamplingStatus getSamplingStatus(Topology topology, String asUser);

    SamplingStatus getSamplingStatus(Topology topology, TopologyComponent component, String asUser);

    SampledEvents getSampledEvents(Topology topology, TopologyComponent component, EventQueryParams qps, String asUser);

    interface SamplingStatus {
        Boolean getEnabled();
        Integer getPct();
    }

    interface SampledEvents {
        Collection<SampledEvent> getEvents();
        Long getNext();
        Integer getLength();
    }

    interface SampledEvent {
        long getTime();
        String getEvent();
        int getStartOffset();
        int getLength();
    }

    interface EventQueryParams {
        Long start();
        Integer length();
        int count();
        boolean desc();
    }
}
