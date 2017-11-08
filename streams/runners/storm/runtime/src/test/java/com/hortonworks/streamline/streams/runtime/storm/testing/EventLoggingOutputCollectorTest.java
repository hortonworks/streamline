package com.hortonworks.streamline.streams.runtime.storm.testing;

import com.google.common.collect.Lists;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(JMockit.class)
public class EventLoggingOutputCollectorTest {
    private static final String TEST_COMPONENT_NAME_FOR_STORM = "1-testComponent";
    private static final String TEST_TARGET_COMPONENT_FOR_TASK_1_FOR_STORM = "2-testTargetComponent1";
    private static final String TEST_TARGET_COMPONENT_FOR_TASK_2_FOR_STORM = "3-testTargetComponent2";
    private static final int TASK_1 = 1;
    private static final int TASK_2 = 2;

    @Injectable
    private TestRunEventLogger mockedEventLogger;

    @Injectable
    private TopologyContext mockedTopologyContext;

    @Injectable
    private OutputCollector mockedOutputCollector;

    @Injectable
    private Tuple anchor;

    private Collection<Tuple> anchors;

    private EventLoggingOutputCollector sut;

    public static final StreamlineEventImpl INPUT_STREAMLINE_EVENT = StreamlineEventImpl.builder()
            .fieldsAndValues(new HashMap<String, Object>() {{
                put("illuminance", 70);
                put("temp", 104);
                put("foo", 100);
                put("humidity", "40h");
            }})
            .dataSourceId("ds-" + System.currentTimeMillis()).build();

    @Before
    public void setUp() {
        anchors = Collections.singletonList(anchor);
    }

    @Test
    public void emit() throws Exception {
        setupExpectationsForTopologyContextEmit();
        sut = new EventLoggingOutputCollector(mockedTopologyContext, mockedOutputCollector, mockedEventLogger);

        String testStreamId = "testStreamId";
        List<Integer> expectedTasks = Lists.newArrayList(TASK_1, TASK_2);
        final List<Tuple> anchors = Collections.singletonList(anchor);
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);

