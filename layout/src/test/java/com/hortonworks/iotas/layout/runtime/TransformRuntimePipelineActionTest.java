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
package com.hortonworks.iotas.layout.runtime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.layout.design.rule.action.TransformAction;
import com.hortonworks.iotas.layout.design.transform.MergeTransform;
import com.hortonworks.iotas.layout.design.transform.ProjectionTransform;
import com.hortonworks.iotas.layout.design.transform.SubstituteTransform;
import com.hortonworks.iotas.layout.runtime.rule.action.ActionRuntime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link TransformActionRuntime}
 */
public class TransformRuntimePipelineActionTest {

    @Test
    public void testMergeProject() throws Exception {
        Map<String, Object> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("1", "one");
        fieldsAndValues.put("2", "two");

        Map<String, String> defaults = new HashMap<>();
        defaults.put("2", "TWO");
        defaults.put("3", "THREE");

        IotasEvent event = new IotasEventImpl(fieldsAndValues, "dsrcid");
        MergeTransform merge = new MergeTransform(defaults);
        ProjectionTransform projection = new ProjectionTransform("test-projection", defaults.keySet());
        TransformAction transformAction = new TransformAction(ImmutableList.of(merge, projection));
        transformAction.setOutputStreams(ImmutableSet.of("streamid"));
        ActionRuntime actionRuntime = new TransformActionRuntime(transformAction);
        List<IotasEvent> resultEvents = new ArrayList<>();
        for (Result result : actionRuntime.execute(event)) {
            resultEvents.addAll(result.events);
        }
        assertEquals(1, resultEvents.size());
        assertEquals(2, resultEvents.get(0).getFieldsAndValues().size());
        assertEquals("THREE", resultEvents.get(0).getFieldsAndValues().get("3"));
        assertEquals("two", resultEvents.get(0).getFieldsAndValues().get("2"));
    }

    @Test
    public void testMergeSubstituteProject() throws Exception {
        Map<String, Object> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("1", "one");
        fieldsAndValues.put("2", "${1} plus ${1}");

        Map<String, Object> defaults = new HashMap<>();
        defaults.put("2", "TWO");
        defaults.put("3", "THREE");
        defaults.put("4", "${2} plus ${2}");

        IotasEvent event = new IotasEventImpl(fieldsAndValues, "dsrcid");
        MergeTransform merge = new MergeTransform(defaults);
        SubstituteTransform substitute = new SubstituteTransform();
        ProjectionTransform projection = new ProjectionTransform("test-projection", defaults.keySet());
        TransformAction transformAction = new TransformAction(ImmutableList.of(merge, substitute, projection));
        transformAction.setOutputStreams(ImmutableSet.of("streamid"));
        ActionRuntime actionRuntime = new TransformActionRuntime(transformAction);
        List<IotasEvent> resultEvents = new ArrayList<>();
        for (Result result : actionRuntime.execute(event)) {
            resultEvents.addAll(result.events);
        }
        assertEquals(1, resultEvents.size());
        assertEquals(3, resultEvents.get(0).getFieldsAndValues().size());
        assertEquals("THREE", resultEvents.get(0).getFieldsAndValues().get("3"));
        assertEquals("one plus one", resultEvents.get(0).getFieldsAndValues().get("2"));
        assertEquals("one plus one plus one plus one", resultEvents.get(0).getFieldsAndValues().get("4"));
    }
}