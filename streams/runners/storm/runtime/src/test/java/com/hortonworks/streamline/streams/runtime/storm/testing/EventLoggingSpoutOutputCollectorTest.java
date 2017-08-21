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

package com.hortonworks.streamline.streams.runtime.storm.testing;

import com.google.common.collect.Lists;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class EventLoggingSpoutOutputCollectorTest {
    private static final String TEST_COMPONENT_NAME_FOR_STORM = "1-testComponent";
    private static final String TEST_COMPONENT_NAME = "testComponent";
    private static final String TEST_TARGET_COMPONENT_FOR_TASK_1_FOR_STORM = "2-testTargetComponent1";
    private static final String TEST_TARGET_COMPONENT_FOR_TASK_1 = "testTargetComponent1";
    private static final String TEST_TARGET_COMPONENT_FOR_TASK_2_FOR_STORM = "3-testTargetComponent2";
    private static final String TEST_TARGET_COMPONENT_FOR_TASK_2 = "testTargetComponent2";
    private static final int TASK_1 = 1;
    private static final int TASK_2 = 2;

    @Injectable
    private TestRunEventLogger mockedEventLogger;

    @Injectable
    private TopologyContext mockedTopologyContext;

    @Injectable
    private SpoutOutputCollector mockedOutputCollector;

    private EventLoggingSpoutOutputCollector sut;

    public static final StreamlineEventImpl INPUT_STREAMLINE_EVENT = new StreamlineEventImpl(new HashMap<String, Object>() {{
        put("illuminance", 70);
        put("temp", 104);
        put("foo", 100);
        put("humidity", "40h");
    }}, "ds-" + System.currentTimeMillis(), "id-" + System.currentTimeMillis());

    @Test
    public void emit() throws Exception {
        String testStreamId = "testStreamId";
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);
        String messageId = "testMessageId";
        List<Integer> expectedTasks = Lists.newArrayList(TASK_1, TASK_2);

        setupExpectationsForTopologyContextEmit();
        sut = new EventLoggingSpoutOutputCollector(mockedTopologyContext, mockedOutputCollector, mockedEventLogger);

        // String streamId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, tuple);
            this.result = expectedTasks;
        }};

        List<Integer> tasks = sut.emit(testStreamId, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(testStreamId, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, TEST_TARGET_COMPONENT_FOR_TASK_1, INPUT_STREAMLINE_EVENT);
            times = 1;
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, TEST_TARGET_COMPONENT_FOR_TASK_2, INPUT_STREAMLINE_EVENT);
            times = 1;
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
                    Utils.DEFAULT_STREAM_ID, TEST_TARGET_COMPONENT_FOR_TASK_1, INPUT_STREAMLINE_EVENT);
            times = 1;
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    Utils.DEFAULT_STREAM_ID, TEST_TARGET_COMPONENT_FOR_TASK_2, INPUT_STREAMLINE_EVENT);
            times = 1;
        }};

        // String streamId, List<Object> tuple, Object messageId
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, tuple, messageId);
            result = expectedTasks;
        }};

        tasks = sut.emit(testStreamId, tuple, messageId);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(testStreamId, tuple, messageId);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, TEST_TARGET_COMPONENT_FOR_TASK_1, INPUT_STREAMLINE_EVENT);
            times = 1;
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, TEST_TARGET_COMPONENT_FOR_TASK_2, INPUT_STREAMLINE_EVENT);
            times = 1;
        }};

        // List<Object> tuple, Object messageId
        new Expectations() {{
            mockedOutputCollector.emit(tuple, messageId);
            result = expectedTasks;
        }};

        tasks = sut.emit(tuple, messageId);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(tuple, messageId);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    Utils.DEFAULT_STREAM_ID, TEST_TARGET_COMPONENT_FOR_TASK_1, INPUT_STREAMLINE_EVENT);
            times = 1;
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    Utils.DEFAULT_STREAM_ID, TEST_TARGET_COMPONENT_FOR_TASK_2, INPUT_STREAMLINE_EVENT);
            times = 1;
        }};
    }

    @Test
    public void emitDirect() throws Exception {
        setupExpectationsForTopologyContextEmitDirect();
        sut = new EventLoggingSpoutOutputCollector(mockedTopologyContext, mockedOutputCollector, mockedEventLogger);

        String testStreamId = "testStreamId";
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);
        String messageId = "testMessageId";

        // int taskId, String streamId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(TASK_1, testStreamId, tuple);
        }};

        sut.emitDirect(TASK_1, testStreamId, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(TASK_1, testStreamId, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, TEST_TARGET_COMPONENT_FOR_TASK_1, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // int taskId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(TASK_1, tuple);
        }};

        sut.emitDirect(TASK_1, tuple);

        new Verifications() {{
            mockedOutputCollector.emitDirect(TASK_1, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    Utils.DEFAULT_STREAM_ID, TEST_TARGET_COMPONENT_FOR_TASK_1, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // int taskId, String streamId, List<Object> tuple, Object messageId
        new Expectations() {{
            mockedOutputCollector.emitDirect(TASK_1, testStreamId, tuple, messageId);
        }};

        sut.emitDirect(TASK_1, testStreamId, tuple, messageId);

        new Verifications() {{
            mockedOutputCollector.emitDirect(TASK_1, testStreamId, tuple, messageId);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, TEST_TARGET_COMPONENT_FOR_TASK_1, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // int taskId, List<Object> tuple, Object messageId
        new Expectations() {{
            mockedOutputCollector.emitDirect(TASK_1, tuple, messageId);
        }};

        sut.emitDirect(TASK_1, tuple, messageId);

        new Verifications() {{
            mockedOutputCollector.emitDirect(TASK_1, tuple, messageId);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    Utils.DEFAULT_STREAM_ID, TEST_TARGET_COMPONENT_FOR_TASK_1, INPUT_STREAMLINE_EVENT); times = 1;
        }};
    }

    @Test
    public void reportError() throws Exception {
        setupExpectationsForTopologyContextNoEmit();
        sut = new EventLoggingSpoutOutputCollector(mockedTopologyContext, mockedOutputCollector, mockedEventLogger);

        Throwable throwable = new RuntimeException("error");
        sut.reportError(throwable);

        new Verifications() {{
            mockedOutputCollector.reportError(throwable); times = 1;
        }};
    }

    @Test
    public void getPendingCount() throws Exception {
        setupExpectationsForTopologyContextNoEmit();
        sut = new EventLoggingSpoutOutputCollector(mockedTopologyContext, mockedOutputCollector, mockedEventLogger);

        sut.getPendingCount();

        new Verifications() {{
            mockedOutputCollector.getPendingCount(); times = 1;
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
}