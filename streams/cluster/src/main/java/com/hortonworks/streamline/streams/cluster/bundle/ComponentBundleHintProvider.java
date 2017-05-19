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

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;
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
     * @param environmentService {@link com.hortonworks.streamline.streams.cluster.service.EnvironmentService}
     */
    void init(EnvironmentService environmentService);

    /**
     * Provide hints on specific component bundle with selected namespace.
     *
     * @param namespace selected namespace
     * @param securityContext
     * @param subject
     * @return Hint structures. The structure of map should be cluster id -> (cluster, hints).
     */
    Map<Long, BundleHintsResponse> provide(Namespace namespace, SecurityContext securityContext, Subject subject);
}