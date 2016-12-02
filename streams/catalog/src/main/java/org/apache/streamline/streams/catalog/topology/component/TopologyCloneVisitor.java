package org.apache.streamline.streams.catalog.topology.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.streamline.common.ComponentTypes;

import org.apache.streamline.common.Config;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.streams.catalog.*;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.catalog.topology.TopologyComponentBundle;
import org.apache.streamline.streams.layout.component.*;

import org.apache.streamline.streams.layout.component.rule.Rule;
import org.apache.streamline.streams.layout.component.impl.RulesProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by schendamaraikannan on 11/2/16.
 */
public final class TopologyCloneVisitor extends TopologyDagVisitor {
    private final Long topologyId;
    private final Long clonedTopologyId;
    private final StreamCatalogService streamCatalogService;
    private final Map<String, RulesHandler> rulesHandlerMap;

    private Map<Long, Long> ruleIdMapping = new HashMap<>();
    private Map<Long, Long> windowIdMapping = new HashMap<>();
    private Map<Long, Long> branchRuleIdMapping = new HashMap<>();
    private Map<Long, Long> idMapping;

    public TopologyCloneVisitor(Long topologyId, Long clonedTopologyId, StreamCatalogService streamCatalogService) {
        this.topologyId = topologyId;
        this.clonedTopologyId = clonedTopologyId;
        this.streamCatalogService = streamCatalogService;
        rulesHandlerMap = createRuleHandlerMap();
        this.idMapping = new HashMap<>();
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
                RuleInfo clonedRuleInfo = new RuleInfo(ruleInfo);
                clonedRuleInfo.setId(null);
                streamCatalogService.addRule(clonedTopologyId, clonedRuleInfo);
                ruleIdMapping.put(ruleInfo.getId(), clonedRuleInfo.getId());
            }
        });

        rulesHandlerBuilder.put(ComponentTypes.BRANCH, new RulesHandler() {
            @Override
            public void handle(Rule rule) throws Exception {
                BranchRuleInfo branchRuleInfo = streamCatalogService.getBranchRule(topologyId, rule.getId());
                BranchRuleInfo clonedBranchRuleInfo = new BranchRuleInfo(branchRuleInfo);
                branchRuleInfo.setId(null);
                streamCatalogService.addBranchRule(clonedTopologyId, clonedBranchRuleInfo);
                branchRuleIdMapping.put(branchRuleInfo.getId(), clonedBranchRuleInfo.getId());
            }
        });

        rulesHandlerBuilder.put(ComponentTypes.WINDOW, new RulesHandler() {
            @Override
            public void handle(Rule rule) throws Exception {
                WindowInfo windowInfo = streamCatalogService.getWindow(topologyId, rule.getId());
                WindowInfo clonedWindowInfo = new WindowInfo(windowInfo);
                clonedWindowInfo.setId(null);
                streamCatalogService.addWindow(clonedTopologyId, clonedWindowInfo);
                windowIdMapping.put(windowInfo.getId(), clonedWindowInfo.getId());
            }
        });
        return rulesHandlerBuilder.build();
    }

    public void visit(RulesProcessor rulesProcessor) {
        TopologyProcessor topologyProcessor = streamCatalogService.getTopologyProcessor(
                topologyId,
                Long.parseLong(rulesProcessor.getId()));
        TopologyProcessor clonedTopologyProcessor = new TopologyProcessor(topologyProcessor);
        clonedTopologyProcessor.setId(null);
        List<StreamInfo> outputStreams = topologyProcessor.getOutputStreams();
        List<StreamInfo> clonedOutputStreams = new ArrayList<>();
        for (StreamInfo streamInfo : outputStreams) {
            StreamInfo newStreamInfo = new StreamInfo(streamInfo);
            newStreamInfo.setId(null);
            streamCatalogService.addStreamInfo(clonedTopologyId, newStreamInfo);
            clonedOutputStreams.add(newStreamInfo);
        }
        clonedTopologyProcessor.setOutputStreams(clonedOutputStreams);

        try {
            TopologyComponentBundle componentBundle = streamCatalogService.getTopologyComponentBundle(
                    Long.parseLong(rulesProcessor.getTopologyComponentBundleId()));
            String subType = componentBundle.getSubType();
            RulesHandler rulesHandler = rulesHandlerMap.get(subType);
            for (Rule rule : rulesProcessor.getRules()) {
                rulesHandler.handle(rule);
            }
            Config config = topologyProcessor.getConfig();
            List<Long> ruleIds = new ObjectMapper().convertValue(
                    config.getAny(RulesProcessor.CONFIG_KEY_RULES),
                    new TypeReference<List<Long>>() {
                    });
            TopologyComponentBundle topologyComponentBundle = streamCatalogService.getTopologyComponentBundle(
                    topologyProcessor.getTopologyComponentBundleId());

            List<Long> updatedRuleIds = new ArrayList<>();
            String type = topologyComponentBundle.getSubType();
            if (type.equals(ComponentTypes.RULE)) {
                for (Long ruleId : ruleIds) {
                    updatedRuleIds.add(ruleIdMapping.get(ruleId));
                }
            } else if (type.equals(ComponentTypes.BRANCH)) {
                for (Long ruleId : ruleIds) {
                    updatedRuleIds.add(branchRuleIdMapping.get(ruleId));
                }

            } else if (type.equals(ComponentTypes.WINDOW)) {
                for (Long ruleId : ruleIds) {
                    updatedRuleIds.add(windowIdMapping.get(ruleId));
                }
            }
            config.setAny(RulesProcessor.CONFIG_KEY_RULES, updatedRuleIds);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Unexpected exception thrown while trying to add the rule %d",
                            rulesProcessor.getId()),
                    e);
        }


        streamCatalogService.addTopologyProcessor(clonedTopologyId, clonedTopologyProcessor);
        idMapping.put(topologyProcessor.getId(), clonedTopologyProcessor.getId());
    }

    public void visit(StreamlineSource source) {
        TopologySource topologySource = streamCatalogService.getTopologySource(
                topologyId,
                Long.parseLong(source.getId()));
        TopologySource clonedTopologySource = new TopologySource(topologySource);
        clonedTopologySource.setId(null);

        streamCatalogService.addTopologySource(clonedTopologyId, clonedTopologySource);
        idMapping.put(topologySource.getId(), clonedTopologySource.getId());
    }

    public void visit(StreamlineSink sink) {
        TopologySink topologySink = streamCatalogService.getTopologySink(
                topologyId,
                Long.parseLong(sink.getId()));
        TopologySink clonedTopologySink = new TopologySink(topologySink);
        clonedTopologySink.setId(null);

        streamCatalogService.addTopologySink(clonedTopologyId, clonedTopologySink);
        idMapping.put(topologySink.getId(), clonedTopologySink.getId());
    }

    public void visit(StreamlineProcessor processor) {
        TopologyProcessor topologyProcessor = streamCatalogService.getTopologyProcessor(
                topologyId,
                Long.parseLong(processor.getId()));
        TopologyProcessor clonedTopologyProcessor = new TopologyProcessor(topologyProcessor);
        clonedTopologyProcessor.setId(null);

        streamCatalogService.addTopologyProcessor(clonedTopologyId, clonedTopologyProcessor);
        idMapping.put(topologyProcessor.getId(), clonedTopologyProcessor.getId());
    }

    public void visit(Edge edge) {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam("fromId", edge.getFrom().getId()));
        queryParams.add(new QueryParam("toId", edge.getTo().getId()));

        try {
            Collection<TopologyEdge> topologyEdges = streamCatalogService.listTopologyEdges(queryParams);
            /*
                The topology edge collection is guaranteed to be unique because we do a duplicate check
                when adding an edge.
             */
            TopologyEdge topologyEdge = topologyEdges.iterator().next();
            TopologyEdge clonedTopologyEdge = new TopologyEdge(topologyEdges.iterator().next());
            clonedTopologyEdge.setFromId(idMapping.get(topologyEdge.getFromId()));
            clonedTopologyEdge.setToId(idMapping.get(topologyEdge.getToId()));
            streamCatalogService.addTopologyEdge(clonedTopologyId, clonedTopologyEdge);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "Unexpected exception while trying to retrieve topology edge from %s to %s",
                            edge.getFrom().getId(),
                            edge.getTo().getId()), e);
        }
    }

    public Map<Long, Long> getIdMapping() {
        return new HashMap<>(idMapping);
    }
}
