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

import com.hortonworks.iotas.bolt.AbstractProcessorBolt;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.layout.design.normalization.NormalizationProcessor;
import com.hortonworks.iotas.topology.component.Stream;
import com.hortonworks.iotas.layout.design.normalization.NormalizationProcessorJsonBuilder;
import com.hortonworks.iotas.layout.runtime.normalization.NormalizationProcessorRuntime;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class NormalizationBolt extends AbstractProcessorBolt {
    private static final Logger LOG = LoggerFactory.getLogger(NormalizationBolt.class);
    private final NormalizationProcessor normalizationProcessor;

    private NormalizationProcessorRuntime normalizationProcessorRuntime;

    public NormalizationBolt(NormalizationProcessor normalizationProcessor) {
        this.normalizationProcessor = normalizationProcessor;
    }

    public NormalizationBolt(NormalizationProcessorJsonBuilder normalizationProcessorJsonBuilder) {
        this.normalizationProcessor = normalizationProcessorJsonBuilder.build();
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        normalizationProcessorRuntime = new NormalizationProcessorRuntime(normalizationProcessor);
        normalizationProcessorRuntime.initialize(Collections.<String, Object>emptyMap());
    }

    public void process(Tuple inputTuple, IotasEvent iotasEvent) throws Exception {
        LOG.debug("Normalizing received IotasEvent: [{}] with tuple: [{}]", iotasEvent, inputTuple);
        //todo this bolt will be replaced with custom baseprocessor bolt.
        IotasEventImpl iotasEventWithStream = new IotasEventImpl(iotasEvent.getFieldsAndValues(), iotasEvent.getDataSourceId(),
                iotasEvent.getId(), iotasEvent.getHeader(), inputTuple.getSourceStreamId());
        List<Result> outputEvents = normalizationProcessorRuntime.process(iotasEventWithStream);
        LOG.debug("Emitting events to collector: [{}]", outputEvents);
        for (Result outputEvent : outputEvents) {
            for (IotasEvent event : outputEvent.events) {
                collector.emit(outputEvent.stream, inputTuple, new Values(event));
            }
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        for (Stream stream : normalizationProcessor.getOutputStreams()) {
            declarer.declareStream(stream.getId(), new Fields(IotasEvent.IOTAS_EVENT));
        }
    }
}
