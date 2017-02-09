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
package com.hortonworks.streamline.streams.actions.topology.service;

import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.catalog.topology.TopologyComponentBundle;
import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.StreamlineComponent;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;


public class TopologyComponentBundleJarHandler extends TopologyDagVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyComponentBundleJarHandler.class);
    private final StreamCatalogService streamCatalogService;
    private Set<TopologyComponentBundle> topologyComponentBundleSet = new HashSet<>();
    TopologyComponentBundleJarHandler(StreamCatalogService streamCatalogService) {
        this.streamCatalogService = streamCatalogService;
    }

    @Override
    public void visit (RulesProcessor rulesProcessor) {
        handleIotasComponentBundle(rulesProcessor);
    }

    @Override
    public void visit (StreamlineSource streamlineSource) {
        handleIotasComponentBundle(streamlineSource);
    }

    @Override
    public void visit (StreamlineSink streamlineSink) {
        handleIotasComponentBundle(streamlineSink);
    }

    @Override
    public void visit (StreamlineProcessor streamlineProcessor) {
        handleIotasComponentBundle(streamlineProcessor);
    }

    @Override
    public void visit (Edge edge) {

    }

    private void handleIotasComponentBundle (StreamlineComponent streamlineComponent) {
        TopologyComponentBundle topologyComponentBundle = streamCatalogService.getTopologyComponentBundle(Long.parseLong(streamlineComponent.
                getTopologyComponentBundleId()));
        if (topologyComponentBundle == null) {
            throw new RuntimeException("Likely to run in to issues while deployging topology since TopologyComponentBundle not found for id " +
                    streamlineComponent.getTopologyComponentBundleId());
        }
        if (!topologyComponentBundle.getBuiltin()) {
            topologyComponentBundleSet.add(topologyComponentBundle);
        } else {
            LOG.debug("No need to copy any jar for {} since its a builtin bundle", streamlineComponent);
        }
    }

    public Set<TopologyComponentBundle>  getTopologyComponentBundleSet () {
        return topologyComponentBundleSet;
    }

}
