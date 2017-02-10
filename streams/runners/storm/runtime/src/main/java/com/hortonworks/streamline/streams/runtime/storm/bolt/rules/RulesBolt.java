/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/


package com.hortonworks.streamline.streams.runtime.storm.bolt.rules;

import com.hortonworks.streamline.common.Constants;
import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.runtime.processor.RuleProcessorRuntime;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class RulesBolt extends BaseRichBolt {
    private static final Logger LOG = LoggerFactory.getLogger(RulesBolt.class);

    private RuleProcessorRuntime ruleProcessorRuntime;
    private final RulesProcessor rulesProcessor;
    private final RuleProcessorRuntime.ScriptType scriptType;

    private OutputCollector collector;

    public RulesBolt(RulesProcessor rulesProcessor, RuleProcessorRuntime.ScriptType scriptType) {
        this.rulesProcessor = rulesProcessor;
        this.scriptType = scriptType;
    }

    public RulesBolt(String rulesProcessorJson, RuleProcessorRuntime.ScriptType scriptType) {
        this(Utils.createObjectFromJson(rulesProcessorJson, RulesProcessor.class), scriptType);
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        if (this.rulesProcessor == null) {
            throw new RuntimeException("rulesProcessor cannot be null");
        }
        this.collector = collector;
        ruleProcessorRuntime = new RuleProcessorRuntime(rulesProcessor, scriptType);

        Map<String, Object> config = Collections.emptyMap();
        if (stormConf != null) {
            config = new HashMap<>();
            config.put(Constants.CATALOG_ROOT_URL, stormConf.get(Constants.CATALOG_ROOT_URL));
            config.put(Constants.LOCAL_FILES_PATH, stormConf.get(Constants.LOCAL_FILES_PATH));
        }
        ruleProcessorRuntime.initialize(config);
    }

    @Override
    public void execute(Tuple input) {  // Input tuple is expected to be an StreamlineEvent
        try {
            final Object event = input.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
            if (event instanceof StreamlineEvent) {
                StreamlineEvent eventWithStream = getStreamlineEventWithStream((StreamlineEvent) event, input);
                LOG.debug("++++++++ Executing tuple [{}], StreamlineEvent [{}]", input, eventWithStream);
                for (Result result : ruleProcessorRuntime.process(eventWithStream)) {
                    for (StreamlineEvent e : result.events) {
                        collector.emit(result.stream, input, new Values(e));
                    }
                }
            } else {
                LOG.debug("Invalid tuple received. Tuple disregarded and rules not evaluated.\n\tTuple [{}]." +
                        "\n\tStreamlineEvent [{}].", input, event);
            }
            collector.ack(input);
        } catch (Exception e) {
            collector.fail(input);
            collector.reportError(e);
            LOG.debug("", e);                        // useful to debug unit tests
        }
    }

    private StreamlineEvent getStreamlineEventWithStream(StreamlineEvent event, Tuple tuple) {
        return new StreamlineEventImpl(event,
                event.getDataSourceId(), event.getId(),
                event.getHeader(), tuple.getSourceStreamId(), event.getAuxiliaryFieldsAndValues());
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        if (this.rulesProcessor == null) {
            throw new RuntimeException("rulesProcessor cannot be null");
        }
        for (Stream stream : rulesProcessor.getOutputStreams()) {
            declarer.declareStream(stream.getId(), new Fields(StreamlineEvent.STREAMLINE_EVENT));
        }
    }
}
