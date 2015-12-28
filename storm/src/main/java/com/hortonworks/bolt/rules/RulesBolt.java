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

package com.hortonworks.bolt.rules;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.layout.runtime.processor.RuleProcessorRuntimeStorm;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntime;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntimeStormDeclaredOutput;
import com.hortonworks.iotas.layout.runtime.rule.RulesBoltDependenciesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class RulesBolt extends BaseRichBolt {
    private static final Logger LOG = LoggerFactory.getLogger(RulesBolt.class);

    private final RulesBoltDependenciesFactory boltDependenciesFactory;
    private OutputCollector collector;

    // transient fields
    private transient RuleProcessorRuntimeStorm ruleProcessorRuntime;

    public RulesBolt(RulesBoltDependenciesFactory boltDependenciesFactory) {
        this.boltDependenciesFactory = boltDependenciesFactory;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        ruleProcessorRuntime = boltDependenciesFactory.createRuleProcessorRuntimeStorm();
    }

    @Override
    public void execute(Tuple input) {  // Input tuple is expected to be an IotasEvent
        try {
            final Object iotasEvent = input.getValueByField(IotasEvent.IOTAS_EVENT);

            if (iotasEvent instanceof IotasEvent) {
                LOG.debug("++++++++ Executing tuple [{}] which contains IotasEvent [{}]", input, iotasEvent);

                for (RuleRuntime<Tuple, OutputCollector> rule : ruleProcessorRuntime.getRulesRuntime()) {
                    if (rule.evaluate((IotasEvent) iotasEvent)) {
                        rule.execute(input, collector); // collector can be null when the rule does not forward a stream
                    }
                }
            } else {
                LOG.debug("Invalid tuple received. Tuple disregarded and rules not evaluated.\n\tTuple [{}]." +
                        "\n\tIotasEvent [{}].", input, iotasEvent);
            }
            collector.ack(input);
        } catch (Exception e) {
            collector.fail(input);
            collector.reportError(e);
            LOG.debug("", e);                        // useful to debug unit tests
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        final List<RuleRuntimeStormDeclaredOutput> declaredOutputs = boltDependenciesFactory.createDeclaredOutputs();
        for (RuleRuntimeStormDeclaredOutput declaredOutput : declaredOutputs) {
            declarer.declareStream(declaredOutput.getStreamId(), declaredOutput.getField());
        }
    }
}