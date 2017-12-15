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
package com.hortonworks.streamline.streams.common;

import com.google.common.collect.ImmutableMap;
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
import static org.junit.Assert.assertTrue;

/**
 * Created by aiyer on 9/23/15.
 */
public class StreamlineEventImplTest {

    @Test
    public void testGetFieldsAndValues() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(map).build();
        assertEquals(map, event);
    }

    @Test
    public void testGetId() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");

        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(map).build();
        assertNotNull(UUID.fromString(event.getId()));
    }

    @Test
    public void testGetDataSourceId() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");

        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(map).dataSourceId("1").build();
        assertEquals("1", event.getDataSourceId());

    }

    @Test
    public void testGetSourceStream() throws Exception {
        String sourceStream = "stream";
        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(Collections.emptyMap()).dataSourceId("1").build();
        assertEquals(StreamlineEventImpl.DEFAULT_SOURCE_STREAM, event.getSourceStream());
        event = StreamlineEventImpl.builder().fieldsAndValues(Collections.emptyMap()).dataSourceId("1").sourceStream(sourceStream).build();
        assertEquals(sourceStream, event.getSourceStream());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPut() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");

        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(map).build();
        event.put("key", "val");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClear() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");

        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(map).build();
        event.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPutAll() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");

        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(Collections.emptyMap()).build();
        event.putAll(map);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemove() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");

        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(Collections.emptyMap()).build();
        event.remove("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveIterator() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");

        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(map).build();
        Iterator<Map.Entry<String, Object>> it = event.entrySet().iterator();
        while(it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    @Test
    public void testBuilder() {
        StreamlineEventImpl event = StreamlineEventImpl.builder().put("a", "A").build();
        assertEquals(1, event.size());
        assertEquals("A", event.get("a"));

        event = StreamlineEventImpl.builder().put("a", "A").put("b", "B").build();
        assertEquals(2, event.size());
        assertEquals("A", event.get("a"));
        assertEquals("B", event.get("b"));

        event = StreamlineEventImpl.builder().put("a", "A").putAll(ImmutableMap.of("b", "B", "c", "C")).build();
        assertEquals(3, event.size());
        assertEquals("A", event.get("a"));
        assertEquals("B", event.get("b"));
        assertEquals("C", event.get("c"));

        ImmutableMap<String, Object> kv = ImmutableMap.of("b", "B", "c", "C");
        event = StreamlineEventImpl.builder().putAll(kv).build();
        assertEquals(2, event.size());
        assertEquals("B", event.get("b"));
        assertEquals("C", event.get("c"));
        // should be same reference
        assertTrue(kv == event.delegate());


        event = StreamlineEventImpl.builder().put("a", "A").put("b", "B").build();

        StreamlineEventImpl event2 = StreamlineEventImpl.builder().dataSourceId("dsrcid").putAll(event).build();

        // should be same reference
        assertTrue(event.delegate() == event2.delegate());

    }

    @Test
    public void testToFromString() {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");

        StreamlineEventImpl se1 = StreamlineEventImpl.builder().putAll(map)
                .build();
        String s = se1.toString();
        StreamlineEvent se2 = StreamlineEventImpl.fromString(s);
        assertEquals("aval", se2.get("a"));
        assertEquals("bval", se2.get("b"));

    }

}
