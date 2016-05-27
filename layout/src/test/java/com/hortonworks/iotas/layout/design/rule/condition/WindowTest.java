package com.hortonworks.iotas.layout.design.rule.condition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.topology.component.rule.condition.Window;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Window}
 */
public class WindowTest {
    @Test
    public void testCountBasedWindowConfig() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"windowLength\":{\"class\":\".Window$Count\",\"count\":100},\"slidingInterval\":{\"class\":\".Window$Count\",\"count\":10},\"tsField\":null,\"lagMs\":0}";
        Window w = mapper.readValue(json, Window.class);

        assertEquals(0L, w.getLagMs());
        assertEquals(null, w.getTsField());
        assertEquals(new Window.Count(100), w.getWindowLength());
        assertEquals(new Window.Count(10), w.getSlidingInterval());
    }

    @Test
    public void testCountAndDurationBasedWindowConfig() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"windowLength\":{\"class\":\".Window$Duration\",\"durationMs\":100},\"slidingInterval\":{\"class\":\".Window$Count\",\"count\":10},\"tsField\":\"ts\",\"lagMs\":5}";
        Window w = mapper.readValue(json, Window.class);

        assertEquals(5L, w.getLagMs());
        assertEquals("ts", w.getTsField());
        assertEquals(new Window.Duration(100), w.getWindowLength());
        assertEquals(new Window.Count(10), w.getSlidingInterval());

    }

}