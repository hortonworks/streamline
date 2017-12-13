/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.hortonworks.streamline.streams.runtime.storm.bolt.model;

import com.hortonworks.streamline.streams.runtime.storm.event.correlation.EventCorrelatingOutputCollector;
import org.apache.storm.pmml.PMMLPredictorBolt;
import org.apache.storm.pmml.model.ModelOutputs;
import org.apache.storm.pmml.runner.ModelRunnerFactory;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;

import java.util.Map;

/**
 * All processors need to extend either AbstractProcessorBolt or AbstractWindowedProcessorBolt to support
 * event correlation, but the class already extends Storm's PMMLPredictorBolt, hence we can't extend the class.
 * Instead, we override prepare() method and wrap collector directly.
 */
public class StreamlinePMMLPredictorBolt extends PMMLPredictorBolt {
    /**
     * Creates an instance of {@link PMMLPredictorBolt} that executes, for every tuple, the runner constructed with
     * the {@link ModelRunnerFactory} specified in the parameter
     * The {@link PMMLPredictorBolt} instantiated with this constructor declares the output fields as specified
     * by the {@link ModelOutputs} parameter
     *
     * @param modelRunnerFactory
     * @param modelOutputs
     */
    public StreamlinePMMLPredictorBolt(ModelRunnerFactory modelRunnerFactory, ModelOutputs modelOutputs) {
        super(modelRunnerFactory, modelOutputs);
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, new EventCorrelatingOutputCollector(context, collector));
    }
}
