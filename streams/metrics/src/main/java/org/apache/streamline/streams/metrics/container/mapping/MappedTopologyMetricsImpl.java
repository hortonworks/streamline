package org.apache.streamline.streams.metrics.container.mapping;

public enum MappedTopologyMetricsImpl {
    STORM("org.apache.streamline.streams.metrics.storm.topology.StormTopologyMetricsImpl");

    private final String className;

    MappedTopologyMetricsImpl(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
