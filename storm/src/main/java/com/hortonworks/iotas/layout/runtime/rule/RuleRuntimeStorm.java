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
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.runtime.rule.condition.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a rule runtime in the {@code Storm} streaming framework
 */
public class RuleRuntimeStorm extends RuleRuntime<Tuple, OutputCollector> {
    private static final Logger log = LoggerFactory.getLogger(RuleRuntimeStorm.class);

    RuleRuntimeStorm(Rule rule, Script<IotasEvent, ?> script) {
        super(rule, script);
    }

    public void execute(Tuple input, OutputCollector collector) {
        collector.emit(getStreamId(), input, input.getValues());
        log.debug("Executed rule [{}]\n\tInput tuple [{}]\n\tCollector [{}]\n\tStream:[{}]",
                rule, input, collector, getStreamId());
    }

    public void declareOutput(OutputFieldsDeclarer declarer) {
        final String streamId = getStreamId();
        final Fields fields = getFields();
        declarer.declareStream(streamId, fields);
        log.debug("Declared stream. Stream Id [{}] Fields [{}]", streamId, fields);
    }

    public String getStreamId() {
        return rule.getRuleProcessorName() + "." + rule.getName() + "." + rule.getId();
    }

    private Fields getFields() {
        return new Fields(IotasEvent.IOTAS_EVENT);
    }
}
