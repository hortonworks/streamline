package org.apache.streamline.streams.catalog.topology.component;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.apache.streamline.common.ComponentTypes;
import org.apache.streamline.streams.catalog.BranchRuleInfo;
import org.apache.streamline.streams.catalog.RuleInfo;
import org.apache.streamline.streams.catalog.TopologyEdge;
import org.apache.streamline.streams.catalog.TopologyProcessor;
import org.apache.streamline.streams.catalog.TopologySink;
import org.apache.streamline.streams.catalog.TopologySource;
import org.apache.streamline.streams.catalog.WindowInfo;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.catalog.topology.TopologyComponentBundle;
import org.apache.streamline.streams.catalog.topology.TopologyData;
import org.apache.streamline.streams.layout.component.Edge;
import org.apache.streamline.streams.layout.component.StreamlineComponent;
import org.apache.streamline.streams.layout.component.StreamlineProcessor;
import org.apache.streamline.streams.layout.component.StreamlineSink;
import org.apache.streamline.streams.layout.component.StreamlineSource;
import org.apache.streamline.streams.layout.component.TopologyDagVisitor;
import org.apache.streamline.streams.layout.component.impl.RulesProcessor;
import org.apache.streamline.streams.layout.component.rule.Rule;

import java.util.HashMap;
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
                RuleInfo ruleInfo = streamCatalogService.getRule(topologyId, rule.getId());
                topologyData.addRule(ruleInfo);
            }
        });

        rulesHandlerBuilder.put(ComponentTypes.BRANCH, new RulesHandler() {
            @Override
            public void handle(Rule rule) throws Exception {
                BranchRuleInfo branchRuleInfo = streamCatalogService.getBranchRule(topologyId, rule.getId());
                topologyData.addBranch(branchRuleInfo);
            }
        });

        rulesHandlerBuilder.put(ComponentTypes.WINDOW, new RulesHandler() {
            @Override
            public void handle(Rule rule) throws Exception {
                WindowInfo windowInfo = streamCatalogService.getWindow(topologyId, rule.getId());
                topologyData.addWindow(windowInfo);
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
                    String.format("Unexpected exception thrown while trying to add the rule %d",
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
