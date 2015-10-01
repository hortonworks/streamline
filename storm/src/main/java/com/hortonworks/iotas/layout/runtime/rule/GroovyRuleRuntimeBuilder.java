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

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.runtime.rule.condition.expression.GroovyExpression;
import com.hortonworks.iotas.layout.runtime.rule.condition.script.GroovyScript;
import com.hortonworks.iotas.layout.runtime.rule.condition.script.engine.GroovyScriptEngine;

public class GroovyRuleRuntimeBuilder implements RuleRuntimeBuilder<Tuple, OutputCollector> {
    private Rule rule;
    private GroovyExpression groovyExpression;
    private GroovyScriptEngine groovyScriptEngine;
    private GroovyScript groovyScript;

    @Override
    public void setRule(Rule rule) {
        this.rule = rule;
    }

    @Override
    public void buildExpression() {
        groovyExpression = new GroovyExpression(rule.getCondition());
    }

    @Override
    public void buildScriptEngine() {
        groovyScriptEngine = new GroovyScriptEngine();
    }

    @Override
    public void buildScript() {
        groovyScript = new GroovyScript(groovyExpression, groovyScriptEngine);
    }

    @Override
    public RuleRuntimeStorm buildRuleRuntime() {
        return new RuleRuntimeStorm(rule, groovyScript);
    }

    @Override
    public String toString() {
        return "GroovyRuleRuntimeBuilder{" +
                ", groovyScriptEngine=" + groovyScriptEngine +
                ", groovyScript=" + groovyScript +
                '}';
    }
}
