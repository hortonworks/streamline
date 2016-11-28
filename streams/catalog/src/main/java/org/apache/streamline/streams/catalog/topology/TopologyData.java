package org.apache.streamline.streams.catalog.topology;

import java.util.List;
import com.google.common.base.Preconditions;
import org.apache.streamline.streams.catalog.BranchRuleInfo;
import org.apache.streamline.streams.catalog.RuleInfo;
import org.apache.streamline.streams.catalog.TopologyEditorMetadata;
import org.apache.streamline.streams.catalog.TopologySource;
import org.apache.streamline.streams.catalog.TopologyProcessor;
import org.apache.streamline.streams.catalog.TopologyEdge;
import org.apache.streamline.streams.catalog.TopologySink;
import org.apache.streamline.streams.catalog.WindowInfo;

import java.util.ArrayList;

/**
 * Created by schendamaraikannan on 11/1/16.
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
    private TopologyEditorMetadata topologyEditorMetadata;

    public String getTopologyName() {
        return topologyName;
    }

    public void setName(String name) {
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
}
