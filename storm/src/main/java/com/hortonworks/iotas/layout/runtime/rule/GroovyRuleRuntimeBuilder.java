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

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.runtime.rule.condition.expression.GroovyExpression;
import com.hortonworks.iotas.layout.runtime.script.GroovyScript;
import com.hortonworks.iotas.layout.runtime.script.Script;
import com.hortonworks.iotas.layout.runtime.script.engine.GroovyScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;

public class GroovyRuleRuntimeBuilder extends AbstractRuleRuntimeBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(GroovyRuleRuntimeBuilder.class);
    private Rule rule;
    private GroovyExpression groovyExpression;
    private GroovyScriptEngine groovyScriptEngine;
    private GroovyScript<Boolean> groovyScript;

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
        groovyScript = new GroovyScript<Boolean>(groovyExpression.asString(), groovyScriptEngine) {
            @Override
            public Boolean evaluate(IotasEvent iotasEvent) throws ScriptException {
                Boolean evaluates = false;
                try {
                    evaluates =  super.evaluate(iotasEvent);
                } catch (ScriptException e) {
                    if (e.getCause() != null && e.getCause() instanceof groovy.lang.MissingPropertyException) {
                        // Occurs when not all the properties required for evaluating the script are set. This can happen for example
                        // when receiving an IotasEvent that does not have all the fields required to evaluate the expression
                        LOG.debug("Missing property required to evaluate expression. {}", e.getCause().getMessage());
                        LOG.trace("",e);
                        evaluates = false;
                    } else {
                        throw e;
                    }
                }
                return evaluates;
            }
        };
    }

    @Override
    public RuleRuntime buildRuleRuntime() {
        GroovyScript<IotasEvent> wrapper = new GroovyScript<IotasEvent>(groovyExpression.asString(), groovyScriptEngine) {
            @Override
            public IotasEvent evaluate(IotasEvent input) throws ScriptException {
                return groovyScript.evaluate(input) ? input : null;
            }
        };
        return new RuleRuntime(rule, wrapper, actions);
    }

    @Override
    public String toString() {
        return "GroovyRuleRuntimeBuilder{" +
                "groovyScriptEngine=" + groovyScriptEngine +
                ", groovyScript=" + groovyScript +
                ", actions=" + actions +
                '}';
    }

    @Override
    protected Rule getRule() {
        return rule;
    }
}
