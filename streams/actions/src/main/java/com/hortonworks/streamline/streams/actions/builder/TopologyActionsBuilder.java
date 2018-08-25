package com.hortonworks.streamline.streams.actions.builder;

import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import javax.security.auth.Subject;
import java.util.Map;

public interface TopologyActionsBuilder<T> {

    /**
     * initialize the configs based on the namespace
     */
    void init(Map<String, String> conf, EnvironmentService environmentService, Namespace namespace, Subject subject);

    TopologyActions getTopologyActions();

    T getConfig();

    void cleanup();
}
