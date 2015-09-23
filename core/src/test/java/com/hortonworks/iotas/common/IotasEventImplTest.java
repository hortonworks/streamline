package com.hortonworks.iotas.common;

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


        IotasEvent event = new IotasEventImpl(map);

        assertEquals(map, event.getFieldsAndValues());
    }

    @Test
    public void testGetId() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        IotasEvent event = new IotasEventImpl(map);

        assertNotNull(UUID.fromString(event.getId()));
    }

    @Test
    public void testGetDataSourceId() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        IotasEvent event = new IotasEventImpl(map).withDataSourceId("1");

        assertEquals("1", event.getDataSourceId());

    }

    @Test
    public void testEquals() throws Exception {

        Map<String, Object> map = new HashMap<>();
        map.put("a", "aval");
        map.put("b", "bval");


        IotasEvent event1 = new IotasEventImpl(map);

        IotasEvent event2 = new IotasEventImpl(map, event1.getId());

        assertEquals(event1, event2);
    }
}