        // String streamId, Tuple anchor, List<Object> anchor
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, anchor, tuple);
            result = expectedTasks;
        }};

        List<Integer> tasks = sut.emit(testStreamId, anchor, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(testStreamId, anchor, tuple);
        }};

        verifyEventsAreWrittenProperly(INPUT_STREAMLINE_EVENT, expectedTasks.size());

        // String streamId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, tuple);
            result = expectedTasks;
        }};

        tasks = sut.emit(testStreamId, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(testStreamId, tuple);
        }};

        verifyEventsAreWrittenProperly(INPUT_STREAMLINE_EVENT, expectedTasks.size());

        // Collection<Tuple> anchors, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(anchors, tuple);
            result = expectedTasks;
        }};

        tasks = sut.emit(anchors, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(anchors, tuple);
        }};

        verifyEventsAreWrittenProperly(INPUT_STREAMLINE_EVENT, expectedTasks.size());

        // Tuple anchor, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(anchor, tuple);
            result = expectedTasks;
        }};

        tasks = sut.emit(anchor, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(anchor, tuple);
        }};

        verifyEventsAreWrittenProperly(INPUT_STREAMLINE_EVENT, expectedTasks.size());

        // List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(tuple);
            result = expectedTasks;
        }};

        tasks = sut.emit(tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(tuple);
        }};

        verifyEventsAreWrittenProperly(INPUT_STREAMLINE_EVENT, expectedTasks.size());

        // String streamId, Collection<Tuple> anchors, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, anchors, tuple);
            result = expectedTasks;
        }};

        tasks = sut.emit(testStreamId, anchors, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(testStreamId, anchors, tuple);
        }};

        verifyEventsAreWrittenProperly(INPUT_STREAMLINE_EVENT, expectedTasks.size());
    }

    @Test
    public void emitDirect() throws Exception {
        setupExpectationsForTopologyContextEmitDirect();
        sut = new EventLoggingOutputCollector(mockedTopologyContext, mockedOutputCollector, mockedEventLogger);

        int testTaskId = 1;
        String testStreamId = "testStreamId";
        final List<Tuple> anchors = Collections.singletonList(anchor);
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);

        // int taskId, String streamId, Tuple anchor, List<Object> anchor
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, anchor, tuple);
        }};

        sut.emitDirect(testTaskId, testStreamId, anchor, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, anchor, tuple);
        }};

        verifyEventsAreWrittenProperly(INPUT_STREAMLINE_EVENT, 1);

        // int taskId, String streamId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, tuple);
        }};

        sut.emitDirect(testTaskId, testStreamId, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, tuple);
        }};

        verifyEventsAreWrittenProperly(INPUT_STREAMLINE_EVENT, 1);

        // int taskId, Collection<Tuple> anchors, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, anchors, tuple);
        }};

        sut.emitDirect(testTaskId, anchors, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, anchors, tuple);
        }};

        verifyEventsAreWrittenProperly(INPUT_STREAMLINE_EVENT, 1);

        // int taskId, Tuple anchor, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, anchor, tuple);
        }};

        sut.emitDirect(testTaskId, anchor, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, anchor, tuple);
        }};

        verifyEventsAreWrittenProperly(INPUT_STREAMLINE_EVENT, 1);

        // int taskId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, tuple);
        }};

        sut.emitDirect(testTaskId, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, tuple);
        }};

        verifyEventsAreWrittenProperly(INPUT_STREAMLINE_EVENT, 1);

        // int taskId, String streamId, Collection<Tuple> anchors, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, anchors, tuple);
        }};

        sut.emitDirect(testTaskId, testStreamId, anchors, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, anchors, tuple);
        }};

        verifyEventsAreWrittenProperly(INPUT_STREAMLINE_EVENT, 1);
    }

    @Test
    public void testAck() throws Exception {
        setupExpectationsForTopologyContextNoEmit();
        sut = new EventLoggingOutputCollector(mockedTopologyContext, mockedOutputCollector, mockedEventLogger);

        sut.ack(anchor);

        new Verifications() {{
            mockedOutputCollector.ack(anchor); times = 1;
        }};
    }

    @Test
    public void testFail() throws Exception {
        setupExpectationsForTopologyContextNoEmit();
        sut = new EventLoggingOutputCollector(mockedTopologyContext, mockedOutputCollector, mockedEventLogger);

        sut.fail(anchor);

        new Verifications() {{
            mockedOutputCollector.fail(anchor); times = 1;
        }};
    }

    @Test
    public void testResetTimeout() throws Exception {
        setupExpectationsForTopologyContextNoEmit();
        sut = new EventLoggingOutputCollector(mockedTopologyContext, mockedOutputCollector, mockedEventLogger);

        sut.resetTimeout(anchor);

        new Verifications() {{
            mockedOutputCollector.resetTimeout(anchor); times = 1;
        }};
    }

    @Test
    public void testReportError() throws Exception {
        setupExpectationsForTopologyContextNoEmit();
        sut = new EventLoggingOutputCollector(mockedTopologyContext, mockedOutputCollector, mockedEventLogger);

        Throwable throwable = new RuntimeException("error");
        sut.reportError(throwable);

        new Verifications() {{
            mockedOutputCollector.reportError(throwable); times = 1;
        }};
    }


    private void setupExpectationsForTopologyContextEmit() {
        new Expectations() {{
            mockedTopologyContext.getComponentId(TASK_1);
            result = TEST_TARGET_COMPONENT_FOR_TASK_1_FOR_STORM;
            mockedTopologyContext.getComponentId(TASK_2);
            result = TEST_TARGET_COMPONENT_FOR_TASK_2_FOR_STORM;

            mockedTopologyContext.getThisComponentId();
            result = TEST_COMPONENT_NAME_FOR_STORM;
        }};
    }

    private void setupExpectationsForTopologyContextEmitDirect() {
        new Expectations() {{
            mockedTopologyContext.getComponentId(TASK_1);
            result = TEST_TARGET_COMPONENT_FOR_TASK_1_FOR_STORM;

            mockedTopologyContext.getThisComponentId();
            result = TEST_COMPONENT_NAME_FOR_STORM;
        }};
    }

    private void setupExpectationsForTopologyContextNoEmit() {
        new Expectations() {{
            mockedTopologyContext.getThisComponentId();
            result = TEST_COMPONENT_NAME_FOR_STORM;
        }};
    }

    private void verifyEventsAreWrittenProperly(StreamlineEvent event, int numTargets) {
        new Verifications() {{
            List<StreamlineEvent> events = new ArrayList<>();
            mockedEventLogger.writeEvent(anyLong, anyString, anyString, anyString, withCapture(events));
            assertEquals(numTargets, events.size());
            assertEquals(createExpectingList(event, numTargets), events);
        }};
    }

    private List<StreamlineEvent> createExpectingList(StreamlineEvent event, int numOccurences) {
        List<StreamlineEvent> events = new ArrayList<>();
        for (int i = 0 ; i < numOccurences ; i++) {
            events.add(event);
        }
        return events;
    }
}