package com.hortonworks.streamline.streams.cluster.bundle.impl;

import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.bundle.AbstractSecureBundleHintProvider;
import com.hortonworks.streamline.streams.cluster.service.metadata.StormMetadataService;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Security;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;

public abstract class StormBundleHintProvider extends AbstractSecureBundleHintProvider {
    public static final String FIELD_NAME_CLUSTER_NAME = "clusterName";
    public static final String FIELD_NAME_PRINCIPAL = "principal";
    public static final String FIELD_NAME_KEYTAB_PATH = "keytabPath";

    @Override
    public Security getSecurity(Cluster cluster, SecurityContext securityContext, Subject subject) {
        try {
            StormMetadataService sms = new StormMetadataService
                    .Builder(environmentService, cluster.getId(), securityContext, subject).build();
            return sms.getSecurity();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster, SecurityContext securityContext, Subject subject) {
        Map<String, Object> hintClusterMap = new HashMap<>();
        hintClusterMap.put(FIELD_NAME_CLUSTER_NAME, cluster.getName());

        final Security security = getSecurity(cluster, securityContext, subject);
        hintClusterMap.put(FIELD_NAME_PRINCIPAL, security.getPrincipals());
        hintClusterMap.put(FIELD_NAME_KEYTAB_PATH, security.getKeytabs());

        return hintClusterMap;
    }

    @Override
    public String getServiceName() {
        return Constants.Storm.SERVICE_NAME;
    }
}
