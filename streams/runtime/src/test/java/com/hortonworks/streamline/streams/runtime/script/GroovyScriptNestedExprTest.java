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
package com.hortonworks.streamline.streams.runtime.script;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.runtime.script.engine.GroovyScriptEngine;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests to validate groovy conditions with nested expressions.
 */
public class GroovyScriptNestedExprTest {

    GroovyScript<Boolean> groovyScript;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testEvaluateNestedMap() throws Exception {
        groovyScript = new GroovyScript<>("x < y['b']", new GroovyScriptEngine());
        Map<String, Object> nested = new HashMap<>();
        nested.put("a", 5);
        nested.put("b", 10);
        Map<String, Object> kv = new HashMap<>();
        kv.put("x", 2);
        kv.put("y", nested);
        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(kv).dataSourceId("1").build();
        Boolean result = groovyScript.evaluate(event);
        System.out.println(result);
    }

    @Test
    public void testEvaluateNestedList() throws Exception {
        groovyScript = new GroovyScript<>("x < y[0]", new GroovyScriptEngine());
        List<Integer> nested = new ArrayList<>();
        nested.add(5);
        nested.add(1);
        Map<String, Object> kv = new HashMap<>();
        kv.put("x", 2);
        kv.put("y", nested);
        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(kv).dataSourceId("1").build();
        Boolean result = groovyScript.evaluate(event);
        System.out.println(result);
    }

    @Test
    public void testEvaluateNestedMapList() throws Exception {
        groovyScript = new GroovyScript<>("x < y['a'][0]", new GroovyScriptEngine());
        List<Integer> nestedList = new ArrayList<>();
        nestedList.add(5);
        nestedList.add(1);
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("a", nestedList);
        Map<String, Object> kv = new HashMap<>();
        kv.put("x", 2);
        kv.put("y", nestedMap);
        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(kv).dataSourceId("1").build();
        Boolean result = groovyScript.evaluate(event);
        System.out.println(result);
    }
}