package org.apache.streamline.streams.runtime.script;

import org.apache.streamline.streams.IotasEvent;
import org.apache.streamline.streams.common.IotasEventImpl;
import org.apache.streamline.streams.runtime.script.engine.GroovyScriptEngine;
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
        IotasEvent event = new IotasEventImpl(kv, "1");
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
        IotasEvent event = new IotasEventImpl(kv, "1");
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
        IotasEvent event = new IotasEventImpl(kv, "1");
        Boolean result = groovyScript.evaluate(event);
        System.out.println(result);
    }
}