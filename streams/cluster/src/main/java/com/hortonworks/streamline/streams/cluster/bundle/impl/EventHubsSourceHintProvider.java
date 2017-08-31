package com.hortonworks.streamline.streams.cluster.bundle.impl;

import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.bundle.AbstractBundleHintProvider;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;
import java.util.Collections;
import java.util.Map;

public class EventHubsSourceHintProvider extends AbstractBundleHintProvider {
    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster, SecurityContext securityContext, Subject subject) {
        return Collections.emptyMap();
    }

    @Override
    public String getServiceName() {
        return Constants.EventHubs.SERVICE_NAME;
    }
}
