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

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.layout.runtime.rule.condition.expression.Expression;
import com.hortonworks.iotas.layout.runtime.script.engine.ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.Map;

/**
 * Evaluates the {@link Expression} for each {@code Input} using the provided Groovy Engine
 *
 * @param <O> Type of output returned after the script is evaluated with {@link GroovyScript#evaluate(IotasEvent)}.
 */
public class GroovyScript<O> extends Script<IotasEvent, O, javax.script.ScriptEngine> {
    private static final Logger LOG = LoggerFactory.getLogger(GroovyScript.class);

    private CompiledScript compiledScript;

    public GroovyScript(String expression,
                        ScriptEngine<javax.script.ScriptEngine> scriptEngine) {
        super(expression, scriptEngine);
        LOG.debug("Created Groovy Script: {}", super.toString());
    }

    @Override
    public O evaluate(IotasEvent iotasEvent) throws ScriptException {
        LOG.debug("Evaluating [{}] with [{}]", expression, iotasEvent);
        O evaluatedResult = null;

        // lazy compilation
        if (compiledScript == null && scriptEngine instanceof Compilable) {
            compiledScript = ((Compilable) scriptEngine).compile(expression);
        }

        if (iotasEvent != null) {
            final Map<String, Object> fieldsToValues = iotasEvent.getFieldsAndValues();
            if (fieldsToValues != null) {
                Bindings bindings = new SimpleBindings();
                bindings.putAll(fieldsToValues);
                LOG.debug("Use script binding to [{}]", fieldsToValues);

                if (compiledScript != null) {
                    evaluatedResult = (O) compiledScript.eval(bindings);
                } else {
                    evaluatedResult = (O) scriptEngine.eval(expression, bindings);
                }

                LOG.debug("Expression [{}] evaluated to [{}]", expression, evaluatedResult);
            }
        }

        return evaluatedResult;
    }
}
