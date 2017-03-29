package com.hortonworks.streamline.streams.cluster.bundle.impl;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.bundle.AbstractBundleHintProvider;

import java.util.Collections;
import java.util.Map;

public class EventHubsSourceHintProvider extends AbstractBundleHintProvider {
    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster) {
        return Collections.emptyMap();
    }

    @Override
    public String getServiceName() {
        return Constants.EventHubs.SERVICE_NAME;
    }
}
