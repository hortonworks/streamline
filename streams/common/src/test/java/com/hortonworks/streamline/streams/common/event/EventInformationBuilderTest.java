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
package com.hortonworks.streamline.streams.common.event;

import com.google.common.collect.Sets;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.common.event.correlation.EventCorrelationInjector;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.*;

public class EventInformationBuilderTest {
    private static final String TEST_COMPONENT_NAME = "testComponent";
    public static final String TEST_TARGET_COMPONENT_NAME = "targetComponentName";
    public static final String TEST_TARGET_COMPONENT_NAME_2 = "targetComponentName2";
    public static final String TEST_STREAM_ID = "streamId";

    private EventInformationBuilder sut;

    @Before
    public void setUp() throws Exception {
        sut = new EventInformationBuilder();
    }

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
    public void testBuild() throws Exception {
        long timestamp = System.currentTimeMillis();

        EventCorrelationInjector injector = new EventCorrelationInjector();

        StreamlineEvent parentEvent = copyEventWithNewID();

        // use same component name for parent and ancestor for easy testing
        // this line is covered via `testNoParent()`
        parentEvent = injector.injectCorrelationInformation(parentEvent, Collections.emptyList(), TEST_COMPONENT_NAME);

        StreamlineEvent injectedEvent = injector.injectCorrelationInformation(
                INPUT_STREAMLINE_EVENT, Collections.singletonList(parentEvent), TEST_COMPONENT_NAME);

        EventInformation information = sut.build(timestamp, TEST_COMPONENT_NAME, TEST_STREAM_ID,
                Sets.newHashSet(TEST_TARGET_COMPONENT_NAME, TEST_TARGET_COMPONENT_NAME_2), injectedEvent);
        assertEquals(timestamp, information.getTimestamp());
        assertEquals(injectedEvent.getId(), information.getEventId());
        assertEquals(EventCorrelationInjector.getRootIds(injectedEvent), information.getRootIds());
        assertEquals(EventCorrelationInjector.getParentIds(injectedEvent), information.getParentIds());
        assertEquals(EventCorrelationInjector.getSourceComponentName(injectedEvent), information.getComponentName());
        assertEquals(TEST_COMPONENT_NAME, information.getComponentName());
        assertEquals(TEST_STREAM_ID, information.getStreamId());
        assertEquals(Sets.newHashSet(TEST_TARGET_COMPONENT_NAME, TEST_TARGET_COMPONENT_NAME_2),
                information.getTargetComponents());
    }

    private StreamlineEventImpl copyEventWithNewID() {
        return StreamlineEventImpl.builder().from(INPUT_STREAMLINE_EVENT).build();
    }

}