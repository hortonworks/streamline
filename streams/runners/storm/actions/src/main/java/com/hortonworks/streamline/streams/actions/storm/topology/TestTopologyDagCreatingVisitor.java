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
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunRulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import com.hortonworks.streamline.streams.layout.storm.TestRunProcessorBoltFluxComponent;
import com.hortonworks.streamline.streams.layout.storm.TestRunRulesProcessorBoltFluxComponent;
import com.hortonworks.streamline.streams.layout.storm.TestRunSinkBoltFluxComponent;
import com.hortonworks.streamline.streams.layout.storm.TestRunSourceSpoutFluxComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestTopologyDagCreatingVisitor extends TopologyDagVisitor {

    private final Map<String, StreamlineSource> sourceToReplacedTestSourceMap;
    private final Map<String, StreamlineProcessor> processorToReplacedTestProcessorMap;
    private final Map<String, StreamlineSink> sinkToReplacedTestSinkMap;
    private final Map<String, TestRunSource> testRunSourcesForEachSource;
    private final Map<String, TestRunProcessor> testRunProcessorsForEachProcessor;
    private final Map<String, TestRunRulesProcessor> testRunRulesProcessorsForEachProcessor;
    private final Map<String, TestRunSink> testRunSinksForEachSink;
    private final TopologyDag originTopologyDag;

    private TopologyDag testTopologyDag;

    public TestTopologyDagCreatingVisitor(TopologyDag originTopologyDag,
                                          Map<String, TestRunSource> testRunSourcesForEachSource,
                                          Map<String, TestRunProcessor> testRunProcessorsForEachProcessor,
                                          Map<String, TestRunRulesProcessor> testRunRulesProcessorsForEachProcessor,
                                          Map<String, TestRunSink> testRunSinksForEachSink) {
        this.originTopologyDag = originTopologyDag;
        this.testRunSourcesForEachSource = testRunSourcesForEachSource;
        this.testRunProcessorsForEachProcessor = testRunProcessorsForEachProcessor;
        this.testRunRulesProcessorsForEachProcessor = testRunRulesProcessorsForEachProcessor;
        this.testRunSinksForEachSink = testRunSinksForEachSink;

        this.testTopologyDag = new TopologyDag();
        this.sourceToReplacedTestSourceMap = new HashMap<>();
        this.processorToReplacedTestProcessorMap = new HashMap<>();
        this.sinkToReplacedTestSinkMap = new HashMap<>();
    }

    @Override
    public void visit(RulesProcessor rulesProcessor) {
        String id = rulesProcessor.getId();
        String processorName = rulesProcessor.getName();

        if (!testRunRulesProcessorsForEachProcessor.containsKey(processorName)) {
            throw new IllegalStateException("Not all processors have corresponding TestRunRulesProcessor instance. processor name: " + processorName);
        }

        Config config = new Config(rulesProcessor.getConfig());

        TestRunRulesProcessor testRunRulesProcessor = testRunRulesProcessorsForEachProcessor.get(processorName);

        testRunRulesProcessor.setId(id);
        testRunRulesProcessor.setName(processorName);
        testRunRulesProcessor.setConfig(config);
        testRunRulesProcessor.setTransformationClass(TestRunRulesProcessorBoltFluxComponent.class.getName());

        testTopologyDag.add(testRunRulesProcessor);
        processorToReplacedTestProcessorMap.put(processorName, testRunRulesProcessor);

        copyEdges(rulesProcessor);
    }

    @Override
    public void visit(StreamlineSource source) {
        String id = source.getId();
        String sourceName = source.getName();

        if (!testRunSourcesForEachSource.containsKey(sourceName)) {
            throw new IllegalStateException("Not all sources have corresponding TestRunSource instance. source name: " + sourceName);
        }

        Config config = new Config(source.getConfig());

        TestRunSource testRunSource = testRunSourcesForEachSource.get(sourceName);

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

        if (!testRunSinksForEachSink.containsKey(sinkName)) {
            throw new IllegalStateException("Not all sinks have corresponding TestRunSink instance. sink name: " + sinkName);
        }

        Config config = new Config(sink.getConfig());

        TestRunSink testRunSink = testRunSinksForEachSink.get(sinkName);

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
        String id = processor.getId();
        String processorName = processor.getName();

        if (!testRunProcessorsForEachProcessor.containsKey(processorName)) {
            throw new IllegalStateException("Not all processors have corresponding TestRunProcessor instance. processor name: " + processorName);
        }

        Config config = new Config(processor.getConfig());

        TestRunProcessor testRunProcessor = testRunProcessorsForEachProcessor.get(processorName);

        testRunProcessor.setId(id);
        testRunProcessor.setName(processorName);
        testRunProcessor.setConfig(config);
        testRunProcessor.setTransformationClass(TestRunProcessorBoltFluxComponent.class.getName());

        testTopologyDag.add(testRunProcessor);
        processorToReplacedTestProcessorMap.put(processorName, testRunProcessor);

        copyEdges(processor);
    }

    private void copyEdges(InputComponent inputComponent) {
        List<Edge> edgesTo = originTopologyDag.getEdgesTo(inputComponent);
        edgesTo.forEach(e -> {
            OutputComponent from = e.getFrom();
            InputComponent to = e.getTo();
            Edge newEdge = new Edge(e.getId(), e.getFrom(), e.getTo(), e.getStreamGroupings());

            StreamlineSource replacedSource = sourceToReplacedTestSourceMap.get(from.getName());
            StreamlineProcessor replacedProcessorSource = processorToReplacedTestProcessorMap.get(from.getName());

            StreamlineSink replacedSink = sinkToReplacedTestSinkMap.get(to.getName());
            StreamlineProcessor replacedProcessorSink = processorToReplacedTestProcessorMap.get(to.getName());

            if (replacedSource != null) {
                newEdge.setFrom(replacedSource);
            } else if (replacedProcessorSource != null) {
                newEdge.setFrom(replacedProcessorSource);
            }

            if (replacedSink != null) {
                newEdge.setTo(replacedSink);
            } else if (replacedProcessorSink != null) {
                newEdge.setTo(replacedProcessorSink);
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