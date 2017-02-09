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

package com.hortonworks.streamline.streams.runtime.storm.bolt.model;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.layout.component.Stream;

import org.apache.storm.pmml.model.ModelOutputs;
import org.apache.storm.pmml.runner.jpmml.JPmmlModelRunner;
import org.apache.storm.tuple.Tuple;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.FieldValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamlineJPMMLModelRunner extends JPmmlModelRunner {
    private static final Logger LOG = LoggerFactory.getLogger(StreamlineJPMMLModelRunner.class);
    private final String modelId;
    private final Set<Stream> outputStreams;

    public StreamlineJPMMLModelRunner(Set<Stream> outputStreams, String modelId, Evaluator evaluator, ModelOutputs modelOutputs) {
        super(evaluator, modelOutputs);
        this.modelId = modelId;
        this.outputStreams = outputStreams;
    }

    /**
     * @return The raw inputs extracted from the tuple for all 'active fields'
     */
    @Override
    public Map<FieldName, Object> extractRawInputs(Tuple tuple) {
        LOG.debug("Extracting raw inputs from tuple: = [{}]", tuple);
        Object event = tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        final Map<FieldName, Object> rawInputs = new LinkedHashMap<>();
        if(event instanceof StreamlineEvent) {
            StreamlineEvent streamlineEvent = (StreamlineEvent) event;
            for (FieldName fieldName : getActiveFields()) {
                if (streamlineEvent.containsKey(fieldName.getValue())) {
                    rawInputs.put(fieldName, streamlineEvent.get(fieldName.getValue()));
                }
            }
        } else {
            LOG.debug("Not processing invalid input tuple:[{}] with streamline event:[{}]", tuple, event);
        }

        LOG.debug("Raw inputs = [{}]", rawInputs);
        return rawInputs;
    }

    @Override
    public Map<String, List<Object>> scoredTuplePerStream(Tuple input) {
        final Map<FieldName, Object> rawInputs = extractRawInputs(input);
        final Map<FieldName, FieldValue> preProcInputs = preProcessInputs(rawInputs);
        final Map<FieldName, ?> predScores = predictScores(preProcInputs);

        return toStreamLineEvents(predScores);
    }

    private Map<String, List<Object>> toStreamLineEvents(Map<FieldName, ?> predScores) {
        Map<String, List<Object>> streamsToEvents = new HashMap<>();
        final Map<String, Object> scoredVals = new LinkedHashMap<>();

        for (FieldName predictedField : getPredictedFields()) {
            Object targetValue = predScores.get(predictedField);
            scoredVals.put(predictedField.getValue(), EvaluatorUtil.decode(targetValue));
        }

        for (FieldName outputField : getOutputFields()) {
            Object targetValue = predScores.get(outputField);
            scoredVals.put(outputField.getValue(), EvaluatorUtil.decode(targetValue));
        };

        for (Stream stream : outputStreams) {
            streamsToEvents.put(stream.getId(), Collections.singletonList(new StreamlineEventImpl(scoredVals, modelId)));
        }

        return streamsToEvents;
    }
}
