package com.hortonworks.streamline.streams.actions.container.mapping;

public enum MappedTopologyActionsImpl {
    STORM("com.hortonworks.streamline.streams.actions.storm.topology.StormTopologyActionsImpl");

    private final String className;

    MappedTopologyActionsImpl(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
