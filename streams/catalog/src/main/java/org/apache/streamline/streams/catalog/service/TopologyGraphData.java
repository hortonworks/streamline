package org.apache.streamline.streams.catalog.service;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

/**
 * Created by schendamaraikannan on 11/28/16.
 */
public class TopologyGraphData {
    private List<TopologyComponentGraphData> sources;
    private List<TopologyComponentGraphData> sinks;
    private List<TopologyComponentGraphData> processors;
    private GraphTransform graphTransforms;

    @JsonCreator
    public TopologyGraphData() {
    }

    public List<TopologyComponentGraphData> getSources() {
        return sources;
    }

    public void setSources(List<TopologyComponentGraphData> sources) {
        this.sources = sources;
    }

    public List<TopologyComponentGraphData> getSinks() {
        return sinks;
    }

    public void setSinks(List<TopologyComponentGraphData> sinks) {
        this.sinks = sinks;
    }

    public List<TopologyComponentGraphData> getProcessors() {
        return processors;
    }

    public void setProcessors(List<TopologyComponentGraphData> processors) {
        this.processors = processors;
    }

    public GraphTransform getGraphTransforms() {
        return graphTransforms;
    }

    public void setGraphTransforms(GraphTransform graphTranforms) {
        this.graphTransforms = graphTranforms;
    }
}
