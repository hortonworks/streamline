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

import backtype.storm.topology.OutputFieldsDeclarer;
import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntime;

import java.io.Serializable;
import java.util.List;


/**
 * Object representing a design time rules processor.
 */
public class RuleProcessorRuntime implements Serializable {

    protected RulesProcessor rulesProcessor;
    protected List<RuleRuntime> rulesRuntime;

    RuleProcessorRuntime(List<RuleRuntime> rulesRuntime, RulesProcessor rulesProcessor) {
        this.rulesRuntime = rulesRuntime;

        this.rulesProcessor = rulesProcessor;
    }

    public List<RuleRuntime> getRulesRuntime() {
        return rulesRuntime;
    }

    public void declareOutput(OutputFieldsDeclarer declarer) {
        for (RuleRuntime ruleRuntime:rulesRuntime) {
            ruleRuntime.declareOutput(declarer);
        }
    }

    public void setRulesRuntime(List<RuleRuntime> rulesRuntime) {
        this.rulesRuntime = rulesRuntime;
    }

    public RulesProcessor getRuleProcessor() {
        return rulesProcessor;
    }

    public void setRuleProcessor(RulesProcessor rulesProcessor) {
        this.rulesProcessor = rulesProcessor;
    }
}

