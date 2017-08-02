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
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class EventLoggingSpoutOutputCollectorTest {
    private static final String TEST_COMPONENT_NAME = "testComponent";

    @Injectable
    private TestRunEventLogger mockedEventLogger;

    @Injectable
    private SpoutOutputCollector mockedOutputCollector;

    private EventLoggingSpoutOutputCollector sut;

    public static final StreamlineEventImpl INPUT_STREAMLINE_EVENT = new StreamlineEventImpl(new HashMap<String, Object>() {{
        put("illuminance", 70);
        put("temp", 104);
        put("foo", 100);
        put("humidity", "40h");
    }}, "ds-" + System.currentTimeMillis(), "id-" + System.currentTimeMillis());

    @Before
    public void setUp() {
        sut = new EventLoggingSpoutOutputCollector(mockedOutputCollector, TEST_COMPONENT_NAME, mockedEventLogger);
    }

    @Test
    public void emit() throws Exception {
        String testStreamId = "testStreamId";
        ArrayList<Integer> expectedTasks = Lists.newArrayList(1, 2);
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);
        String messageId = "testMessageId";

        // String streamId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, tuple);
            result = expectedTasks;
        }};

        List<Integer> tasks = sut.emit(testStreamId, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            mockedOutputCollector.emit(testStreamId, tuple);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, INPUT_STREAMLINE_EVENT); times = 1;
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
                    testStreamId, INPUT_STREAMLINE_EVENT); times = 1;
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
                    Utils.DEFAULT_STREAM_ID, INPUT_STREAMLINE_EVENT); times = 1;
        }};
    }

    @Test
    public void emitDirect() throws Exception {
        int testTaskId = 1;
        String testStreamId = "testStreamId";
        ArrayList<Integer> expectedTasks = Lists.newArrayList(1, 2);
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);
        String messageId = "testMessageId";

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

        // int taskId, String streamId, List<Object> tuple, Object messageId
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, tuple, messageId);
            result = expectedTasks;
        }};

        sut.emitDirect(testTaskId, testStreamId, tuple, messageId);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, testStreamId, tuple, messageId);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    testStreamId, INPUT_STREAMLINE_EVENT); times = 1;
        }};

        // int taskId, List<Object> tuple, Object messageId
        new Expectations() {{
            mockedOutputCollector.emitDirect(testTaskId, tuple, messageId);
            result = expectedTasks;
        }};

        sut.emitDirect(testTaskId, tuple, messageId);

        new Verifications() {{
            mockedOutputCollector.emitDirect(testTaskId, tuple, messageId);
            mockedEventLogger.writeEvent(anyLong, TestRunEventLogger.EventType.OUTPUT, TEST_COMPONENT_NAME,
                    Utils.DEFAULT_STREAM_ID, INPUT_STREAMLINE_EVENT); times = 1;
        }};
    }

    @Test
    public void reportError() throws Exception {
        Throwable throwable = new RuntimeException("error");
        sut.reportError(throwable);

        new Verifications() {{
            mockedOutputCollector.reportError(throwable); times = 1;
        }};
    }

    @Test
    public void getPendingCount() throws Exception {
        sut.getPendingCount();

        new Verifications() {{
            mockedOutputCollector.getPendingCount(); times = 1;
        }};
    }

}