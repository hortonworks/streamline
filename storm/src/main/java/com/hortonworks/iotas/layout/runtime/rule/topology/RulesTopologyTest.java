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

package com.hortonworks.iotas.layout.runtime.rule.topology;

import backtype.storm.Config;
import backtype.storm.ILocalCluster;
import backtype.storm.LocalCluster;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.TopologyBuilder;
import com.hortonworks.bolt.rules.RulesBolt;
import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.runtime.processor.RuleProcessorRuntime;
import com.hortonworks.iotas.layout.runtime.processor.RuleProcessorRuntimeConstructor;
import com.hortonworks.iotas.layout.runtime.processor.RulesProcessorRuntimeBuilder;
import com.hortonworks.iotas.layout.runtime.rule.GroovyRuleRuntimeBuilder;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntimeBuilder;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntimeConstructor;

public class RulesTopologyTest {

    protected static final String RULES_TEST_SPOUT = "RulesTestSpout";
    protected static final String RULES_BOLT = "rulesBolt";
    protected static final String RULES_TEST_SINK_BOLT = "RulesTestSinkBolt";
    protected static final String RULES_TEST_SINK_BOLT_1 = RULES_TEST_SINK_BOLT + "_1";
    protected static final String RULES_TEST_SINK_BOLT_2 = RULES_TEST_SINK_BOLT + "_2";

    public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException {
        RulesTopologyTest rulesTopologyTest = new RulesTopologyTest();
        rulesTopologyTest.submitTopology();
    }
    protected void submitTopology() throws AlreadyAliveException, InvalidTopologyException {
        final Config config = new Config();
        config.setDebug(true);
        final String topologyName = "RulesTopologyTest";
        ILocalCluster localCluster = new LocalCluster();
        localCluster.submitTopology(topologyName, config, createTopology());
    }

    protected StormTopology createTopology() {
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout(RULES_TEST_SPOUT, new RulesTestSpout());
        builder.setBolt(RULES_BOLT, createRulesBolt(createRulesProcessorRuntime())).shuffleGrouping(RULES_TEST_SPOUT);
        builder.setBolt(RULES_TEST_SINK_BOLT_1, new RulesTestSinkBolt()).shuffleGrouping(RULES_BOLT, getStream1());
        builder.setBolt(RULES_TEST_SINK_BOLT_2, new RulesTestSinkBolt()).shuffleGrouping(RULES_BOLT, getStream2());
        return builder.createTopology();
    }

    protected IRichBolt createRulesBolt(RuleProcessorRuntime rulesProcessorRuntime) {
        return new RulesBolt(rulesProcessorRuntime);
    }

    protected RuleProcessorRuntime createRulesProcessorRuntime() {
        RulesProcessor rulesProcessor = new RuleProcessorMockBuilder(1,2,2).build();
        RuleRuntimeBuilder ruleRuntimeBuilder = new GroovyRuleRuntimeBuilder();
        RuleRuntimeConstructor ruleRuntimeConstructor = new RuleRuntimeConstructor(ruleRuntimeBuilder);
        RulesProcessorRuntimeBuilder ruleProcessorRuntimeBuilder = new RulesProcessorRuntimeBuilder(rulesProcessor, ruleRuntimeConstructor);
        RuleProcessorRuntimeConstructor ruleProcessorRuntimeConstructor = new RuleProcessorRuntimeConstructor(ruleProcessorRuntimeBuilder);
        ruleProcessorRuntimeConstructor.construct();
        return ruleProcessorRuntimeConstructor.getRuleProcessorRuntime();
    }

    protected String getStream1() {
        return "rule_processsor_1.rule_1.1";
    }


    protected String getStream2() {
        return "rule_processsor_1.rule_2.2";
    }
}
