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
import com.hortonworks.iotas.layout.design.rule.action.Action;
import com.hortonworks.iotas.layout.runtime.ActionRuntime;
import com.hortonworks.iotas.layout.runtime.rule.condition.expression.GroovyExpression;
import com.hortonworks.iotas.layout.runtime.script.GroovyScript;
import com.hortonworks.iotas.layout.runtime.script.engine.GroovyScriptEngine;
import com.hortonworks.iotas.layout.transform.AddHeaderTransform;
import com.hortonworks.iotas.layout.transform.IdentityTransform;
import com.hortonworks.iotas.layout.transform.ProjectionTransform;
import com.hortonworks.iotas.layout.transform.Transform;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroovyRuleRuntimeBuilder implements RuleRuntimeBuilder {
    private Rule rule;
    private GroovyExpression groovyExpression;
    private GroovyScriptEngine groovyScriptEngine;
    private GroovyScript<Boolean> groovyScript;
    private List<ActionRuntime> actions;

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
    public void buildActions() {
        List<ActionRuntime> runtimeActions = new ArrayList<>();
        for (Action action : rule.getActions()) {
            String streamId = rule.getRuleProcessorName() + "." + rule.getName() + "."
                    + rule.getId() + "." + action.getName();
            /*
             * Add an ActionRuntime to perform necessary transformation for notification
             */
            runtimeActions.add(new ActionRuntime(streamId, getTransforms(action)));
        }
        actions = runtimeActions;
    }

    @Override
    public void buildScript() {
        groovyScript = new GroovyScript<Boolean>(groovyExpression.getExpression(), groovyScriptEngine) {
            @Override
            public Boolean evaluate(IotasEvent iotasEvent) throws ScriptException {
                Boolean evaluates = false;
                try {
                    evaluates =  super.evaluate(iotasEvent);
                } catch (ScriptException e) {
                    if (e.getCause() != null && e.getCause().getCause() instanceof groovy.lang.MissingPropertyException) {
                        // Occurs when not all the properties required for evaluating the script are set. This can happen for example
                        // when receiving an IotasEvent that does not have all the fields required to evaluate the expression
                        log.debug("Missing property required to evaluate expression. {}", e.getCause().getMessage());
                        log.trace("",e);
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
       return new RuleRuntime(rule, groovyScript, actions);
    }

    @Override
    public String toString() {
        return "GroovyRuleRuntimeBuilder{" +
                "groovyScriptEngine=" + groovyScriptEngine +
                ", groovyScript=" + groovyScript +
                ", actions=" + actions +
                '}';
    }

    /**
     * Returns the necessary transforms to perform based on the action.
     */
    private List<Transform> getTransforms(Action action) {
        List<Transform> transforms = new ArrayList<>();
        if (!action.getOutputFieldsAndDefaults().isEmpty()) {
            transforms.add(new ProjectionTransform(action.getOutputFieldsAndDefaults()));
        }
        if (action.isIncludeMeta()) {
            Map<String, Object> headers = new HashMap<>();
            headers.put("ruleId", rule.getId());
            transforms.add(new AddHeaderTransform(headers));
        }
        // default is to just forward the event
        if(transforms.isEmpty()) {
            transforms.add(new IdentityTransform());
        }
        return transforms;
    }
}
