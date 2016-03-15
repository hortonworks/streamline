package com.hortonworks.iotas.common;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created by aiyer on 9/23/15.
 */
public class IotasEventImplTest {

    @Test
    public void testGetFieldsAndValues() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        IotasEvent event = new IotasEventImpl(map, StringUtils.EMPTY);

        assertEquals(map, event.getFieldsAndValues());
    }

    @Test
    public void testGetId() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        IotasEvent event = new IotasEventImpl(map, StringUtils.EMPTY);

        assertNotNull(UUID.fromString(event.getId()));
    }

    @Test
    public void testGetDataSourceId() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        IotasEvent event = new IotasEventImpl(map, "1");

        assertEquals("1", event.getDataSourceId());

    }

    @Test
    public void testGetSourceStream() throws Exception {
        String sourceStream = "stream";
        IotasEvent event = new IotasEventImpl(new HashMap<String, Object>(), "1");
        assertEquals(IotasEventImpl.DEFAULT_SOURCE_STREAM, event.getSourceStream());
        event = new IotasEventImpl(new HashMap<String, Object>(), "1", "1", new HashMap<String, Object>(), sourceStream);
        assertEquals(sourceStream, event.getSourceStream());
    }

    @Test
    public void testEquals() throws Exception {

        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        IotasEvent event1 = new IotasEventImpl(map, StringUtils.EMPTY);

        IotasEvent event2 = new IotasEventImpl(map, StringUtils.EMPTY, event1.getId());

        assertEquals(event1, event2);
    }

    @Test
    public void testHashcode() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        IotasEvent event1 = new IotasEventImpl(map, StringUtils.EMPTY);

        IotasEvent event2 = new IotasEventImpl(map, StringUtils.EMPTY, event1.getId());

        assertEquals(event1.hashCode(), event2.hashCode());
    }
}
