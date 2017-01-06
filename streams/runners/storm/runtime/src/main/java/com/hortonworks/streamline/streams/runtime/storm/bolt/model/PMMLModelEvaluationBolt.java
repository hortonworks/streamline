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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.streamline.streams.runtime.storm.bolt.model;

import org.apache.storm.pmml.model.jpmml.JpmmlModelOutputs;
import org.apache.storm.pmml.runner.jpmml.JPmmlModelRunner;
import org.apache.storm.pmml.runner.jpmml.JpmmlFactory;
import org.apache.storm.shade.com.google.common.base.Preconditions;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.layout.component.impl.model.ModelProcessor;
import com.hortonworks.streamline.streams.layout.component.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class PMMLModelEvaluationBolt extends BaseRichBolt {
    protected static final Logger LOG = LoggerFactory.getLogger(PMMLModelEvaluationBolt.class);
    private final ModelProcessor modelProcessor;
    private JPmmlModelRunner modelRunner;
    private OutputCollector collector;

    public PMMLModelEvaluationBolt(ModelProcessor modelProcessor) {
        Preconditions.checkNotNull(modelProcessor);
        this.modelProcessor = modelProcessor;
    }

    public PMMLModelEvaluationBolt(String modelProcessorJson) throws Exception {
        this(Utils.createObjectFromJson(modelProcessorJson, ModelProcessor.class));
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        try {
            InputStream modelInputStream = new ByteArrayInputStream(
                    modelProcessor.getPmml().getBytes());
            this.modelRunner = new StreamlineJPMMLModelRunner(
                    modelProcessor.getOutputStreams(),
                    modelProcessor.getId(),
                    JpmmlFactory.newEvaluator(modelInputStream),
                    JpmmlModelOutputs.toDefaultStream(modelInputStream));
            this.collector = collector;
        } catch (Exception e) {
            LOG.error("Unexpected exception while preparing the model evaluation bolt", e);
            throw new RuntimeException("Unexpected exception while preparing the model evaluation bolt", e);
        }
    }

    @Override
    public void execute(Tuple inputTuple) {
        try {
            Map<String, List<Object>> results = modelRunner.scoredTuplePerStream(inputTuple);
            for (Map.Entry<String, List<Object>> result : results.entrySet()) {
                LOG.debug("Emitting tuples in the following stream: [{}]", result.getKey());
                collector.emit(result.getKey(), inputTuple, new Values(result.getValue()));
            }
        } catch (Exception exception) {
            LOG.error("There was an error while evaluation the tuple [{}]", inputTuple);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        for (Stream stream : modelProcessor.getOutputStreams()) {
            declarer.declareStream(stream.getId(), new Fields(StreamlineEvent.STREAMLINE_EVENT));
        }
    }
}
