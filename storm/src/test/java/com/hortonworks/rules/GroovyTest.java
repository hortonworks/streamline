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

package com.hortonworks.rules;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.Eval;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class GroovyTest {
    protected static final Logger log = LoggerFactory.getLogger(GroovyTest.class);

    @Test
    public void testGroovyScriptEngine() throws Exception {
        final ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("groovy");
//        engine.put("x", "5");
//        engine.put("y", "3");
//        String s = "x  > \"2\"  &&  y  > 1";
        engine.put("x", 5);
        engine.put("y", 3);
        String s = "x > 2 && y > 1";
        Object result = engine.eval(s);
        log.debug("evaluating [{}] with (x,y)=({},{}) => {}\n", s, engine.get("x"), engine.get("y"), result);
        Assert.assertEquals(true, result);

        engine.put("y", 0);
        result = engine.eval(s);
        Assert.assertEquals(false, result);
        log.debug("evaluating [{}] with (x,y)=({},{}) => {}\n", s, engine.get("x"), engine.get("y"), result);
    }

    @Test
    public void testGroovyEval() throws Exception {
        int result = (int) Eval.me("33*3");
        log.debug("33*3 = {}", result);
        Assert.assertEquals(99, result);
    }

    @Test
    public void testGroovyShell() throws Exception {
        GroovyShell groovyShell = new GroovyShell();
        final String s = "x  > 2  &&  y  > 1";
        Script script = groovyShell.parse(s);

        Binding binding = new Binding();
        binding.setProperty("x",5);
        binding.setProperty("y",3);
        script.setBinding(binding);

        Object result = script.run();
        Assert.assertEquals(true, result);
        log.debug("evaluating [{}] with (x,y)=({},{}) => {}\n", s,
                script.getBinding().getProperty("x"), script.getBinding().getProperty("y"), result);

        binding.setProperty("y",0);
        result = script.run();
        Assert.assertEquals(false, result);
        log.debug("evaluating [{}] with (x,y)=({},{}) => {}\n", s,
                binding.getProperty("x"), binding.getProperty("y"), result);
    }

    @Test(expected = groovy.lang.MissingPropertyException.class)
    public void testGroovyShell_goodBindingFollowedByBadBinding_Exception() throws Exception {
        GroovyShell groovyShell = new GroovyShell();
        final String s = "x  > 2  &&  y  > 1";
        Script script = groovyShell.parse(s);

        Binding binding = new Binding();
        binding.setProperty("x", 5);
        binding.setProperty("y", 3);
        script.setBinding(binding);

        Object result = script.run();
        Assert.assertEquals(true, result);

        Assert.assertTrue(binding.hasVariable("x"));

        binding = new Binding();
        binding.setProperty("x1", 5);
        binding.setProperty("y1", 3);
        script.setBinding(binding);

        Assert.assertFalse(binding.hasVariable("x"));

        script.run();  // throws exception because no bindings for x, y
    }
}
