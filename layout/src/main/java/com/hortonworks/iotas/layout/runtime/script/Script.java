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
import com.hortonworks.iotas.layout.runtime.script.engine.ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.Serializable;

/**
 * @param <I> The type of input on which this script is evaluated, e.g. {@code tuple} for {@code Apache Storm} or {@link IotasEvent}
 * @param <E> The Script Engine used to evaluate the scripts
 */
public abstract class Script<I, O, E> implements Serializable {
    protected static final Logger log = LoggerFactory.getLogger(Script.class);

    protected final String scriptText;
    protected final E scriptEngine;

    public Script(String scriptText, ScriptEngine<E> scriptEngine) {
        this.scriptText = scriptText;
        this.scriptEngine = scriptEngine.getEngine();
    }

    public abstract O evaluate(I input) throws ScriptException;

    @Override
    public String toString() {
        return "Script{" +
                "scriptText='" + scriptText + '\'' +
                ", scriptEngine=" + scriptEngine +
                '}';
    }
}
