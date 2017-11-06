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

package com.hortonworks.streamline.streams.common.event.correlation;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.common.event.util.StreamlineEventTestUtil;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.*;

@RunWith(JMockit.class)
public class EventCorrelationInjectorTest {

    private static final String TEST_COMPONENT_NAME = "testComponent";

    private EventCorrelationInjector sut = new EventCorrelationInjector();

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
    public void eventArgsTestNoParent() throws Exception {
        StreamlineEvent injectedEvent = sut.injectCorrelationInformation(
                INPUT_STREAMLINE_EVENT, Collections.emptyList(), TEST_COMPONENT_NAME);

        StreamlineEventTestUtil.assertEventIsProperlyCopied(injectedEvent, INPUT_STREAMLINE_EVENT);

        // added headers
        // if rootIds is empty itself is the root event
        assertEquals(Collections.emptySet(), EventCorrelationInjector.getRootIds(injectedEvent));
        assertEquals(Collections.emptySet(), EventCorrelationInjector.getParentIds(injectedEvent));
        assertEquals(TEST_COMPONENT_NAME, EventCorrelationInjector.getSourceComponentName(injectedEvent));
    }

    @Test
    public void eventArgsTestWithAParentWhichIsRootEvent() throws Exception {
        StreamlineEvent parentEvent = copyEventWithNewID();

        // use same component name for parent and ancestor for easy testing
        // this line is covered via `testNoParent()`
        parentEvent = sut.injectCorrelationInformation(parentEvent, Collections.emptyList(), TEST_COMPONENT_NAME);

        StreamlineEvent injectedEvent = sut.injectCorrelationInformation(
                INPUT_STREAMLINE_EVENT, Collections.singletonList(parentEvent), TEST_COMPONENT_NAME);

        StreamlineEventTestUtil.assertEventIsProperlyCopied(injectedEvent, INPUT_STREAMLINE_EVENT);

        // added headers
        assertEquals(Collections.singleton(parentEvent.getId()), EventCorrelationInjector.getRootIds(injectedEvent));
        assertEquals(Collections.singleton(parentEvent.getId()), EventCorrelationInjector.getParentIds(injectedEvent));
        assertEquals(TEST_COMPONENT_NAME, EventCorrelationInjector.getSourceComponentName(injectedEvent));
    }

    @Test
    public void eventArgsTestWithTwoParentsWhichAreRootEvents() throws Exception {
        StreamlineEvent parentEvent1 = copyEventWithNewID();
        // this line is covered via `testNoParent()`
        parentEvent1 = sut.injectCorrelationInformation(parentEvent1, Collections.emptyList(), TEST_COMPONENT_NAME);

        StreamlineEvent parentEvent2 = copyEventWithNewID();
        // this line is covered via `testNoParent()`
        parentEvent2 = sut.injectCorrelationInformation(parentEvent2, Collections.emptyList(), TEST_COMPONENT_NAME);

        StreamlineEvent injectedEvent = sut.injectCorrelationInformation(
                INPUT_STREAMLINE_EVENT, Lists.newArrayList(parentEvent1, parentEvent2), TEST_COMPONENT_NAME);

        StreamlineEventTestUtil.assertEventIsProperlyCopied(injectedEvent, INPUT_STREAMLINE_EVENT);

        // added headers
        assertEquals(Sets.newHashSet(parentEvent1.getId(), parentEvent2.getId()), EventCorrelationInjector.getRootIds(injectedEvent));
        assertEquals(Sets.newHashSet(parentEvent1.getId(), parentEvent2.getId()), EventCorrelationInjector.getParentIds(injectedEvent));
        assertEquals(TEST_COMPONENT_NAME, EventCorrelationInjector.getSourceComponentName(injectedEvent));
    }

    @Test
    public void eventArgsTestWithTwoParentsWhichOneIsRootEventAndAnotherOneIsNonRootEvent() throws Exception {
        StreamlineEvent parentEvent1 = copyEventWithNewID();
        // this line is covered via `testNoParent()`
        parentEvent1 = sut.injectCorrelationInformation(parentEvent1, Collections.emptyList(), TEST_COMPONENT_NAME);

        StreamlineEvent parentOfParentEvent2 = copyEventWithNewID();
        // this line is covered via `testNoParent()`
        parentOfParentEvent2 = sut.injectCorrelationInformation(parentOfParentEvent2, Collections.emptyList(), TEST_COMPONENT_NAME);

        StreamlineEvent parentEvent2 = copyEventWithNewID();
        // this line is covered via `testWithAParentWhichIsRootEvent()`
        parentEvent2 = sut.injectCorrelationInformation(parentEvent2, Collections.singletonList(parentOfParentEvent2), TEST_COMPONENT_NAME);

        StreamlineEvent injectedEvent = sut.injectCorrelationInformation(INPUT_STREAMLINE_EVENT,
                Lists.newArrayList(parentEvent1, parentEvent2), TEST_COMPONENT_NAME);

        StreamlineEventTestUtil.assertEventIsProperlyCopied(injectedEvent, INPUT_STREAMLINE_EVENT);

        // added headers
        assertEquals(Sets.newHashSet(parentEvent1.getId(), parentOfParentEvent2.getId()), EventCorrelationInjector.getRootIds(injectedEvent));
        assertEquals(Sets.newHashSet(parentEvent1.getId(), parentEvent2.getId()), EventCorrelationInjector.getParentIds(injectedEvent));
        assertEquals(TEST_COMPONENT_NAME, EventCorrelationInjector.getSourceComponentName(injectedEvent));
    }

    private StreamlineEventImpl copyEventWithNewID() {
        return StreamlineEventImpl.builder().from(INPUT_STREAMLINE_EVENT).build();
    }

}