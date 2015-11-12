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

package com.hortonworks.iotas.layout.runtime.script.engine;

import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import javax.script.Bindings;
import java.io.Serializable;

public class GroovyScriptEngine implements ScriptEngine<javax.script.ScriptEngine>, Serializable {
    /*@Override
    public javax.script.ScriptEngine getEngine() {
        final ScriptEngineManager factory = new ScriptEngineManager();
        javax.script.ScriptEngine engine = factory.getEngineByName("groovy");
        Bindings bindings = engine.createBindings();
        bindings.put("engine", engine);
        return engine;
    }*/

    @Override
    public javax.script.ScriptEngine getEngine() {
        javax.script.ScriptEngine engine = new GroovyScriptEngineImplSerializable();
        Bindings bindings = engine.createBindings();
        bindings.put("engine", engine);
        return engine;
    }

    // This is needed to avoid java.io.NotSerializableException: org.codehaus.groovy.jsr223.GroovyScriptEngineImpl
    public static class GroovyScriptEngineImplSerializable extends GroovyScriptEngineImpl implements Serializable {
        public GroovyScriptEngineImplSerializable() {
            super();
        }
    }
}
