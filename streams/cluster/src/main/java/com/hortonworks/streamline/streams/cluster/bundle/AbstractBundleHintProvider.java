package com.hortonworks.streamline.streams.cluster.bundle;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.NamespaceServiceClusterMapping;
import com.hortonworks.streamline.streams.catalog.exception.ClusterNotFoundException;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractBundleHintProvider implements ComponentBundleHintProvider {

    protected EnvironmentService environmentService;


    @Override
    public void init(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public Map<Long, BundleHintsResponse> provide(Namespace namespace) {
        Map<Long, BundleHintsResponse> hintMap = new HashMap<>();

        Collection<NamespaceServiceClusterMapping> serviceMappings = environmentService.listServiceClusterMapping(
                namespace.getId(), getServiceName());
        for (NamespaceServiceClusterMapping mapping : serviceMappings) {
            Long clusterId = mapping.getClusterId();
            Cluster cluster = environmentService.getCluster(clusterId);
            if (cluster == null) {
                throw new RuntimeException(new ClusterNotFoundException(clusterId));
            }

            BundleHintsResponse response = new BundleHintsResponse(cluster, getHintsOnCluster(cluster));
            hintMap.put(clusterId, response);
        }

        return hintMap;
    }

    public abstract Map<String, Object> getHintsOnCluster(Cluster cluster);

    public abstract String getServiceName();
}
