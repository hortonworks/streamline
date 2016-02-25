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
package com.hortonworks.iotas.bolt.normalization;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.layout.runtime.normalization.NormalizationProcessorRuntime;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import com.hortonworks.iotas.bolt.AbstractProcessorBolt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class NormalizationBolt extends AbstractProcessorBolt {
    private static final Logger LOG = LoggerFactory.getLogger(NormalizationBolt.class);

    private NormalizationProcessorRuntime normalizationProcessorRuntime;

    public NormalizationBolt(NormalizationProcessorRuntime normalizationProcessorRuntime) {
        this.normalizationProcessorRuntime = normalizationProcessorRuntime;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        normalizationProcessorRuntime.prepare();
    }

    public void process(Tuple inputTuple, IotasEvent iotasEvent) throws Exception {
        LOG.debug("Normalizing received IotasEvent: [{}] with tuple: [{}]", iotasEvent, inputTuple);

       IotasEvent outputEvents = normalizationProcessorRuntime.execute(iotasEvent);
        LOG.debug("Emitting events to collector: [{}]", outputEvents);

        collector.emit(inputTuple.getSourceStreamId(), inputTuple, new Values(outputEvents));
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        Set<String> thisStreams = context.getThisStreams();
        for (String streamId : thisStreams) {
            declarer.declareStream(streamId, new Fields(IotasEvent.IOTAS_EVENT));
        }
    }
}
