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
import com.hortonworks.iotas.layout.design.rule.condition.Expression;
import com.hortonworks.iotas.layout.runtime.script.engine.ScriptEngine;

import javax.script.ScriptException;
import java.io.Serializable;

/**
 * Evaluates the {@link Expression} with the {@link ScriptEngine} for each {@code Input}
 * @param <I> The type of input on which this script is evaluated, e.g. {@code tuple} for {@code Apache Storm} or {@link IotasEvent}
 * @param <O> The type of output returned after the script is evaluated with {@link Script#evaluate(I)}.
 * @param <E> The Script Engine used to evaluate the scripts
 */
public abstract class Script<I, O, E> implements Serializable {
    protected final String expression;
    protected final E scriptEngine;

    /**
     * Sets the {@link Expression} to be evaluated by the {@link ScriptEngine}
     */
    public Script(String expression, ScriptEngine<E> scriptEngine) {
        this.expression = expression;
        this.scriptEngine = scriptEngine.getEngine();
    }

    public abstract O evaluate(I input) throws ScriptException;

    @Override
    public String toString() {
        return "Script{" + expression + ", scriptEngine=" + scriptEngine + '}';
    }
}
