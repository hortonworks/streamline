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

package com.hortonworks.iotas.layout.runtime.processor;

import com.hortonworks.iotas.layout.design.component.ComponentBuilder;
import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntime;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntimeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RuleProcessorRuntimeDependenciesBuilder {
    protected static final Logger LOG = LoggerFactory.getLogger(RuleProcessorRuntimeDependenciesBuilder.class);

    private final RulesProcessor rulesProcessor;
    private final RuleRuntimeBuilder ruleRuntimeBuilder;

    public RuleProcessorRuntimeDependenciesBuilder(ComponentBuilder<RulesProcessor> rulesProcessorBuilder,
                                                   RuleRuntimeBuilder ruleRuntimeBuilder) {
        this.rulesProcessor = rulesProcessorBuilder.build();
        this.ruleRuntimeBuilder = ruleRuntimeBuilder;
    }

    public List<RuleRuntime> getRulesRuntime() {
        final List<Rule> rules = rulesProcessor.getRules();
        final List<RuleRuntime> rulesRuntime = new ArrayList<>();

        if (rules != null) {
            for (Rule rule : rules) {
                ruleRuntimeBuilder.setRule(rule);
                if (rule.getCondition() != null) {
                    ruleRuntimeBuilder.buildExpression();
                    ruleRuntimeBuilder.buildScriptEngine();
                    ruleRuntimeBuilder.buildScript();
                }
                ruleRuntimeBuilder.buildActions();
                RuleRuntime ruleRuntime = ruleRuntimeBuilder.buildRuleRuntime();
                rulesRuntime.add(ruleRuntime);
                LOG.trace("Added {}", ruleRuntime);
            }
            LOG.debug("Finished building. [{}]", this);
        }
        return rulesRuntime;
    }

    public RulesProcessor getRulesProcessor() {
        return rulesProcessor;
    }

    @Override
    public String toString() {
        return "RuleProcessorRuntimeDependenciesBuilder{" + rulesProcessor +
                ", "+ ruleRuntimeBuilder + '}';
    }
}
