package com.hortonworks.streamline.streams.runtime.utils;

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
