package com.hortonworks.streamline.streams.sampling.service.mapping;

public enum MappedTopologySamplingImpl {
    STORM("com.hortonworks.streamline.streams.sampling.service.storm.StormTopologySamplingService");

    private final String className;

    MappedTopologySamplingImpl(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
