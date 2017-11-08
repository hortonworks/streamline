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
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.runtime.utils.StreamlineEventTestUtil;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class EventCorrelatingSpoutOutputCollectorTest {
    private static final String TEST_COMPONENT_NAME_FOR_STORM = "1-testComponent";
    private static final int TASK_1 = 1;
    private static final int TASK_2 = 2;

    @Injectable
    private TopologyContext mockedTopologyContext;

    private StormEventCorrelationInjector mockEventCorrelationInjector = new StormEventCorrelationInjector();

    @Injectable
    private SpoutOutputCollector mockedOutputCollector;

    private EventCorrelatingSpoutOutputCollector sut;

    public static final StreamlineEventImpl INPUT_STREAMLINE_EVENT = StreamlineEventImpl.builder()
            .fieldsAndValues(new HashMap<String, Object>() {{
                put("illuminance", 70);
                put("temp", 104);
                put("foo", 100);
                put("humidity", "40h");
            }})
            .dataSourceId("ds-" + System.currentTimeMillis())
            .build();

    @Test
    public void emit() throws Exception {
        String testStreamId = "testStreamId";
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);
        String messageId = "testMessageId";
        List<Integer> expectedTasks = Lists.newArrayList(TASK_1, TASK_2);

        setupExpectationsForTopologyContextEmit();
        setupExpectationsForEventCorrelationInjector();

        sut = new EventCorrelatingSpoutOutputCollector(mockedTopologyContext, mockedOutputCollector);
        Deencapsulation.setField(sut, "eventCorrelationInjector", mockEventCorrelationInjector);

        // String streamId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, withAny(tuple));
            this.result = expectedTasks;
        }};

        List<Integer> tasks = sut.emit(testStreamId, tuple);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emit(testStreamId, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(0, capturedParents.size());
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
            mockEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(0, capturedParents.size());
        }};

        // String streamId, List<Object> tuple, Object messageId
        new Expectations() {{
            mockedOutputCollector.emit(testStreamId, withAny(tuple), messageId);
            result = expectedTasks;
        }};

        tasks = sut.emit(testStreamId, tuple, messageId);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emit(testStreamId, capturedValues = withCapture(), messageId);
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(0, capturedParents.size());
        }};

        // List<Object> tuple, Object messageId
        new Expectations() {{
            mockedOutputCollector.emit(withAny(tuple), messageId);
            result = expectedTasks;
        }};

        tasks = sut.emit(tuple, messageId);
        assertEquals(expectedTasks, tasks);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emit(capturedValues = withCapture(), messageId);
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(0, capturedParents.size());
        }};
    }

    @Test
    public void emitDirect() throws Exception {
        setupExpectationsForTopologyContextEmit();
        setupExpectationsForEventCorrelationInjector();

        sut = new EventCorrelatingSpoutOutputCollector(mockedTopologyContext, mockedOutputCollector);
        Deencapsulation.setField(sut, "eventCorrelationInjector", mockEventCorrelationInjector);

        String testStreamId = "testStreamId";
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);
        String messageId = "testMessageId";

        // int taskId, String streamId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(TASK_1, testStreamId, withAny(tuple));
        }};

        sut.emitDirect(TASK_1, testStreamId, tuple);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emitDirect(TASK_1, testStreamId, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(0, capturedParents.size());
        }};

        // int taskId, List<Object> tuple
        new Expectations() {{
            mockedOutputCollector.emitDirect(TASK_1, withAny(tuple));
        }};

        sut.emitDirect(TASK_1, tuple);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emitDirect(TASK_1, capturedValues = withCapture());
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(0, capturedParents.size());
        }};

        // int taskId, String streamId, List<Object> tuple, Object messageId
        new Expectations() {{
            mockedOutputCollector.emitDirect(TASK_1, testStreamId, withAny(tuple), messageId);
        }};

        sut.emitDirect(TASK_1, testStreamId, tuple, messageId);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emitDirect(TASK_1, testStreamId, capturedValues = withCapture(), messageId);
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(0, capturedParents.size());
        }};

        // int taskId, List<Object> tuple, Object messageId
        new Expectations() {{
            mockedOutputCollector.emitDirect(TASK_1, withAny(tuple), messageId);
        }};

        sut.emitDirect(TASK_1, tuple, messageId);

        new Verifications() {{
            List<Object> capturedValues;
            mockedOutputCollector.emitDirect(TASK_1, capturedValues = withCapture(), messageId);
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) capturedValues.get(0), INPUT_STREAMLINE_EVENT);

            List<Tuple> capturedParents;
            mockEventCorrelationInjector.injectCorrelationInformation(tuple,
                    capturedParents = withCapture(), TEST_COMPONENT_NAME_FOR_STORM);
            assertEquals(0, capturedParents.size());
        }};
    }

    @Test
    public void reportError() throws Exception {
        setupExpectationsForTopologyContextNoEmit();
        setupExpectationsForEventCorrelationInjector();

        sut = new EventCorrelatingSpoutOutputCollector(mockedTopologyContext, mockedOutputCollector);
        Deencapsulation.setField(sut, "eventCorrelationInjector", mockEventCorrelationInjector);

        Throwable throwable = new RuntimeException("error");
        sut.reportError(throwable);

        new Verifications() {{
            mockedOutputCollector.reportError(throwable); times = 1;
        }};
    }

    @Test
    public void getPendingCount() throws Exception {
        setupExpectationsForTopologyContextNoEmit();
        setupExpectationsForEventCorrelationInjector();

        sut = new EventCorrelatingSpoutOutputCollector(mockedTopologyContext, mockedOutputCollector);
        Deencapsulation.setField(sut, "eventCorrelationInjector", mockEventCorrelationInjector);

        sut.getPendingCount();

        new Verifications() {{
            mockedOutputCollector.getPendingCount(); times = 1;
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
        new Expectations(mockEventCorrelationInjector) {{
        }};
    }

}