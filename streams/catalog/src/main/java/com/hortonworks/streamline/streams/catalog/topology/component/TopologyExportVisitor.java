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
package com.hortonworks.streamline.streams.catalog.topology.component;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.hortonworks.streamline.common.ComponentTypes;
import com.hortonworks.streamline.streams.catalog.TopologyBranchRule;
import com.hortonworks.streamline.streams.catalog.TopologyRule;
import com.hortonworks.streamline.streams.catalog.TopologyEdge;
import com.hortonworks.streamline.streams.catalog.TopologyProcessor;
import com.hortonworks.streamline.streams.catalog.TopologySink;
import com.hortonworks.streamline.streams.catalog.TopologySource;
import com.hortonworks.streamline.streams.catalog.TopologyWindow;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.catalog.topology.TopologyComponentBundle;
import com.hortonworks.streamline.streams.catalog.topology.TopologyData;
import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.StreamlineComponent;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;

import java.util.Map;

/**
 * Visitor that fills the topology entities into a {@link TopologyData} object for exporting the topology
 */
public final class TopologyExportVisitor extends TopologyDagVisitor {
    private final Long topologyId;
    private final TopologyData topologyData;
    private final StreamCatalogService streamCatalogService;
    private final Map<String, RulesHandler> rulesHandlerMap;

    public TopologyExportVisitor(Long topologyId, TopologyData topologyData, StreamCatalogService streamCatalogService) {
        this.topologyId = topologyId;
        this.topologyData = topologyData;
        this.streamCatalogService = streamCatalogService;
        rulesHandlerMap = createRuleHandlerMap();
    }

    private interface RulesHandler {
        void handle(Rule rule) throws Exception;
    }

    private Map<String, RulesHandler> createRuleHandlerMap() {
        ImmutableMap.Builder<String, RulesHandler> rulesHandlerBuilder = ImmutableMap.builder();

        rulesHandlerBuilder.put(ComponentTypes.RULE, new RulesHandler() {
            @Override
            public void handle(Rule rule) throws Exception {
                TopologyRule topologyRule = streamCatalogService.getRule(topologyId, rule.getId());
                topologyData.addRule(topologyRule);
            }
        });

        rulesHandlerBuilder.put(ComponentTypes.BRANCH, new RulesHandler() {
            @Override
            public void handle(Rule rule) throws Exception {
                TopologyBranchRule topologyBranchRule = streamCatalogService.getBranchRule(topologyId, rule.getId());
                topologyData.addBranch(topologyBranchRule);
            }
        });

        rulesHandlerBuilder.put(ComponentTypes.WINDOW, new RulesHandler() {
            @Override
            public void handle(Rule rule) throws Exception {
                TopologyWindow topologyWindow = streamCatalogService.getWindow(topologyId, rule.getId());
                topologyData.addWindow(topologyWindow);
            }
        });
        return rulesHandlerBuilder.build();
    }
    public void visit(RulesProcessor rulesProcessor) {
        try {
            TopologyComponentBundle componentBundle = streamCatalogService.getTopologyComponentBundle(
                    Long.parseLong(rulesProcessor.getTopologyComponentBundleId()));
            String subType = componentBundle.getSubType();
            RulesHandler rulesHandler = rulesHandlerMap.get(subType);
            for (Rule rule : rulesProcessor.getRules()) {
                rulesHandler.handle(rule);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Unexpected exception thrown while trying to add the rule %s",
                            rulesProcessor.getId()),
                    e);
        }

        TopologyProcessor topologyProcessor = streamCatalogService.getTopologyProcessor(
                topologyId,
                Long.parseLong(rulesProcessor.getId()));
        topologyData.addProcessor(topologyProcessor);
        storeBundleIdToType(rulesProcessor);
    }

    public void visit(StreamlineSource source) {
        TopologySource topologySource = streamCatalogService.getTopologySource(
                topologyId,
                Long.parseLong(source.getId()));
        topologyData.addSource(topologySource);
        storeBundleIdToType(source);
    }

    public void visit(StreamlineSink sink) {
        TopologySink topologySink = streamCatalogService.getTopologySink(
                topologyId,
                Long.parseLong(sink.getId()));

        topologyData.addSink(topologySink);
        storeBundleIdToType(sink);
    }

    public void visit(StreamlineProcessor processor) {
        TopologyProcessor topologyProcessor = streamCatalogService.getTopologyProcessor(
                topologyId,
                Long.parseLong(processor.getId()));
        topologyData.addProcessor(topologyProcessor);
        storeBundleIdToType(processor);
    }


    public void visit(Edge edge) {
        TopologyEdge topologyEdge = streamCatalogService.getTopologyEdge(topologyId, Long.parseLong(edge.getId()));
        topologyData.addEdge(topologyEdge);
    }

    private void storeBundleIdToType(StreamlineComponent component) {
        TopologyComponentBundle bundle = streamCatalogService.getTopologyComponentBundle(
                Long.parseLong(component.getTopologyComponentBundleId()));
        Preconditions.checkNotNull(bundle, "No bundle with id: " + component.getTopologyComponentBundleId());
        topologyData.addBundleIdToType(component.getTopologyComponentBundleId(), bundle.getSubType());
    }
}
