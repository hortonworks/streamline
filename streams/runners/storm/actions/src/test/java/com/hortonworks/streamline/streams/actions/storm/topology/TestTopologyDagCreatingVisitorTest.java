package com.hortonworks.streamline.streams.actions.storm.topology;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.InputComponent;
import com.hortonworks.streamline.streams.layout.component.OutputComponent;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestTopologyDagCreatingVisitorTest {
    @Test
    public void visitSource() throws Exception {
        StreamlineSource originSource = createStreamlineSource("1");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originSource);

        TestRunSource testSource = createTestRunSource(originSource);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.singletonMap(originSource.getName(), testSource),
                        Collections.emptyMap());
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
        StreamlineSource originSource = createStreamlineSource("1");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originSource);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag, Collections.emptyMap(), Collections.emptyMap());

        try {
            visitor.visit(originSource);
            fail("IllegalStateException should be thrown.");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains(originSource.getName()));
        }
    }

    @Test
    public void visitSink_connectedFromSource() throws Exception {
        StreamlineSource originSource = createStreamlineSource("1");
        StreamlineSink originSink = createStreamlineSink("2");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originSource);
        originTopologyDag.add(originSink);
        originTopologyDag.addEdge(new Edge("e1", originSource, originSink, "default", Stream.Grouping.SHUFFLE));

        TestRunSource testSource = createTestRunSource(originSource);
        TestRunSink testSink = createTestRunSink();

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.singletonMap(originSource.getName(), testSource),
                        Collections.singletonMap(originSink.getName(), testSink));

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
        assertEquals(testSink.getOutputFileUUID(), testRunSink.getOutputFileUUID());
        assertEquals(testSink.getOutputFilePath(), testRunSink.getOutputFilePath());

        assertEquals(1, testTopologyDag.getEdgesFrom(testRunSource).size());
        assertEquals(1, testTopologyDag.getEdgesTo(testRunSink).size());

        assertTrue(testTopologyDag.getEdgesFrom(testRunSource).get(0) == testTopologyDag.getEdgesTo(testRunSink).get(0));
    }

    @Test
    public void visitSink_connectedFromProcessor() throws Exception {
        StreamlineProcessor originProcessor = createStreamlineProcessor("1");
        StreamlineSink originSink = createStreamlineSink("2");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originProcessor);
        originTopologyDag.add(originSink);
        originTopologyDag.addEdge(new Edge("e1", originProcessor, originSink, "default", Stream.Grouping.SHUFFLE));

        TestRunSink testSink = createTestRunSink();

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.emptyMap(),
                        Collections.singletonMap(originSink.getName(), testSink));

        visitor.visit(originProcessor);
        visitor.visit(originSink);

        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        List<InputComponent> testSinks = testTopologyDag.getInputComponents().stream()
                .filter(o -> (o instanceof TestRunSink && o.getName().equals(originSink.getName())))
                .collect(toList());

        assertEquals(1, testSinks.size());

        TestRunSink testRunSink = (TestRunSink) testSinks.get(0);
        assertEquals(originSink.getId(), testRunSink.getId());
        assertEquals(testSink.getOutputFileUUID(), testRunSink.getOutputFileUUID());
        assertEquals(testSink.getOutputFilePath(), testRunSink.getOutputFilePath());

        assertEquals(1, testTopologyDag.getEdgesFrom(originProcessor).size());
        assertEquals(1, testTopologyDag.getEdgesTo(testRunSink).size());

        assertTrue(testTopologyDag.getEdgesFrom(originProcessor).get(0) == testTopologyDag.getEdgesTo(testRunSink).get(0));
    }

    @Test
    public void visitSink_noMatchingTestRunSink() throws Exception {
        TopologyDag originTopologyDag = new TopologyDag();
        StreamlineSink originSink = createStreamlineSink("1");
        originTopologyDag.add(originSink);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag, Collections.emptyMap(), Collections.emptyMap());

        try {
            visitor.visit(originSink);
            fail("IllegalStateException should be thrown.");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains(originSink.getName()));
        }
    }

    @Test
    public void visitProcessor_connectedFromSource() throws Exception {
        StreamlineSource originSource = createStreamlineSource("1");
        StreamlineProcessor originProcessor = createStreamlineProcessor("2");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originSource);
        originTopologyDag.add(originProcessor);
        originTopologyDag.addEdge(new Edge("e1", originSource, originProcessor, "default", Stream.Grouping.SHUFFLE));

        TestRunSource testSource = createTestRunSource(originSource);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.singletonMap(originSource.getName(), testSource),
                        Collections.emptyMap());

        visitor.visit(originSource);
        visitor.visit(originProcessor);

        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        List<OutputComponent> testSources = testTopologyDag.getOutputComponents().stream()
                .filter(o -> (o instanceof TestRunSource && o.getName().equals(originSource.getName())))
                .collect(toList());

        TestRunSource testRunSource = (TestRunSource) testSources.get(0);

        assertEquals(1, testTopologyDag.getEdgesFrom(testRunSource).size());
        assertEquals(1, testTopologyDag.getEdgesTo(originProcessor).size());

        assertTrue(testTopologyDag.getEdgesFrom(testRunSource).get(0) == testTopologyDag.getEdgesTo(originProcessor).get(0));
    }

    @Test
    public void visitProcessor_connectedFromProcessor() throws Exception {
        StreamlineProcessor originProcessor = createStreamlineProcessor("1");
        StreamlineProcessor originProcessor2 = createStreamlineProcessor("2");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originProcessor);
        originTopologyDag.add(originProcessor2);
        originTopologyDag.addEdge(new Edge("e1", originProcessor, originProcessor2, "default", Stream.Grouping.SHUFFLE));

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.emptyMap(),
                        Collections.emptyMap());

        visitor.visit(originProcessor);
        visitor.visit(originProcessor2);

        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        assertEquals(1, testTopologyDag.getEdgesFrom(originProcessor).size());
        assertEquals(1, testTopologyDag.getEdgesTo(originProcessor2).size());

        assertTrue(testTopologyDag.getEdgesFrom(originProcessor).get(0) == testTopologyDag.getEdgesTo(originProcessor2).get(0));
    }

    @Test
    public void visitRulesProcessor_connectedFromSource() throws Exception {
        StreamlineSource originSource = createStreamlineSource("1");
        RulesProcessor rulesProcessor = createRulesProcessor("2");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originSource);
        originTopologyDag.add(rulesProcessor);
        originTopologyDag.addEdge(new Edge("e1", originSource, rulesProcessor, "default", Stream.Grouping.SHUFFLE));

        TestRunSource testSource = createTestRunSource(originSource);

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.singletonMap(originSource.getName(), testSource),
                        Collections.emptyMap());

        visitor.visit(originSource);
        visitor.visit(rulesProcessor);

        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        List<OutputComponent> testSources = testTopologyDag.getOutputComponents().stream()
                .filter(o -> (o instanceof TestRunSource && o.getName().equals(originSource.getName())))
                .collect(toList());

        TestRunSource testRunSource = (TestRunSource) testSources.get(0);

        assertEquals(1, testTopologyDag.getEdgesFrom(testRunSource).size());
        assertEquals(1, testTopologyDag.getEdgesTo(rulesProcessor).size());

        assertTrue(testTopologyDag.getEdgesFrom(testRunSource).get(0) == testTopologyDag.getEdgesTo(rulesProcessor).get(0));
    }

    @Test
    public void visitRulesProcessor_connectedFromProcessor() throws Exception {
        StreamlineProcessor originProcessor = createStreamlineProcessor("1");
        RulesProcessor rulesProcessor = createRulesProcessor("2");

        TopologyDag originTopologyDag = new TopologyDag();
        originTopologyDag.add(originProcessor);
        originTopologyDag.add(rulesProcessor);
        originTopologyDag.addEdge(new Edge("e1", originProcessor, rulesProcessor, "default", Stream.Grouping.SHUFFLE));

        TestTopologyDagCreatingVisitor visitor =
                new TestTopologyDagCreatingVisitor(originTopologyDag,
                        Collections.emptyMap(),
                        Collections.emptyMap());

        visitor.visit(originProcessor);
        visitor.visit(rulesProcessor);

        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        assertEquals(1, testTopologyDag.getEdgesFrom(originProcessor).size());
        assertEquals(1, testTopologyDag.getEdgesTo(rulesProcessor).size());

        assertTrue(testTopologyDag.getEdgesFrom(originProcessor).get(0) == testTopologyDag.getEdgesTo(rulesProcessor).get(0));
    }

    private StreamlineSource createStreamlineSource(String id) {
        Stream stream = createDefaultStream();
        StreamlineSource source = new StreamlineSource(Sets.newHashSet(stream));
        source.setId(id);
        source.setName("testSource_" + id);
        source.setConfig(new Config());
        source.setTransformationClass("dummyTransformation");
        return source;
    }

    private StreamlineProcessor createStreamlineProcessor(String id) {
        Stream stream = createDefaultStream();
        StreamlineProcessor processor = new StreamlineProcessor(Sets.newHashSet(stream));
        processor.setId(id);
        processor.setName("testProcessor_" + id);
        processor.setConfig(new Config());
        processor.setTransformationClass("dummyTransformation");
        return processor;
    }

    private RulesProcessor createRulesProcessor(String id) {
        RulesProcessor processor = new RulesProcessor();
        processor.setId(id);
        processor.setName("testRuleProcessor_" + id);
        processor.setConfig(new Config());
        processor.setTransformationClass("dummyTransformation");
        processor.setProcessAll(true);
        processor.setRules(Collections.emptyList());
        return processor;
    }

    private StreamlineSink createStreamlineSink(String id) {
        StreamlineSink sink = new StreamlineSink();
        sink.setId(id);
        sink.setName("testSink_" + id);
        sink.setConfig(new Config());
        sink.setTransformationClass("dummyTransformation");
        return sink;
    }

    private TestRunSource createTestRunSource(StreamlineSource originSource) {
        Map<String, List<Map<String, Object>>> testRecordsMap = new HashMap<>();
        for (Stream stream : originSource.getOutputStreams()) {
            testRecordsMap.put(stream.getId(), createTestRecords());
        }
        return new TestRunSource(originSource.getOutputStreams(), testRecordsMap);
    }

    private List<Map<String, Object>> createTestRecords() {
        List<Map<String, Object>> testRecords = new ArrayList<>();

        Map<String, Object> testRecord1 = new HashMap<>();
        testRecord1.put("A", 1);
        testRecord1.put("B", 2);

        Map<String, Object> testRecord2 = new HashMap<>();
        testRecord2.put("A", 1);
        testRecord2.put("B", 2);

        testRecords.add(testRecord1);
        testRecords.add(testRecord2);

        return testRecords;
    }

    private TestRunSink createTestRunSink() {
        return new TestRunSink(UUID.randomUUID().toString(), "dummyFilePath");
    }

    private Stream createDefaultStream() {
        Schema.Field field = Schema.Field.of("A", Schema.Type.INTEGER);
        Schema.Field field2 = Schema.Field.of("B", Schema.Type.INTEGER);
        return new Stream("default", Lists.newArrayList(field, field2));
    }
}