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
import javax.script.ScriptContext;
import javax.script.ScriptException;
import java.util.Map;

/**
 * Evaluates the {@link Expression} for each {@code Input} using the provided Groovy Engine
 *
 * @param <O> Type of output returned after the script is evaluated with {@link GroovyScript#evaluate(IotasEvent)}.
 */
public class GroovyScript<O> extends Script<IotasEvent, O, javax.script.ScriptEngine> {
    private static final Logger LOG = LoggerFactory.getLogger(GroovyScript.class);

    public GroovyScript(String expression,
                        ScriptEngine<javax.script.ScriptEngine> scriptEngine) {
        super(expression, scriptEngine);
        LOG.debug("Created Groovy Script: {}", super.toString());
    }

    @Override
    public O evaluate(IotasEvent iotasEvent) throws ScriptException {
        LOG.debug("Evaluating [{}] with [{}]", expression, iotasEvent);
        O evaluatedResult = null;

        try {
            if (iotasEvent != null) {
                final Map<String, Object> fieldsToValues = iotasEvent.getFieldsAndValues();
                if (fieldsToValues != null) {
                    getEngineScopeBindings().putAll(fieldsToValues);
                    LOG.debug("Set script binding to [{}]", fieldsToValues);

                    evaluatedResult = (O) scriptEngine.eval(expression);

                    LOG.debug("Expression [{}] evaluated to [{}]", expression, evaluatedResult);
                }
            }
        } finally {
            // It is absolutely necessary to clear the bindings. Otherwise the old values of a key will be used
            // to evaluate the expression when the iotasEvent doesn't have such key
            clearBindings();
        }
        return evaluatedResult;
    }

    private Bindings getEngineScopeBindings() {
        return scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
    }

    private void clearBindings() {
        getEngineScopeBindings().clear();
        LOG.debug("Script binding reset to empty binding");
    }
}
