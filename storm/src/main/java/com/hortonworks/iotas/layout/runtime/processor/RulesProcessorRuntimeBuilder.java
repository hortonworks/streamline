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

import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntime;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntimeConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RulesProcessorRuntimeBuilder {
    private static final Logger log = LoggerFactory.getLogger(RulesProcessorRuntimeBuilder.class);

    private final RulesProcessor rulesProcessor;
    private final RuleRuntimeConstructor ruleRuntimeConstructor;
    private List<RuleRuntime> rulesRuntime;

    public RulesProcessorRuntimeBuilder(RulesProcessor rulesProcessor,
                                        RuleRuntimeConstructor ruleRuntimeConstructor) {
        this.rulesProcessor = rulesProcessor;
        this.ruleRuntimeConstructor = ruleRuntimeConstructor;
    }

    public void build() {
        final List<Rule> rules = rulesProcessor.getRules();

        rulesRuntime = new ArrayList<>();

        if (rules != null) {
            for (Rule rule : rules) {
                ruleRuntimeConstructor.construct(rule);
                RuleRuntime ruleRuntime = ruleRuntimeConstructor.getRuleRuntime(rule);
                rulesRuntime.add(ruleRuntime);
                log.debug("Added {}", ruleRuntime);
            }
        }
    }

    public RuleProcessorRuntime getRuleProcessorRuntime() {
        return  new RuleProcessorRuntime(rulesRuntime, rulesProcessor) ;
    }
}
