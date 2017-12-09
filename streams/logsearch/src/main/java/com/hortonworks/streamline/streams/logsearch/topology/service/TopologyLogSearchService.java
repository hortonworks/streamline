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
package com.hortonworks.streamline.streams.logsearch.topology.service;

import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.container.ContainingNamespaceAwareContainer;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.logsearch.LogSearchCriteria;
import com.hortonworks.streamline.streams.logsearch.LogSearchResult;
import com.hortonworks.streamline.streams.logsearch.TopologyLogSearch;
import com.hortonworks.streamline.streams.logsearch.container.TopologyLogSearchContainer;

import javax.security.auth.Subject;
import java.util.List;

public class TopologyLogSearchService implements ContainingNamespaceAwareContainer {
  private final EnvironmentService environmentService;
  private final TopologyLogSearchContainer topologyLogSearchContainer;

  public TopologyLogSearchService(EnvironmentService environmentService, Subject subject) {
    this.environmentService = environmentService;
    this.topologyLogSearchContainer = new TopologyLogSearchContainer(environmentService, subject);
  }

  public LogSearchResult search(Topology topology, LogSearchCriteria criteria) {
    TopologyLogSearch topologyLogSearch = getTopologyLogSearchInstance(topology);
    return topologyLogSearch.search(criteria);
  }

  @Override
  public void invalidateInstance(Long namespaceId) {
    try {
      topologyLogSearchContainer.invalidateInstance(namespaceId);
    } catch (Throwable e) {
      // swallow
    }
  }

  private TopologyLogSearch getTopologyLogSearchInstance(Topology topology) {
    Namespace namespace = environmentService.getNamespace(topology.getNamespaceId());
    if (namespace == null) {
      throw new RuntimeException("Corresponding namespace not found: " + topology.getNamespaceId());
    }

    TopologyLogSearch topologyLogSearch = topologyLogSearchContainer.findInstance(namespace);
    if (topologyLogSearch == null) {
      throw new RuntimeException("Can't find Topology Log Search for such namespace " + topology.getNamespaceId());
    }
    return topologyLogSearch;
  }
}
