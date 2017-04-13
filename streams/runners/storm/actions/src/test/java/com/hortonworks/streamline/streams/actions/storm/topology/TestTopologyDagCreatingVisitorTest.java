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

import com.hortonworks.streamline.streams.actions.utils.TopologyTestHelper;
import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.InputComponent;
import com.hortonworks.streamline.streams.layout.component.OutputComponent;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestTopologyDagCreatingVisitorTest {
    @Test
    public void visitSource() throws Exception {
        StreamlineSource originSource = TopologyTestHelper.createStreamlineSource("1");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originSource);

        TestRunSource testSource = createTestRunSource(originSource);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.singletonMap(originSource.getName(), testSource),
                        Collections.emptyMap(), Collections.emptyMap());
        visitor.visit(originSource);

        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        List<OutputComponent> testSources = testTopologyDag.getOutputComponents().stream()
                .filter(o -> (o instanceof TestRunSource && o.getName().equals(originSource.getName())))
                .collect(toList());

        assertEquals(1, testSources.size());

        TestRunSource testRunSource = (TestRunSource) testSources.get(0);
        assertEquals(originSource.getId(), testRunSource.getId());
        assertEquals(testSource.getTestRecordsForEachStream(), testRunSource.getTestRecordsForEachStream());
    }

    @Test
    public void visitSource_noMatchingTestRunSource() throws Exception {
        StreamlineSource originSource = TopologyTestHelper.createStreamlineSource("1");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originSource);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag, Collections.emptyMap(), Collections.emptyMap(),
                        Collections.emptyMap());

        try {
            visitor.visit(originSource);
            fail("IllegalStateException should be thrown.");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains(originSource.getName()));
        }
    }

    @Test
    public void visitSink_connectedFromSource() throws Exception {
        StreamlineSource originSource = TopologyTestHelper.createStreamlineSource("1");
        StreamlineSink originSink = TopologyTestHelper.createStreamlineSink("2");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originSource);
        originTopologyDag.add(originSink);
        originTopologyDag.addEdge(new Edge("e1", originSource, originSink, "default", Stream.Grouping.SHUFFLE));

        TestRunSource testSource = createTestRunSource(originSource);
        TestRunSink testSink = createTestRunSink(originSink);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.singletonMap(originSource.getName(), testSource),
                        Collections.emptyMap(), Collections.singletonMap(originSink.getName(), testSink));

        visitor.visit(originSource);
        visitor.visit(originSink);

        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        List<OutputComponent> testSources = testTopologyDag.getOutputComponents().stream()
                .filter(o -> (o instanceof TestRunSource && o.getName().equals(originSource.getName())))
                .collect(toList());

        List<InputComponent> testSinks = testTopologyDag.getInputComponents().stream()
                .filter(o -> (o instanceof TestRunSink && o.getName().equals(originSink.getName())))
                .collect(toList());

        assertEquals(1, testSinks.size());

        TestRunSource testRunSource = (TestRunSource) testSources.get(0);
        TestRunSink testRunSink = (TestRunSink) testSinks.get(0);
        assertEquals(originSink.getId(), testRunSink.getId());
        assertEquals(testSink.getOutputFilePath(), testRunSink.getOutputFilePath());

        assertEquals(1, testTopologyDag.getEdgesFrom(testRunSource).size());
        assertEquals(1, testTopologyDag.getEdgesTo(testRunSink).size());

        assertTrue(testTopologyDag.getEdgesFrom(testRunSource).get(0) == testTopologyDag.getEdgesTo(testRunSink).get(0));
    }

    @Test
    public void visitSink_connectedFromProcessor() throws Exception {
        StreamlineProcessor originProcessor = TopologyTestHelper.createStreamlineProcessor("1");
        StreamlineSink originSink = TopologyTestHelper.createStreamlineSink("2");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originProcessor);
        originTopologyDag.add(originSink);
        originTopologyDag.addEdge(new Edge("e1", originProcessor, originSink, "default", Stream.Grouping.SHUFFLE));

        TestRunProcessor testProcessor = createTestRunProcessor(originProcessor);
        TestRunSink testSink = createTestRunSink(originSink);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.emptyMap(),
                        Collections.singletonMap(originProcessor.getName(), testProcessor),
                        Collections.singletonMap(originSink.getName(), testSink));

        visitor.visit(originProcessor);
        visitor.visit(originSink);

        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        List<OutputComponent> testProcessors = testTopologyDag.getOutputComponents().stream()
                .filter(o -> (o instanceof TestRunProcessor && o.getName().equals(originProcessor.getName())))
                .collect(toList());

        List<InputComponent> testSinks = testTopologyDag.getInputComponents().stream()
                .filter(o -> (o instanceof TestRunSink && o.getName().equals(originSink.getName())))
                .collect(toList());


        assertEquals(1, testProcessors.size());
        assertEquals(1, testSinks.size());

        TestRunProcessor testRunProcessor = (TestRunProcessor) testProcessors.get(0);
        assertEquals(originProcessor.getId(), testRunProcessor.getId());

        TestRunSink testRunSink = (TestRunSink) testSinks.get(0);
        assertEquals(originSink.getId(), testRunSink.getId());
        assertEquals(testSink.getOutputFilePath(), testRunSink.getOutputFilePath());

        assertEquals(1, testTopologyDag.getEdgesFrom(testProcessor).size());
        assertEquals(1, testTopologyDag.getEdgesTo(testRunSink).size());

        assertTrue(testTopologyDag.getEdgesFrom(testRunProcessor).get(0) == testTopologyDag.getEdgesTo(testRunSink).get(0));
    }

    @Test
    public void visitSink_noMatchingTestRunSink() throws Exception {
        TopologyDag originTopologyDag = new TopologyDag();
        StreamlineSink originSink = TopologyTestHelper.createStreamlineSink("1");
        originTopologyDag.add(originSink);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag, Collections.emptyMap(), Collections.emptyMap(),
                        Collections.emptyMap());

        try {
            visitor.visit(originSink);
            fail("IllegalStateException should be thrown.");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains(originSink.getName()));
        }
    }

    @Test
    public void visitProcessor_connectedFromSource() throws Exception {
        StreamlineSource originSource = TopologyTestHelper.createStreamlineSource("1");
        StreamlineProcessor originProcessor = TopologyTestHelper.createStreamlineProcessor("2");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originSource);
        originTopologyDag.add(originProcessor);
        originTopologyDag.addEdge(new Edge("e1", originSource, originProcessor, "default", Stream.Grouping.SHUFFLE));

        TestRunSource testSource = createTestRunSource(originSource);
        TestRunProcessor testProcessor = createTestRunProcessor(originProcessor);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.singletonMap(originSource.getName(), testSource),
                        Collections.singletonMap(originProcessor.getName(), testProcessor),
                        Collections.emptyMap());

        visitor.visit(originSource);
        visitor.visit(originProcessor);

        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        List<OutputComponent> testProcessors = testTopologyDag.getOutputComponents().stream()
                .filter(o -> (o instanceof TestRunProcessor && o.getName().equals(originProcessor.getName())))
                .collect(toList());

        List<OutputComponent> testSources = testTopologyDag.getOutputComponents().stream()
                .filter(o -> (o instanceof TestRunSource && o.getName().equals(originSource.getName())))
                .collect(toList());

        TestRunProcessor testRunProcessor = (TestRunProcessor) testProcessors.get(0);
        TestRunSource testRunSource = (TestRunSource) testSources.get(0);

        assertEquals(1, testTopologyDag.getEdgesFrom(testRunSource).size());
        assertEquals(1, testTopologyDag.getEdgesTo(testRunProcessor).size());

        assertTrue(testTopologyDag.getEdgesFrom(testRunSource).get(0) == testTopologyDag.getEdgesTo(testRunProcessor).get(0));
    }

    @Test
    public void visitProcessor_connectedFromProcessor() throws Exception {
        StreamlineProcessor originProcessor = TopologyTestHelper.createStreamlineProcessor("1");
        StreamlineProcessor originProcessor2 = TopologyTestHelper.createStreamlineProcessor("2");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originProcessor);
        originTopologyDag.add(originProcessor2);
        originTopologyDag.addEdge(new Edge("e1", originProcessor, originProcessor2, "default", Stream.Grouping.SHUFFLE));

        TestRunProcessor testProcessor = createTestRunProcessor(originProcessor);
        TestRunProcessor testProcessor2 = createTestRunProcessor(originProcessor2);

        Map<String, TestRunProcessor> processorMap = new HashMap<>();
        processorMap.put(originProcessor.getName(), testProcessor);
        processorMap.put(originProcessor2.getName(), testProcessor2);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.emptyMap(), processorMap, Collections.emptyMap());

        visitor.visit(originProcessor);
        visitor.visit(originProcessor2);

        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        List<OutputComponent> testProcessors = testTopologyDag.getOutputComponents().stream()
                .filter(o -> (o instanceof TestRunProcessor))
                .collect(toList());

        Optional<OutputComponent> testRunProcessorOptional = testProcessors.stream()
                .filter(o -> o.getName().equals(originProcessor.getName()))
                .findAny();

        Optional<OutputComponent> testRunProcessor2Optional = testProcessors.stream()
                .filter(o -> o.getName().equals(originProcessor2.getName()))
                .findAny();

        assertTrue(testRunProcessorOptional.isPresent());
        assertTrue(testRunProcessor2Optional.isPresent());

        TestRunProcessor testRunProcessor = (TestRunProcessor) testRunProcessorOptional.get();
        TestRunProcessor testRunProcessor2 = (TestRunProcessor) testRunProcessor2Optional.get();

        assertEquals(1, testTopologyDag.getEdgesFrom(testRunProcessor).size());
        assertEquals(1, testTopologyDag.getEdgesTo(testRunProcessor2).size());

        assertTrue(testTopologyDag.getEdgesFrom(testRunProcessor).get(0) == testTopologyDag.getEdgesTo(testRunProcessor2).get(0));
    }

    @Test
    public void visitRulesProcessor_connectedFromSource() throws Exception {
        StreamlineSource originSource = TopologyTestHelper.createStreamlineSource("1");
        RulesProcessor rulesProcessor = TopologyTestHelper.createRulesProcessor("2");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originSource);
        originTopologyDag.add(rulesProcessor);
        originTopologyDag.addEdge(new Edge("e1", originSource, rulesProcessor, "default", Stream.Grouping.SHUFFLE));

        TestRunSource testSource = createTestRunSource(originSource);
        TestRunProcessor testProcessor = createTestRunProcessor(rulesProcessor);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.singletonMap(originSource.getName(), testSource),
                        Collections.singletonMap(rulesProcessor.getName(), testProcessor), Collections.emptyMap());

        visitor.visit(originSource);
        visitor.visit(rulesProcessor);

        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        List<OutputComponent> testProcessors = testTopologyDag.getOutputComponents().stream()
                .filter(o -> (o instanceof TestRunProcessor && o.getName().equals(rulesProcessor.getName())))
                .collect(toList());

        List<OutputComponent> testSources = testTopologyDag.getOutputComponents().stream()
                .filter(o -> (o instanceof TestRunSource && o.getName().equals(originSource.getName())))
                .collect(toList());

        TestRunProcessor testRunProcessor = (TestRunProcessor) testProcessors.get(0);
        TestRunSource testRunSource = (TestRunSource) testSources.get(0);

        assertEquals(1, testTopologyDag.getEdgesFrom(testRunSource).size());
        assertEquals(1, testTopologyDag.getEdgesTo(testRunProcessor).size());

        assertTrue(testTopologyDag.getEdgesFrom(testRunSource).get(0) == testTopologyDag.getEdgesTo(testRunProcessor).get(0));
    }

    @Test
    public void visitRulesProcessor_connectedFromProcessor() throws Exception {
        StreamlineProcessor originProcessor = TopologyTestHelper.createStreamlineProcessor("1");
        RulesProcessor rulesProcessor = TopologyTestHelper.createRulesProcessor("2");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originProcessor);
        originTopologyDag.add(rulesProcessor);
        originTopologyDag.addEdge(new Edge("e1", originProcessor, rulesProcessor, "default", Stream.Grouping.SHUFFLE));

        TestRunProcessor testProcessor = createTestRunProcessor(originProcessor);
        TestRunProcessor testRulesProcessor = createTestRunProcessor(rulesProcessor);

        Map<String, TestRunProcessor> processorMap = new HashMap<>();
        processorMap.put(originProcessor.getName(), testProcessor);
        processorMap.put(rulesProcessor.getName(), testRulesProcessor);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.emptyMap(), processorMap, Collections.emptyMap());

        visitor.visit(originProcessor);
        visitor.visit(rulesProcessor);

        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        List<OutputComponent> testProcessors = testTopologyDag.getOutputComponents().stream()
                .filter(o -> (o instanceof TestRunProcessor))
                .collect(toList());

        Optional<OutputComponent> testRunProcessorOptional = testProcessors.stream()
                .filter(o -> o.getName().equals(originProcessor.getName()))
                .findAny();

        Optional<OutputComponent> testRunRuleProcessorOptional = testProcessors.stream()
                .filter(o -> o.getName().equals(rulesProcessor.getName()))
                .findAny();

        assertTrue(testRunProcessorOptional.isPresent());
        assertTrue(testRunRuleProcessorOptional.isPresent());

        TestRunProcessor testRunProcessor = (TestRunProcessor) testRunProcessorOptional.get();
        TestRunProcessor testRunRuleProcessor = (TestRunProcessor) testRunRuleProcessorOptional.get();

        assertEquals(1, testTopologyDag.getEdgesFrom(testRunProcessor).size());
        assertEquals(1, testTopologyDag.getEdgesTo(testRunRuleProcessor).size());

        assertTrue(testTopologyDag.getEdgesFrom(testRunProcessor).get(0) == testTopologyDag.getEdgesTo(testRunRuleProcessor).get(0));
    }

    private TestRunSource createTestRunSource(StreamlineSource originSource) {
        Map<String, List<Map<String, Object>>> testRecordsMap = new HashMap<>();
        for (Stream stream : originSource.getOutputStreams()) {
            testRecordsMap.put(stream.getId(), TopologyTestHelper.createTestRecords());
        }
        TestRunSource testRunSource = new TestRunSource(originSource.getOutputStreams(), testRecordsMap, 1, "");
        testRunSource.setName(originSource.getName());
        return testRunSource;
    }

    private TestRunProcessor createTestRunProcessor(StreamlineProcessor originProcessor) {
        if (originProcessor instanceof RulesProcessor) {
            RulesProcessor rulesProcessor = (RulesProcessor) originProcessor;

            boolean windowed = rulesProcessor.getRules().stream().anyMatch(r -> r.getWindow() != null);
            TestRunProcessor testRunProcessor = new TestRunProcessor(originProcessor, windowed, "");
            testRunProcessor.setName(originProcessor.getName());
            return testRunProcessor;
        } else {
            TestRunProcessor testRunProcessor = new TestRunProcessor(originProcessor, false, "");
            testRunProcessor.setName(originProcessor.getName());
            return testRunProcessor;
        }
    }

    private TestRunSink createTestRunSink(StreamlineSink originSink) {
        TestRunSink testRunSink = new TestRunSink("dummyFilePath", "");
        testRunSink.setName(originSink.getName());
        return testRunSink;
    }
}