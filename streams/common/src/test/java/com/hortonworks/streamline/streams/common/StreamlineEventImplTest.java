package com.hortonworks.streamline.streams.common;

import com.hortonworks.streamline.streams.StreamlineEvent;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by aiyer on 9/23/15.
 */
public class StreamlineEventImplTest {

    @Test
    public void testGetFieldsAndValues() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        StreamlineEvent event = new StreamlineEventImpl(map, StringUtils.EMPTY);

        assertEquals(map, event);
    }

    @Test
    public void testGetId() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        StreamlineEvent event = new StreamlineEventImpl(map, org.apache.commons.lang.StringUtils.EMPTY);

        assertNotNull(UUID.fromString(event.getId()));
    }

    @Test
    public void testGetDataSourceId() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        StreamlineEvent event = new StreamlineEventImpl(map, "1");

        assertEquals("1", event.getDataSourceId());

    }

    @Test
    public void testGetSourceStream() throws Exception {
        String sourceStream = "stream";
        StreamlineEvent event = new StreamlineEventImpl(new HashMap<String, Object>(), "1");
        assertEquals(StreamlineEventImpl.DEFAULT_SOURCE_STREAM, event.getSourceStream());
        event = new StreamlineEventImpl(new HashMap<String, Object>(), "1", "1", new HashMap<String, Object>(), sourceStream);
        assertEquals(sourceStream, event.getSourceStream());
    }

    @Test
    public void testEquals() throws Exception {

        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        StreamlineEvent event1 = new StreamlineEventImpl(map, StringUtils.EMPTY);

        StreamlineEvent event2 = new StreamlineEventImpl(map, StringUtils.EMPTY, event1.getId());

        assertEquals(event1, event2);
    }

    @Test
    public void testHashcode() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        StreamlineEvent event1 = new StreamlineEventImpl(map, StringUtils.EMPTY);

        StreamlineEvent event2 = new StreamlineEventImpl(map, StringUtils.EMPTY, event1.getId());

        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPut() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");

        StreamlineEvent event = new StreamlineEventImpl(map, StringUtils.EMPTY);
        event.put("key", "val");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClear() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");

        StreamlineEvent event = new StreamlineEventImpl(map, StringUtils.EMPTY);
        event.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPutAll() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");

        StreamlineEvent event = new StreamlineEventImpl(Collections.emptyMap(), StringUtils.EMPTY);
        event.putAll(map);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemove() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");

        StreamlineEvent event = new StreamlineEventImpl(Collections.emptyMap(), StringUtils.EMPTY);
        event.remove("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveIterator() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");

        StreamlineEvent event = new StreamlineEventImpl(map, StringUtils.EMPTY);
        Iterator<Map.Entry<String, Object>> it = event.entrySet().iterator();
        while(it.hasNext()) {
            it.next();
            it.remove();
        }
    }
}
