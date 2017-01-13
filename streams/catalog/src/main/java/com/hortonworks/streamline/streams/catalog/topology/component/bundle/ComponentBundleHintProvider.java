package com.hortonworks.streamline.streams.catalog.topology.component.bundle;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.service.EnvironmentService;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;

import java.util.Map;

/**
 * This interface defines the way to provide hints on specific component bundle.
 *
 * For example, KAFKA component bundle has mandatory fields "zkUrl", "topic". If user selects namespace 'env1' while
 * creating topology and places KAFKA component bundle to topology, we can get such information from KAFKA service(s)
 * mapped to namespace 'env1' and provide that values to be used as hint.
 */
public interface ComponentBundleHintProvider {
    class BundleHintsResponse {
        private Cluster cluster;
        private Map<String, Object> hints;

        public BundleHintsResponse(Cluster cluster, Map<String, Object> hints) {
            this.cluster = cluster;
            this.hints = hints;
        }

        public Cluster getCluster() {
            return cluster;
        }

        public Map<String, Object> getHints() {
            return hints;
        }
    }

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
     * @return Hint structures. The structure of map should be cluster id -> (cluster, hints).
     */
    Map<Long, BundleHintsResponse> provide(Namespace namespace);
}