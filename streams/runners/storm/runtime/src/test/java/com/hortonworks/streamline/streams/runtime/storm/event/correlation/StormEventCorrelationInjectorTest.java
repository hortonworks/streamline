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
package com.hortonworks.streamline.streams.runtime.storm.event.correlation;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.common.event.correlation.EventCorrelationInjector;
import com.hortonworks.streamline.streams.runtime.storm.event.correlation.StormEventCorrelationInjector;
import com.hortonworks.streamline.streams.runtime.utils.StreamlineEventTestUtil;
import com.hortonworks.streamline.streams.storm.common.StormTopologyUtil;
import mockit.Expectations;
import mockit.Injectable;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.tuple.Values;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class StormEventCorrelationInjectorTest {

    private static final String TEST_COMPONENT_NAME = "1-testComponent";

    private StormEventCorrelationInjector sut = new StormEventCorrelationInjector();
    private EventCorrelationInjector eventCorrelationInjector = new EventCorrelationInjector();

    @Injectable
    private TopologyContext mockedTopologyContext;

    public static final StreamlineEventImpl INPUT_STREAMLINE_EVENT = StreamlineEventImpl.builder()
            .fieldsAndValues(new HashMap<String, Object>() {{
                put("illuminance", 70);
                put("temp", 104);
                put("foo", 100);
                put("humidity", "40h");
            }})
            .dataSourceId("ds-" + System.currentTimeMillis())
            .header(Collections.singletonMap("A", 1))
            .build();

    @Test
    public void tupleArgsTestNoParent() throws Exception {
        StreamlineEvent injectedEvent = sut.injectCorrelationInformation(
                new Values(INPUT_STREAMLINE_EVENT), Collections.emptyList(), TEST_COMPONENT_NAME);

        StreamlineEventTestUtil.assertEventIsProperlyCopied(injectedEvent, INPUT_STREAMLINE_EVENT);

        // added headers
        assertEquals(Collections.emptySet(), EventCorrelationInjector.getRootIds(injectedEvent));
        assertEquals(Collections.emptySet(), EventCorrelationInjector.getParentIds(injectedEvent));
        assertEquals(StormTopologyUtil.extractStreamlineComponentName(TEST_COMPONENT_NAME), EventCorrelationInjector.getSourceComponentName(injectedEvent));
    }

    @Test
    public void tupleArgsTestWithAParentWhichIsRootEvent() throws Exception {
        StreamlineEvent parentEvent = copyEventWithNewID();

        // use same component name for parent and ancestor for easy testing
        parentEvent = eventCorrelationInjector.injectCorrelationInformation(parentEvent, Collections.emptyList(), TEST_COMPONENT_NAME);

        int parentTaskId = 1;
        String parentComponentName = "1-parentComponent";

        new Expectations() {{
            mockedTopologyContext.getComponentId(parentTaskId);
            result = parentComponentName;

            mockedTopologyContext.getComponentOutputFields(parentComponentName, INPUT_STREAMLINE_EVENT.getSourceStream());
            result = new Fields(StreamlineEvent.STREAMLINE_EVENT);
        }};

        Tuple parentTuple = new TupleImpl(mockedTopologyContext, new Values(parentEvent), parentTaskId,
                INPUT_STREAMLINE_EVENT.getSourceStream());

        StreamlineEvent injectedEvent = sut.injectCorrelationInformation(
                new Values(INPUT_STREAMLINE_EVENT), Collections.singletonList(parentTuple), TEST_COMPONENT_NAME);

        StreamlineEventTestUtil.assertEventIsProperlyCopied(injectedEvent, INPUT_STREAMLINE_EVENT);

        // added headers
        assertEquals(Collections.singleton(parentEvent.getId()), EventCorrelationInjector.getRootIds(injectedEvent));
        assertEquals(Collections.singleton(parentEvent.getId()), EventCorrelationInjector.getParentIds(injectedEvent));
        assertEquals(StormTopologyUtil.extractStreamlineComponentName(TEST_COMPONENT_NAME), EventCorrelationInjector.getSourceComponentName(injectedEvent));
    }

    @Test
    public void tupleArgsTestWithTwoParentsWhichAreRootEvents() throws Exception {
        int parent1TaskId = 1;
        String parent1ComponentName = "1-parentComponent";
        int parent2TaskId = 2;
        String parent2ComponentName = "2-parentComponent2";

        new Expectations() {{
            mockedTopologyContext.getComponentId(parent1TaskId);
            result = parent1ComponentName;
            mockedTopologyContext.getComponentId(parent2TaskId);
            result = parent2ComponentName;

            mockedTopologyContext.getComponentOutputFields(parent1ComponentName, INPUT_STREAMLINE_EVENT.getSourceStream());
            result = new Fields(StreamlineEvent.STREAMLINE_EVENT);
            mockedTopologyContext.getComponentOutputFields(parent2ComponentName, INPUT_STREAMLINE_EVENT.getSourceStream());
            result = new Fields(StreamlineEvent.STREAMLINE_EVENT);
        }};

        StreamlineEvent parentEvent1 = copyEventWithNewID();

        parentEvent1 = eventCorrelationInjector.injectCorrelationInformation(parentEvent1, Collections.emptyList(), TEST_COMPONENT_NAME);

        Tuple parentTuple1 = new TupleImpl(mockedTopologyContext, new Values(parentEvent1), parent1TaskId,
                INPUT_STREAMLINE_EVENT.getSourceStream());

        StreamlineEvent parentEvent2 = copyEventWithNewID();
        parentEvent2 = eventCorrelationInjector.injectCorrelationInformation(parentEvent2, Collections.emptyList(), TEST_COMPONENT_NAME);

        Tuple parentTuple2 = new TupleImpl(mockedTopologyContext, new Values(parentEvent2), parent2TaskId,
                INPUT_STREAMLINE_EVENT.getSourceStream());

        StreamlineEvent injectedEvent = sut.injectCorrelationInformation(
                new Values(INPUT_STREAMLINE_EVENT),
                Lists.newArrayList(parentTuple1, parentTuple2), TEST_COMPONENT_NAME);

        StreamlineEventTestUtil.assertEventIsProperlyCopied(injectedEvent, INPUT_STREAMLINE_EVENT);

        // added headers
        assertEquals(Sets.newHashSet(parentEvent1.getId(), parentEvent2.getId()), EventCorrelationInjector.getRootIds(injectedEvent));
        assertEquals(Sets.newHashSet(parentEvent1.getId(), parentEvent2.getId()), EventCorrelationInjector.getParentIds(injectedEvent));
        assertEquals(StormTopologyUtil.extractStreamlineComponentName(TEST_COMPONENT_NAME), EventCorrelationInjector.getSourceComponentName(injectedEvent));
    }

    @Test
    public void tupleArgsTestWithTwoParentsWhichOneIsRootEventAndAnotherOneIsNonRootEvent() throws Exception {
        int parent1TaskId = 1;
        String parent1ComponentName = "1-parentComponent";
        int parent2TaskId = 2;
        String parent2ComponentName = "2-parentComponent2";

        new Expectations() {{
            mockedTopologyContext.getComponentId(parent1TaskId);
            result = parent1ComponentName;
            mockedTopologyContext.getComponentId(parent2TaskId);
            result = parent2ComponentName;

            mockedTopologyContext.getComponentOutputFields(parent1ComponentName, INPUT_STREAMLINE_EVENT.getSourceStream());
            result = new Fields(StreamlineEvent.STREAMLINE_EVENT);
            mockedTopologyContext.getComponentOutputFields(parent2ComponentName, INPUT_STREAMLINE_EVENT.getSourceStream());
            result = new Fields(StreamlineEvent.STREAMLINE_EVENT);
        }};

        StreamlineEvent parentEvent1 = copyEventWithNewID();
        parentEvent1 = eventCorrelationInjector.injectCorrelationInformation(parentEvent1, Collections.emptyList(), TEST_COMPONENT_NAME);

        Tuple parentTuple1 = new TupleImpl(mockedTopologyContext, new Values(parentEvent1), parent1TaskId,
                INPUT_STREAMLINE_EVENT.getSourceStream());

        StreamlineEvent parentOfParentEvent2 = copyEventWithNewID();
        parentOfParentEvent2 = eventCorrelationInjector.injectCorrelationInformation(parentOfParentEvent2, Collections.emptyList(), TEST_COMPONENT_NAME);

        StreamlineEvent parentEvent2 = copyEventWithNewID();
        parentEvent2 = eventCorrelationInjector.injectCorrelationInformation(parentEvent2, Collections.singletonList(parentOfParentEvent2), TEST_COMPONENT_NAME);

        Tuple parentTuple2 = new TupleImpl(mockedTopologyContext, new Values(parentEvent2), parent2TaskId,
                INPUT_STREAMLINE_EVENT.getSourceStream());

        StreamlineEvent injectedEvent = sut.injectCorrelationInformation(
                new Values(INPUT_STREAMLINE_EVENT),
                Lists.newArrayList(parentTuple1, parentTuple2), TEST_COMPONENT_NAME);

        StreamlineEventTestUtil.assertEventIsProperlyCopied(injectedEvent, INPUT_STREAMLINE_EVENT);

        // added headers
        assertEquals(Sets.newHashSet(parentEvent1.getId(), parentOfParentEvent2.getId()), EventCorrelationInjector.getRootIds(injectedEvent));
        assertEquals(Sets.newHashSet(parentEvent1.getId(), parentEvent2.getId()), EventCorrelationInjector.getParentIds(injectedEvent));
        assertEquals(StormTopologyUtil.extractStreamlineComponentName(TEST_COMPONENT_NAME), EventCorrelationInjector.getSourceComponentName(injectedEvent));
    }

    private StreamlineEventImpl copyEventWithNewID() {
        return StreamlineEventImpl.builder().from(INPUT_STREAMLINE_EVENT).build();
    }

}