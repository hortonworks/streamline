package org.apache.streamline.streams.catalog.container.mapping;

public enum MappedTopologyActionsImpl {
    STORM("org.apache.streamline.streams.layout.storm.StormTopologyActionsImpl");

    private final String className;

    MappedTopologyActionsImpl(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
