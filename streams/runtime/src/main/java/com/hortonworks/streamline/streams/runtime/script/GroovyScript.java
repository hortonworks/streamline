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

package com.hortonworks.streamline.streams.runtime.script;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Expression;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Collections;
import java.util.Map;

/**
 * Evaluates the {@link Expression} for each {@code Input} using the provided Groovy Engine
 *
 * @param <O> Type of output returned after the script is evaluated with {@link GroovyScript#evaluate(StreamlineEvent)}.
 */
public class GroovyScript<O> extends Script<StreamlineEvent, O, javax.script.ScriptEngine> {
    private static final Logger LOG = LoggerFactory.getLogger(GroovyScript.class);

    // instance of parsed Script is not thread-safe so we want to store parsed script per each thread
    // transient to avoid NotSerializableException
    // volatile to safe lazy-init via Double Checking Lock
    private transient volatile ThreadLocal<groovy.lang.Script> parsedScript;
    private final Map<String, Object> initialBindings;

    public GroovyScript(String expression, com.hortonworks.streamline.streams.runtime.script.engine.ScriptEngine<ScriptEngine> scriptEngine) {
        this(expression, scriptEngine, Collections.<String, Object>emptyMap());
    }

    public GroovyScript(String expression, com.hortonworks.streamline.streams.runtime.script.engine.ScriptEngine<ScriptEngine> scriptEngine, Map<String, Object> initialBindings) {
        super(expression, scriptEngine);
        this.initialBindings = initialBindings;
    }

    @Override
    public O evaluate(StreamlineEvent event) throws ScriptException {
        LOG.debug("Evaluating [{}] with [{}]", expression, event);
        groovy.lang.Script parsedScript = getParsedScript();
        O evaluatedResult = null;

        if (event != null) {
            try {
                Binding binding = createBinding(event);
                parsedScript.setBinding(binding);
                LOG.debug("Set script binding to [{}]", event);

                evaluatedResult = (O) parsedScript.run();

                LOG.debug("Expression [{}] evaluated to [{}]", expression, evaluatedResult);
            } catch (groovy.lang.MissingPropertyException e) {
                LOG.debug("Missing property: Expression [{}] params [{}]", expression, event);
                throw new ScriptException(e);
            }
        }
        return evaluatedResult;
    }

    private Binding createBinding(Map<String, Object> fieldsToValues) {
        Binding binding = new Binding();
        addToBinding(initialBindings, binding);
        addToBinding(fieldsToValues, binding);

        return binding;
    }

    private void addToBinding(Map<String, Object> fieldsToValues, Binding binding) {
        for (Map.Entry<String, Object> entry : fieldsToValues.entrySet()) {
            binding.setProperty(entry.getKey(), entry.getValue());
        }
    }

    private groovy.lang.Script getParsedScript() {
        if (parsedScript == null) {
            synchronized (this) {
                parsedScript = new ThreadLocal<groovy.lang.Script>() {
                    @Override
                    protected groovy.lang.Script initialValue() {
                        return new GroovyShell().parse(expression);
                    }
                };
            }
        }
        return parsedScript.get();
    }

}
