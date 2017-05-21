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
package com.hortonworks.streamline.streams.cluster.bundle.impl;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.bundle.AbstractKafkaBundleHintProvider;

import java.util.Map;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;

public class KafkaBundleHintProvider extends AbstractKafkaBundleHintProvider {
    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster, SecurityContext securityContext, Subject subject) {
        return super.getHintsOnCluster(cluster, securityContext, subject);
    }

    @Override
    public String getServiceName() {
        return super.getServiceName();
    }
}
