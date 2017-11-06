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
package com.hortonworks.streamline.streams.common.event.util;

import com.hortonworks.streamline.streams.StreamlineEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public final class StreamlineEventTestUtil {

    public static void assertEventIsProperlyCopied(StreamlineEvent newEvent, StreamlineEvent sourceEvent) {
        // key-value
        assertEquals(newEvent.entrySet(), sourceEvent.entrySet());

        // header
        sourceEvent.getHeader().forEach((k, v) -> {
            assertTrue(newEvent.getHeader().containsKey(k));
            assertEquals(v, newEvent.getHeader().get(k));
        });

        // other fields
        assertEquals(newEvent.getDataSourceId(), sourceEvent.getDataSourceId());
        assertEquals(newEvent.getSourceStream(), sourceEvent.getSourceStream());
        assertEquals(newEvent.getAuxiliaryFieldsAndValues(), sourceEvent.getAuxiliaryFieldsAndValues());

        // id should be different
        assertNotEquals(newEvent.getId(), sourceEvent.getId());
    }
}
