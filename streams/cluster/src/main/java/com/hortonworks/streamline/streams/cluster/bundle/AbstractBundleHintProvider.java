/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.cluster.bundle;

import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.NamespaceServiceClusterMap;
import com.hortonworks.streamline.streams.cluster.exception.ClusterNotFoundException;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;

public abstract class AbstractBundleHintProvider implements ComponentBundleHintProvider {

    protected EnvironmentService environmentService;


    @Override
    public void init(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

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

            BundleHintsResponse response = new BundleHintsResponse(cluster, getHintsOnCluster(cluster, securityContext, subject));
            hintMap.put(clusterId, response);
        }

        return hintMap;
    }

    public abstract Map<String, Object> getHintsOnCluster(Cluster cluster, SecurityContext securityContext, Subject subject);

    public abstract String getServiceName();
}
