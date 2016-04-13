/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.iotas.layout.runtime.transform;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.layout.design.transform.SubstituteTransform;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link SubstituteTransformRuntime}
 */
public class SubstituteTransformRuntimeTest {

    @Test
    public void testSubstituteNoVars() throws Exception {
        Map<String, Object> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("1", "one");
        fieldsAndValues.put("2", "two");
        fieldsAndValues.put("3", "three");

        IotasEvent event = new IotasEventImpl(fieldsAndValues, "dsrcid");
        TransformRuntime transformRuntime = new SubstituteTransformRuntime();
        List<IotasEvent> result = transformRuntime.execute(event);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getFieldsAndValues().size());
        assertEquals("one", result.get(0).getFieldsAndValues().get("1"));
        assertEquals("two", result.get(0).getFieldsAndValues().get("2"));
        assertEquals("three", result.get(0).getFieldsAndValues().get("3"));
    }

    @Test
    public void testSubstituteSimpleVars() throws Exception {
        Map<String, Object> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("1", "one");
        fieldsAndValues.put("2", "two");
        fieldsAndValues.put("3", "${1} plus ${2}");

        IotasEvent event = new IotasEventImpl(fieldsAndValues, "dsrcid");
        TransformRuntime transformRuntime = new SubstituteTransformRuntime();
        List<IotasEvent> result = transformRuntime.execute(event);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getFieldsAndValues().size());
        assertEquals("one", result.get(0).getFieldsAndValues().get("1"));
        assertEquals("two", result.get(0).getFieldsAndValues().get("2"));
        assertEquals("one plus two", result.get(0).getFieldsAndValues().get("3"));
    }

    @Test
    public void testSubstituteSpecificVars() throws Exception {
        Map<String, Object> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("1", "one");
        fieldsAndValues.put("2", "${1} plus ${1}");
        fieldsAndValues.put("3", "${1} plus two");

        IotasEvent event = new IotasEventImpl(fieldsAndValues, "dsrcid");
        TransformRuntime transformRuntime = new SubstituteTransformRuntime(new SubstituteTransform(Collections.singleton("3")));
        List<IotasEvent> result = transformRuntime.execute(event);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getFieldsAndValues().size());
        assertEquals("one", result.get(0).getFieldsAndValues().get("1"));
        assertEquals("${1} plus ${1}", result.get(0).getFieldsAndValues().get("2"));
        assertEquals("one plus two", result.get(0).getFieldsAndValues().get("3"));
    }

    @Test
    public void testSubstituteSimpleObj() throws Exception {
        Map<String, Object> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("1", 1);
        fieldsAndValues.put("2", 2);
        fieldsAndValues.put("3", "${1} plus ${2}");

        IotasEvent event = new IotasEventImpl(fieldsAndValues, "dsrcid");
        TransformRuntime transformRuntime = new SubstituteTransformRuntime();
        List<IotasEvent> result = transformRuntime.execute(event);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getFieldsAndValues().size());
        assertEquals(1, result.get(0).getFieldsAndValues().get("1"));
        assertEquals(2, result.get(0).getFieldsAndValues().get("2"));
        assertEquals("1 plus 2", result.get(0).getFieldsAndValues().get("3"));
    }

    @Test
    public void testSubstituteRefVars() throws Exception {
        Map<String, Object> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("1", "one");
        fieldsAndValues.put("2", "${1} plus ${1}");
        fieldsAndValues.put("3", "${1} plus ${2}");

        IotasEvent event = new IotasEventImpl(fieldsAndValues, "dsrcid");
        TransformRuntime transformRuntime = new SubstituteTransformRuntime();
        List<IotasEvent> result = transformRuntime.execute(event);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getFieldsAndValues().size());
        assertEquals("one", result.get(0).getFieldsAndValues().get("1"));
        assertEquals("one plus one", result.get(0).getFieldsAndValues().get("2"));
        assertEquals("one plus one plus one", result.get(0).getFieldsAndValues().get("3"));
    }

    @Test
    public void testCyclicRef() throws Exception {
        Map<String, Object> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("1", "${2} minus one");
        fieldsAndValues.put("2", "${1} plus one");

        IotasEvent event = new IotasEventImpl(fieldsAndValues, "dsrcid");
        TransformRuntime transformRuntime = new SubstituteTransformRuntime();
        List<IotasEvent> result = transformRuntime.execute(event);
        System.out.println(result);
        assertEquals(1, result.size());
        assertEquals(event, result.get(0));
    }
}