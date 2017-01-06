package com.hortonworks.streamline.streams.catalog.topology.component.bundle;

import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.service.EnvironmentService;

import java.util.Map;

/**
 * This interface defines the way to provide hints on specific component bundle.
 *
 * For example, KAFKA component bundle has mandatory fields "zkUrl", "topic". If user selects namespace 'env1' while
 * creating topology and places KAFKA component bundle to topology, we can get such information from KAFKA service(s)
 * mapped to namespace 'env1' and provide that values to be used as hint.
 */
public interface ComponentBundleHintProvider {
    /**
     * Initialize provider.
     *
     * @param environmentService {@link com.hortonworks.streamline.streams.catalog.service.EnvironmentService}
     */
    void init(EnvironmentService environmentService);

    /**
     * Provide hints on specific component bundle with selected namespace.
     *
     * @param namespace selected namespace
     * @return Hint structures. The structure of map should be cluster name -> field name -> values.
     */
    Map<String, Map<String, Object>> provide(Namespace namespace);
}