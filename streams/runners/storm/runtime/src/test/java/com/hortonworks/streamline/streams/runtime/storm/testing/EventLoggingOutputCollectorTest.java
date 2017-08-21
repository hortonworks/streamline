package com.hortonworks.streamline.streams.runtime.storm.testing;

import com.google.common.collect.Lists;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
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
    private static final String TEST_COMPONENT_NAME = "testComponent";

    @Injectable
    private TestRunEventLogger mockedEventLogger;

    @Injectable
    private OutputCollector mockedOutputCollector;

    @Injectable
    private Tuple anchor;

    private Collection<Tuple> anchors;

    private EventLoggingOutputCollector sut;

    public static final StreamlineEventImpl INPUT_STREAMLINE_EVENT = new StreamlineEventImpl(new HashMap<String, Object>() {{
        put("illuminance", 70);
        put("temp", 104);
        put("foo", 100);
        put("humidity", "40h");
    }}, "ds-" + System.currentTimeMillis(), "id-" + System.currentTimeMillis());

    @Before
    public void setUp() {
        sut = new EventLoggingOutputCollector(mockedOutputCollector, TEST_COMPONENT_NAME, mockedEventLogger);
        anchors = Collections.singletonList(anchor);
    }

    @Test
    public void emit() throws Exception {
        String testStreamId = "testStreamId";
        ArrayList<Integer> expectedTasks = Lists.newArrayList(1, 2);
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
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // String streamId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, tuple);
            result = expectedTasks;
        }};

        tasks = sut.emit(testStreamId, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(testStreamId, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // Collection<Tuple> anchors, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(anchors, tuple);
            result = expectedTasks;
        }};

        tasks = sut.emit(anchors, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(anchors, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    Utils.DEFAULT_STREAM_ID, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // Tuple anchor, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(anchor, tuple);
            result = expectedTasks;
        }};

        tasks = sut.emit(anchor, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(anchor, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    Utils.DEFAULT_STREAM_ID, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(tuple);
            result = expectedTasks;
        }};

        tasks = sut.emit(tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    Utils.DEFAULT_STREAM_ID, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // String streamId, Collection<Tuple> anchors, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, anchors, tuple);
            result = expectedTasks;
        }};

        tasks = sut.emit(testStreamId, anchors, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(testStreamId, anchors, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, INPUT_STREAMLINE_EVENT); times = 1;
        }};

    }

    @Test
    public void emitDirect() throws Exception {
        int testTaskId = 1;
        String testStreamId = "testStreamId";
        ArrayList<Integer> expectedTasks = Lists.newArrayList(1, 2);
        final List<Tuple> anchors = Collections.singletonList(anchor);
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);

        // int taskId, String streamId, Tuple anchor, List<Object> anchor
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, anchor, tuple);
            result = expectedTasks;
        }};

        sut.emitDirect(testTaskId, testStreamId, anchor, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, anchor, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // int taskId, String streamId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, tuple);
            result = expectedTasks;
        }};

        sut.emitDirect(testTaskId, testStreamId, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // int taskId, Collection<Tuple> anchors, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, anchors, tuple);
            result = expectedTasks;
        }};

        sut.emitDirect(testTaskId, anchors, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, anchors, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    Utils.DEFAULT_STREAM_ID, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // int taskId, Tuple anchor, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, anchor, tuple);
            result = expectedTasks;
        }};

        sut.emitDirect(testTaskId, anchor, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, anchor, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    Utils.DEFAULT_STREAM_ID, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // int taskId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, tuple);
            result = expectedTasks;
        }};

        sut.emitDirect(testTaskId, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    Utils.DEFAULT_STREAM_ID, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // int taskId, String streamId, Collection<Tuple> anchors, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, anchors, tuple);
            result = expectedTasks;
        }};

        sut.emitDirect(testTaskId, testStreamId, anchors, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, anchors, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, INPUT_STREAMLINE_EVENT); times = 1;
        }};

    }

    @Test
    public void testAck() throws Exception {
        sut.ack(anchor);

        new Verifications() {{
            mockedOutputCollector.ack(anchor); times = 1;
        }};
    }

    @Test
    public void testFail() throws Exception {
        sut.fail(anchor);

        new Verifications() {{
            mockedOutputCollector.fail(anchor); times = 1;
        }};
    }

    @Test
    public void testResetTimeout() throws Exception {
        sut.resetTimeout(anchor);

        new Verifications() {{
            mockedOutputCollector.resetTimeout(anchor); times = 1;
        }};
    }

    @Test
    public void testReportError() throws Exception {
        Throwable throwable = new RuntimeException("error");
        sut.reportError(throwable);

        new Verifications() {{
            mockedOutputCollector.reportError(throwable); times = 1;
        }};
    }
}