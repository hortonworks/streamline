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

import javax.script.ScriptException;
import java.util.HashMap;

public class GroovyScriptTest {
    @Test
    public void testBindingsAreBoundOnlyWhenEvaluation() {
        GroovyScriptEngine groovyScriptEngine = new GroovyScriptEngine();
        String groovyExpression = "temperature > 10 && humidity < 30";

        GroovyScript<Boolean> groovyScript = new GroovyScript<Boolean>(groovyExpression, groovyScriptEngine);
        HashMap<String, Object> fieldsAndValue = new HashMap<>();
        fieldsAndValue.put("temperature", 20);
        fieldsAndValue.put("humidity", 10);
        try {
            groovyScript.evaluate(new IotasEventImpl(fieldsAndValue, "1"));
        } catch (ScriptException e) {
            e.printStackTrace();
            Assert.fail("It shouldn't throw ScriptException");
        }

        fieldsAndValue.clear();
        fieldsAndValue.put("no_related_field", 3);
        try {
            groovyScript.evaluate(new IotasEventImpl(fieldsAndValue, "1"));
            Assert.fail("It should not evaluate correctly");
        } catch (ScriptException e) {
            // no-op, that's what we want
        }
    }
}
