package com.hortonworks.streamline.streams.cluster.bundle;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.NamespaceServiceClusterMap;
import com.hortonworks.streamline.streams.catalog.exception.ClusterNotFoundException;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Security;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;

public abstract class AbstractSecureBundleHintProvider extends AbstractBundleHintProvider {

    @Override
    public Map<Long, BundleHintsResponse> provide(Namespace namespace, SecurityContext securityContext, Subject subject) {

        Map<Long, BundleHintsResponse> hintMap = new HashMap<>();
        Collection<NamespaceServiceClusterMap> serviceMappings = environmentService.listServiceClusterMapping(
                namespace.getId(), getServiceName());
        for (NamespaceServiceClusterMap mapping : serviceMappings) {
            Long clusterId = mapping.getClusterId();
            Cluster cluster = environmentService.getCluster(clusterId);
            if (cluster == null) {
                throw new RuntimeException(new ClusterNotFoundException(clusterId));
            }

            BundleHintsResponse response = new SecureBundleHintsResponse(
                    cluster, getSecurity(cluster, securityContext, subject), getHintsOnCluster(cluster, securityContext, subject));

            hintMap.put(clusterId, response);
        }

        return hintMap;
    }

    public abstract Security getSecurity(Cluster cluster, SecurityContext securityContext, Subject subject);
}
