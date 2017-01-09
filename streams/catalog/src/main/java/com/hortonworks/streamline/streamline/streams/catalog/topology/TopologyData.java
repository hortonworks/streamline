package com.hortonworks.streamline.streams.catalog.topology;

import java.util.HashMap;
import java.util.List;
import com.google.common.base.Preconditions;
import com.hortonworks.streamline.streams.catalog.BranchRuleInfo;
import com.hortonworks.streamline.streams.catalog.RuleInfo;
import com.hortonworks.streamline.streams.catalog.TopologyEditorMetadata;
import com.hortonworks.streamline.streams.catalog.TopologySource;
import com.hortonworks.streamline.streams.catalog.TopologyProcessor;
import com.hortonworks.streamline.streams.catalog.TopologyEdge;
import com.hortonworks.streamline.streams.catalog.TopologySink;
import com.hortonworks.streamline.streams.catalog.WindowInfo;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper class holding the various topology entities for export/import.
 */
public final class TopologyData {
    private String topologyName;
    private String config;
    private List<TopologySource> sources = new ArrayList<>();
    private List<TopologySink> sinks = new ArrayList<>();
    private List<TopologyProcessor> processors = new ArrayList<>();
    private List<TopologyEdge> edges = new ArrayList<>();
    private List<RuleInfo> rules = new ArrayList<>();
    private List<WindowInfo> windows = new ArrayList<>();
    private List<BranchRuleInfo> branchRules = new ArrayList<>();
    private Map<String, String> bundleIdToType = new HashMap<>();
    private TopologyEditorMetadata topologyEditorMetadata;

    public TopologyData() {
    }

    // copy ctor
    public TopologyData(TopologyData other) {
        topologyName = other.getTopologyName();
        config = other.getConfig();
        sources = other.sources.stream().map(TopologySource::new).collect(Collectors.toList());
        sinks = other.sinks.stream().map(TopologySink::new).collect(Collectors.toList());
        processors = other.processors.stream().map(TopologyProcessor::new).collect(Collectors.toList());
        edges = other.edges.stream().map(TopologyEdge::new).collect(Collectors.toList());
        rules = other.rules.stream().map(RuleInfo::new).collect(Collectors.toList());
        windows = other.windows.stream().map(WindowInfo::new).collect(Collectors.toList());
        branchRules = other.branchRules.stream().map(BranchRuleInfo::new).collect(Collectors.toList());
        bundleIdToType = new HashMap<>(other.getBundleIdToType());
        topologyEditorMetadata = new TopologyEditorMetadata(other.getTopologyEditorMetadata());
    }

    public String getTopologyName() {
        return topologyName;
    }

    public void setTopologyName(String name) {
        Preconditions.checkNotNull(name);

        this.topologyName = name;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        Preconditions.checkNotNull(config);

        this.config = config;
    }

    public List<TopologySource> getSources() {
        return sources;
    }

    public void addSource(TopologySource source) {
        Preconditions.checkNotNull(source);

        this.sources.add(source);
    }

    public List<TopologySink> getSinks() {
        return sinks;
    }

    public void addSink(TopologySink topologySink) {
        this.sinks.add(topologySink);
    }

    public List<TopologyEdge> getEdges() {
        return edges;
    }
    public void addEdge(TopologyEdge edge) {
        Preconditions.checkNotNull(edge);

        this.edges.add(edge);
    }

    public List<TopologyProcessor> getProcessors() {
        return processors;
    }

    public void addProcessor(TopologyProcessor topologyProcessor) {
        Preconditions.checkNotNull(topologyProcessor);

        this.processors.add(topologyProcessor);
    }

    public List<RuleInfo> getRules() {
        return rules;
    }

    public void addRule(RuleInfo ruleInfo) {
        Preconditions.checkNotNull(ruleInfo);

        this.rules.add(ruleInfo);
    }

    public List<WindowInfo> getWindows() {
        return windows;
    }

    public void addWindow(WindowInfo windowInfo) {
        Preconditions.checkNotNull(windowInfo);

        this.windows.add(windowInfo);
    }

    public List<BranchRuleInfo> getBranchRules() {
        return branchRules;
    }

    public void addBranch(BranchRuleInfo branchRuleInfo) {
        Preconditions.checkNotNull(branchRuleInfo);

        this.branchRules.add(branchRuleInfo);
    }

    public TopologyEditorMetadata getTopologyEditorMetadata() {
        return topologyEditorMetadata;
    }

    public void setMetadata(TopologyEditorMetadata metadata) {
        this.topologyEditorMetadata = metadata;
    }

    public Map<String, String> getBundleIdToType() {
        return bundleIdToType;
    }

    public void addBundleIdToType(String bundleId, String type) {
        bundleIdToType.put(bundleId, type);
    }
}
