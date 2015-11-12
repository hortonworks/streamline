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

package com.hortonworks.iotas.layout.runtime.rule;

import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.runtime.rule.condition.expression.GroovyExpression;
import com.hortonworks.iotas.layout.runtime.script.GroovyScript;
import com.hortonworks.iotas.layout.runtime.script.engine.GroovyScriptEngine;

public class GroovyRuleRuntimeBuilder implements RuleRuntimeBuilder {
    private GroovyExpression groovyExpression;
    private GroovyScriptEngine groovyScriptEngine;
    private GroovyScript groovyScript;

    public GroovyRuleRuntimeBuilder() {
    }

    public void buildExpression(Rule rule) {
        groovyExpression = new GroovyExpression(rule.getCondition());
    }

    public void buildScriptEngine() {
        groovyScriptEngine = new GroovyScriptEngine();
    }

    public void buildScript() {
        groovyScript = new GroovyScript(groovyExpression.getExpression(), groovyScriptEngine);
    }

    public RuleRuntime getRuleRuntime(Rule rule) {
        return new RuleRuntime(rule, groovyScript);
    }
}
