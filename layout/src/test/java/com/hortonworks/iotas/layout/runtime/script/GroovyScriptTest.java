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

package com.hortonworks.iotas.layout.runtime.script;

import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.layout.runtime.script.engine.GroovyScriptEngine;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GroovyScriptTest {
    private static final Logger LOG = LoggerFactory.getLogger(GroovyScriptTest.class);

    @Test
    public void testBindingsAreBoundOnlyWhenEvaluation() {
        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine();
        String groovyExpression = "temperature > 10 && humidity < 30";

        GroovyScript<Boolean> groovyScript = new GroovyScript<Boolean>(groovyExpression, groovyScriptEngine);
        HashMap<String, Object> fieldsAndValue = new HashMap<>();
        fieldsAndValue.put("temperature", 20);
        fieldsAndValue.put("humidity", 10);
        try {
            assertTrue(groovyScript.evaluate(new IotasEventImpl(fieldsAndValue, "1")));
        } catch (ScriptException e) {
            e.printStackTrace();
            Assert.fail("It shouldn't throw ScriptException");
        }

        fieldsAndValue.clear();
        fieldsAndValue.put("no_related_field", 3);
        try {
            groovyScript.evaluate(new IotasEventImpl(fieldsAndValue, "1"));
            // it means that previous bound variables are used now
            Assert.fail("It should not evaluate correctly");
        } catch (ScriptException e) {
            // no-op, that's what we want
        }
    }

    @Test
    public void testGroovyScriptEnsuresThreadSafe() throws InterruptedException {
        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine();
        String groovyExpression = "a % 2 == 0";

        final GroovyScript<Boolean> groovyScript = new GroovyScript<>(groovyExpression, groovyScriptEngine);
        final AtomicInteger index = new AtomicInteger(0);

        List<Thread> threads = new ArrayList<>();
        final AtomicReference<Throwable> anyException = new AtomicReference<>();
        for (int i = 0 ; i < 500 ; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0 ; j < 10 ; j++) {
                        try {
                            Thread.sleep(new Random().nextInt(10));
                        } catch (InterruptedException e) {
                            // no-op
                        }

                        int aVal = index.getAndIncrement();

                        HashMap<String, Object> fieldsAndValue = new HashMap<>();
                        fieldsAndValue.put("a", aVal);

                        try {
                            assertEquals(aVal % 2 == 0, groovyScript.evaluate(new IotasEventImpl(fieldsAndValue, "1")));
                        } catch (Throwable e) {
                            e.printStackTrace();
                            anyException.set(e);
                        }
                    }
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        if (anyException.get() != null) {
            Assert.fail("Exception occurred within thread, first one is " + anyException.get().getMessage());
        }
    }
}