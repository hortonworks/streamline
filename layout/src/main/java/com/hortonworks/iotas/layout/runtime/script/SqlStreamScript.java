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

import javax.script.ScriptException;

// TODO: Replace Object parameterized type with SQLStream Framework Object
public class SqlStreamScript<O> extends Script<IotasEvent, O, SqlStreamScript.Framework> {

    //TODO: Remove and replace with the actual framework object type
    interface Framework {

    }

    public SqlStreamScript(Expression expression,
                           ScriptEngine<Framework> scriptEngine) {
        super(expression.getExpression(), scriptEngine);
    }

    @Override
    public O evaluate(IotasEvent iotasEvent) throws ScriptException {
//        return framework.eval(input);
        return null;
    }

    /*public SqlStreamScript() {
        Interface:

        *//*public interface Evaluation {
            bool	filter(Tuple record);
        }

        Webserver side code:

        Compiler comp = new Compiler(); // From Haohui's class
        Evaluation obj = comp.compile("let x = 1:Integer,...; x + y > 0 and 1 < 2");
        for (Tuple r : record) {
            if (obj.filter(r)) {
                action();
            }
        }

        *//*
    }*/
}
