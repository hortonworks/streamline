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
package com.hortonworks.streamline.streams.layout.design.rule.condition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Window;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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