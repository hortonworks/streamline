package com.hortonworks.streamline.streams.runtime.storm.testing;

import com.google.common.collect.Lists;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.common.event.correlation.EventCorrelationInjector;
import com.hortonworks.streamline.streams.runtime.utils.StreamlineEventTestUtil;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class EventCorrelatingOutputCollectorTest {
    private static final String TEST_COMPONENT_NAME_FOR_STORM = "1-testComponent";
    private static final String TEST_TARGET_COMPONENT_FOR_TASK_0_FOR_STORM = "0-testTargetComponent0";
    private static final int TASK_0 = 0;
    private static final int TASK_1 = 1;
    private static final int TASK_2 = 2;

    @Injectable
    private TopologyContext mockedTopologyContext;

    @Injectable
    private OutputCollector mockedOutputCollector;

    private StormEventCorrelationInjector mockStormEventCorrelationInjector = new StormEventCorrelationInjector();

    private EventCorrelationInjector eventCorrelationInjector = new EventCorrelationInjector();

    private EventCorrelatingOutputCollector sut;

    public static final StreamlineEventImpl INPUT_STREAMLINE_EVENT = StreamlineEventImpl.builder()
            .fieldsAndValues(new HashMap<String, Object>() {{
                put("illuminance", 70);
                put("temp", 104);
                put("foo", 100);
                put("humidity", "40h");
            }})
            .dataSourceId("ds-" + System.currentTimeMillis())
            .build();

    public static final StreamlineEventImpl PARENT_STREAMLINE_EVENT = StreamlineEventImpl.builder()
            .from(INPUT_STREAMLINE_EVENT)
            .build();

    @Test
    public void emit() throws Exception {
        setupExpectationsForTuple();
        setupExpectationsForTopologyContextEmit();
        setupExpectationsForEventCorrelationInjector();

        sut = new EventCorrelatingOutputCollector(mockedTopologyContext, mockedOutputCollector);
        Deencapsulation.setField(sut, "eventCorrelationInjector", mockStormEventCorrelationInjector);

        StreamlineEvent parentEvent = eventCorrelationInjector.injectCorrelationInformation(
                PARENT_STREAMLINE_EVENT, Collections.emptyList(), TEST_COMPONENT_NAME_FOR_STORM);
        Tuple anchor = new TupleImpl(mockedTopologyContext, new Values(parentEvent), TASK_0,
                Utils.DEFAULT_STREAM_ID);

        String testStreamId = "testStreamId";
        List<Integer> expectedTasks = Lists.newArrayList(TASK_1, TASK_2);
        final List<Tuple> anchors = Collections.singletonList(anchor);
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);

        // String streamId, Tuple anchor, List<Object> anchor
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, anchor, withAny(tuple));
            result = expectedTasks;
        }};

        List<Integer> tasks = sut.emit(testStreamId, anchor, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emit(testStreamId, anchor, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockStormEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(1, capturedParents.size());
            assertEquals(anchor, capturedParents.get(0));
        }};

        // String streamId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, withAny(tuple));
            result = expectedTasks;
        }};

        tasks = sut.emit(testStreamId, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emit(testStreamId, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockStormEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(0, capturedParents.size());
        }};

        // Collection<Tuple> anchors, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(anchors, withAny(tuple));
            result = expectedTasks;
        }};

        tasks = sut.emit(anchors, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emit(anchors, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockStormEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(1, capturedParents.size());
            assertEquals(anchor, capturedParents.get(0));
        }};

        // Tuple anchor, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(anchor, withAny(tuple));
            result = expectedTasks;
        }};

        tasks = sut.emit(anchor, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emit(anchor, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockStormEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(1, capturedParents.size());
            assertEquals(anchor, capturedParents.get(0));
        }};

        // List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(withAny(tuple));
            result = expectedTasks;
        }};

        tasks = sut.emit(tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emit(capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockStormEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(0, capturedParents.size());
        }};

        // String streamId, Collection<Tuple> anchors, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, anchors, withAny(tuple));
            result = expectedTasks;
        }};

        tasks = sut.emit(testStreamId, anchors, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emit(testStreamId, anchors, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockStormEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(1, capturedParents.size());
            assertEquals(anchor, capturedParents.get(0));
        }};

    }

    @Test
    public void emitDirect() throws Exception {
        setupExpectationsForTuple();
        setupExpectationsForTopologyContextEmit();
        setupExpectationsForEventCorrelationInjector();

        sut = new EventCorrelatingOutputCollector(mockedTopologyContext, mockedOutputCollector);
        Deencapsulation.setField(sut, "eventCorrelationInjector", mockStormEventCorrelationInjector);

        StreamlineEvent parentEvent = eventCorrelationInjector.injectCorrelationInformation(
                PARENT_STREAMLINE_EVENT, Collections.emptyList(), TEST_COMPONENT_NAME_FOR_STORM);
        Tuple anchor = new TupleImpl(mockedTopologyContext, new Values(parentEvent), TASK_0,
                Utils.DEFAULT_STREAM_ID);

        int testTaskId = TASK_1;
        String testStreamId = "testStreamId";
        final List<Tuple> anchors = Collections.singletonList(anchor);
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);

        // int taskId, String streamId, Tuple anchor, List<Object> anchor
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, anchor, withAny(tuple));
        }};

        sut.emitDirect(testTaskId, testStreamId, anchor, tuple);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, anchor, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockStormEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(1, capturedParents.size());
            assertEquals(anchor, capturedParents.get(0));
        }};

        // int taskId, String streamId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, withAny(tuple));
        }};

        sut.emitDirect(testTaskId, testStreamId, tuple);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockStormEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(0, capturedParents.size());
        }};

        // int taskId, Collection<Tuple> anchors, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, anchors, withAny(tuple));
        }};

        sut.emitDirect(testTaskId, anchors, tuple);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emitDirect(testTaskId, anchors, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockStormEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(1, capturedParents.size());
            assertEquals(anchor, capturedParents.get(0));
        }};

        // int taskId, Tuple anchor, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, anchor, withAny(tuple));
        }};

        sut.emitDirect(testTaskId, anchor, tuple);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emitDirect(testTaskId, anchor, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockStormEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(1, capturedParents.size());
            assertEquals(anchor, capturedParents.get(0));
        }};

        // int taskId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, withAny(tuple));
        }};

        sut.emitDirect(testTaskId, tuple);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emitDirect(testTaskId, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockStormEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(0, capturedParents.size());
        }};

        // int taskId, String streamId, Collection<Tuple> anchors, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, anchors, withAny(tuple));
        }};

        sut.emitDirect(testTaskId, testStreamId, anchors, tuple);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, anchors, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockStormEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(1, capturedParents.size());
            assertEquals(anchor, capturedParents.get(0));
        }};

    }

    @Test
    public void testAck() throws Exception {
        setupExpectationsForTuple();
        setupExpectationsForTopologyContextNoEmit();
        sut = new EventCorrelatingOutputCollector(mockedTopologyContext, mockedOutputCollector);

        Tuple anchor = new TupleImpl(mockedTopologyContext, new Values(PARENT_STREAMLINE_EVENT), TASK_0,
                Utils.DEFAULT_STREAM_ID);

        sut.ack(anchor);

        new Verifications() {{
            mockedOutputCollector.ack(anchor); times = 1;
        }};
    }

    @Test
    public void testFail() throws Exception {
        setupExpectationsForTuple();
        setupExpectationsForTopologyContextNoEmit();
        sut = new EventCorrelatingOutputCollector(mockedTopologyContext, mockedOutputCollector);

        Tuple anchor = new TupleImpl(mockedTopologyContext, new Values(PARENT_STREAMLINE_EVENT), TASK_0,
                Utils.DEFAULT_STREAM_ID);

        sut.fail(anchor);

        new Verifications() {{
            mockedOutputCollector.fail(anchor); times = 1;
        }};
    }

    @Test
    public void testResetTimeout() throws Exception {
        setupExpectationsForTuple();
        setupExpectationsForTopologyContextNoEmit();
        sut = new EventCorrelatingOutputCollector(mockedTopologyContext, mockedOutputCollector);

        Tuple anchor = new TupleImpl(mockedTopologyContext, new Values(PARENT_STREAMLINE_EVENT), TASK_0,
                Utils.DEFAULT_STREAM_ID);

        sut.resetTimeout(anchor);

        new Verifications() {{
            mockedOutputCollector.resetTimeout(anchor); times = 1;
        }};
    }

    @Test
    public void testReportError() throws Exception {
        setupExpectationsForTopologyContextNoEmit();
        sut = new EventCorrelatingOutputCollector(mockedTopologyContext, mockedOutputCollector);

        Throwable throwable = new RuntimeException("error");
        sut.reportError(throwable);

        new Verifications() {{
            mockedOutputCollector.reportError(throwable); times = 1;
        }};
    }

    private void setupExpectationsForTuple() {
        new Expectations() {{
            mockedTopologyContext.getComponentId(TASK_0);
            result = TEST_TARGET_COMPONENT_FOR_TASK_0_FOR_STORM;

            mockedTopologyContext.getComponentOutputFields(TEST_TARGET_COMPONENT_FOR_TASK_0_FOR_STORM, anyString);
            result = new Fields(StreamlineEvent.STREAMLINE_EVENT);
        }};
    }

    private void setupExpectationsForTopologyContextEmit() {
        new Expectations() {{
            mockedTopologyContext.getThisComponentId();
            result = TEST_COMPONENT_NAME_FOR_STORM;
        }};
    }

    private void setupExpectationsForTopologyContextNoEmit() {
        new Expectations() {{
        }};
    }

    private void setupExpectationsForEventCorrelationInjector() {
        new Expectations(mockStormEventCorrelationInjector) {{
        }};
    }

}