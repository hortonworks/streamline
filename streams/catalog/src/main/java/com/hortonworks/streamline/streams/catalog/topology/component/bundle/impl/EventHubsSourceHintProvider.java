package com.hortonworks.streamline.streams.catalog.topology.component.bundle.impl;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.topology.component.bundle.AbstractBundleHintProvider;

import java.util.Collections;
import java.util.Map;

public class EventHubsSourceHintProvider extends AbstractBundleHintProvider {
    public static final String SERVICE_NAME = "EVENTHUBS";

    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster) {
        return Collections.emptyMap();
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
}
