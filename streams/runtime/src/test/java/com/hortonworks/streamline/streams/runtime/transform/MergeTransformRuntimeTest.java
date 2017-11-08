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

package com.hortonworks.streamline.streams.runtime.transform;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.layout.component.rule.action.transform.MergeTransform;
import com.hortonworks.streamline.streams.runtime.TransformRuntime;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link MergeTransformRuntime}
 */
public class MergeTransformRuntimeTest {

    @Test
    public void testExecute() throws Exception {
        Map<String, Object> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("1", "one");
        fieldsAndValues.put("2", "two");

        Map<String, String> defaults = new HashMap<>();
        defaults.put("2", "TWO");
        defaults.put("3", "THREE");

        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(fieldsAndValues).dataSourceId("dsrcid").build();
        TransformRuntime transformRuntime = new MergeTransformRuntime(new MergeTransform(defaults));
        List<StreamlineEvent> result = transformRuntime.execute(event);
        System.out.println(result);
        assertEquals(1, result.size());
        assertEquals("two", result.get(0).get("2"));
        assertEquals("THREE", result.get(0).get("3"));
    }
}