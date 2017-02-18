package com.hortonworks.streamline.streams.actions.storm.topology;

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.InputComponent;
import com.hortonworks.streamline.streams.layout.component.OutputComponent;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import com.hortonworks.streamline.streams.layout.storm.TestRunSinkBoltFluxComponent;
import com.hortonworks.streamline.streams.layout.storm.TestRunSourceSpoutFluxComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestTopologyDagCreatingVisitor extends TopologyDagVisitor {

    private final Map<String, StreamlineSource> sourceToReplacedTestSourceMap;
    private final Map<String, StreamlineSink> sinkToReplacedTestSinkMap;
    private final Map<String, TestRunSource> testRunSourcesMap;
    private final Map<String, TestRunSink> testRunSinksMap;
    private final TopologyDag originTopologyDag;

    private TopologyDag testTopologyDag;

    public TestTopologyDagCreatingVisitor(TopologyDag originTopologyDag,
                                          Map<String, TestRunSource> testRunSourcesMap,
                                          Map<String, TestRunSink> testRunSinksMap) {
        this.originTopologyDag = originTopologyDag;
        this.testRunSourcesMap = testRunSourcesMap;
        this.testRunSinksMap = testRunSinksMap;

        this.testTopologyDag = new TopologyDag();
        this.sourceToReplacedTestSourceMap = new HashMap<>();
        this.sinkToReplacedTestSinkMap = new HashMap<>();
    }

    @Override
    public void visit(RulesProcessor rulesProcessor) {
        visit((StreamlineProcessor) rulesProcessor);
    }

    @Override
    public void visit(StreamlineSource source) {
        String id = source.getId();
        String sourceName = source.getName();

        if (!testRunSourcesMap.containsKey(sourceName)) {
            throw new IllegalStateException("Not all sources have corresponding TestRunSource instance. source name: " + sourceName);
        }

        Config config = new Config(source.getConfig());

        TestRunSource testRunSource = testRunSourcesMap.get(sourceName);

        testRunSource.setId(id);
        testRunSource.setName(sourceName);
        testRunSource.setConfig(config);
        testRunSource.setTransformationClass(TestRunSourceSpoutFluxComponent.class.getName());

        testTopologyDag.add(testRunSource);
        sourceToReplacedTestSourceMap.put(sourceName, testRunSource);
    }

    @Override
    public void visit(StreamlineSink sink) {
        String id = sink.getId();
        String sinkName = sink.getName();

        if (!testRunSinksMap.containsKey(sinkName)) {
            throw new IllegalStateException("Not all sinks have corresponding TestRunSink instance. sink name: " + sinkName);
        }

        Config config = new Config(sink.getConfig());

        TestRunSink testRunSink = testRunSinksMap.get(sinkName);

        testRunSink.setId(id);
        testRunSink.setName(sinkName);
        testRunSink.setConfig(config);
        testRunSink.setTransformationClass(TestRunSinkBoltFluxComponent.class.getName());

        testTopologyDag.add(testRunSink);
        sinkToReplacedTestSinkMap.put(sinkName, testRunSink);

        copyEdges(sink);
    }

    @Override
    public void visit(StreamlineProcessor processor) {
        testTopologyDag.add(processor);

        copyEdges(processor);
    }

    private void copyEdges(InputComponent inputComponent) {
        List<Edge> edgesTo = originTopologyDag.getEdgesTo(inputComponent);
        edgesTo.forEach(e -> {
            OutputComponent from = e.getFrom();
            InputComponent to = e.getTo();
            Edge newEdge = new Edge(e.getId(), e.getFrom(), e.getTo(), e.getStreamGroupings());

            StreamlineSource replacedSource = sourceToReplacedTestSourceMap.get(from.getName());
            StreamlineSink replacedSink = sinkToReplacedTestSinkMap.get(to.getName());

            if (replacedSource != null) {
                newEdge.setFrom(replacedSource);
            }

            if (replacedSink != null) {
                newEdge.setTo(replacedSink);
            }

            testTopologyDag.addEdge(newEdge);
        });
    }

    @Override
    public void visit(Edge edge) {
        // no-op
    }

    public TopologyDag getTestTopologyDag() {
        return testTopologyDag;
    }
}