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
package com.hortonworks.bolt.n11n;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.layout.runtime.n11n.NormalizationProcessorRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class NormalizationBolt extends BaseRichBolt {
    private static final Logger LOG = LoggerFactory.getLogger(NormalizationBolt.class);

    private TopologyContext context;
    private OutputCollector collector;
    private NormalizationProcessorRuntime normalizationProcessorRuntime;

    public NormalizationBolt(NormalizationProcessorRuntime normalizationProcessorRuntime) {
        this.normalizationProcessorRuntime = normalizationProcessorRuntime;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.context = context;
        this.collector = collector;
    }

    @Override
    public void execute(Tuple input) {
        IotasEvent iotasEvent = (IotasEvent) input.getValueByField(IotasEvent.IOTAS_EVENT);
        if(iotasEvent == null) {
            throw new IllegalArgumentException("input tuple should contain a field with name '"+IotasEvent.IOTAS_EVENT+"'");
        }

        try {
            Map<String, Object> resultMap = normalizationProcessorRuntime.execute(iotasEvent);
            IotasEventImpl newIotasEvent = new IotasEventImpl(resultMap, iotasEvent.getDataSourceId(), iotasEvent.getId());
            collector.emit(input.getSourceStreamId(), input, new Values(newIotasEvent));

            collector.ack(input);
        } catch (Exception e) {
            LOG.error("Error occurred while normalizing the tuple", e);
            collector.fail(input);
            collector.reportError(e);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        if(context == null) {
            throw new IllegalStateException("TopologyContext is null, prepare may not have been called.");
        }

        Set<String> thisStreams = context.getThisStreams();
        for (String streamId : thisStreams) {
            declarer.declareStream(streamId, new Fields(IotasEvent.IOTAS_EVENT));
        }
    }
}
