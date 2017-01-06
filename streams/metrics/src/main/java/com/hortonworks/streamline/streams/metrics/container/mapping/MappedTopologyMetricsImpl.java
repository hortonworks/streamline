package com.hortonworks.streamline.streams.metrics.container.mapping;

public enum MappedTopologyMetricsImpl {
    STORM("com.hortonworks.streamline.streams.metrics.storm.topology.StormTopologyMetricsImpl");

    private final String className;

    MappedTopologyMetricsImpl(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
