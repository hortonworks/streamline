package com.hortonworks.streamline.streams.actions.builder.mapping;

public enum MappedTopologyActionsBuilder {
    STORM("com.hortonworks.streamline.streams.actions.storm.topology.StormTopologyActionsBuilder");

    private final String className;

    MappedTopologyActionsBuilder(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
