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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        return toStreamLineEvents(predScores, input);
    }

    private Map<String, List<Object>> toStreamLineEvents(Map<FieldName, ?> predScores, final Tuple input) {
        LOG.debug("Processing tuple {}", input);
        final Set<String> inserted = new HashSet<>();
        final Map<String, List<Object>> streamsToEvents = new HashMap<>();
        final StreamlineEventImpl.Builder eventBuilder = StreamlineEventImpl.builder();

        // add to StreamlineEvent the predicted scores for PMML model predicted fields
        putPmmlScoresInEvent(predScores, inserted, eventBuilder, getPredictedFields(),
                "Added PMML predicted (field,val)=({},{}) to StreamlineEvent");

        // add to StreamlineEvent the predicted scores for PMML model output fields
        putPmmlScoresInEvent(predScores, inserted, eventBuilder, getOutputFields(),
                "Added PMML output (field,val)=({},{}) to StreamlineEvent");

        final StreamlineEvent scoredEvent = eventBuilder.build();
        LOG.debug("Scored StreamlineEvent {}", scoredEvent);

        final StreamlineEvent eventInTuple = getStreamlineEventFromTuple(input);
        for (Stream stream : outputStreams) {
            // Will contain scored and non scored events that match output fields
            final StreamlineEventImpl.Builder finalEventBuilder = StreamlineEventImpl.builder();
            finalEventBuilder.putAll(scoredEvent);

            if (eventInTuple != null) {
                // Add previous tuple's StreamlineEvent to this tuple's StreamlineEvent to pass it downstream
                Map<String, Object> nonScoredFieldsEvent = eventInTuple.entrySet().stream()
                        .filter((e) ->
                                // include only tuple fields untouched by the PMML model
                                !inserted.contains(e.getKey())
                                // include only tuple fields that are in output fields, i.e. were chosen by the user in the UI
                                && stream.getSchema().getFields().stream().anyMatch((of) -> of.getName().equals(e.getKey())))
                        .peek((e) -> LOG.debug("Adding entry {} to StreamlineEvent", e))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                if (nonScoredFieldsEvent != null) {
                    finalEventBuilder.putAll(nonScoredFieldsEvent);
                }
            }
            streamsToEvents.put(stream.getId(), Collections.singletonList(finalEventBuilder.dataSourceId(modelId).build()));
        }
        return streamsToEvents;
    }

    private StreamlineEvent getStreamlineEventFromTuple(Tuple input) {
        Object event = null;
        if (input != null && input.contains(StreamlineEvent.STREAMLINE_EVENT)) {
             event = input.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
            if(event instanceof StreamlineEvent) {
                return (StreamlineEvent) event;
            }
        }
        LOG.debug("Ignoring input tuple field [{}] because it does not contain object of type StreamlineEvent [{}]",
                StreamlineEvent.STREAMLINE_EVENT, event);
        return null;
    }

    private void putPmmlScoresInEvent(Map<FieldName, ?> predScores, Set<String> inserted,
            StreamlineEventImpl.Builder eventBuilder, List<FieldName> predOrOutFields, String msg) {

        for (FieldName predOrOutField : predOrOutFields) {
            final Object targetValue = predScores.get(predOrOutField);
            final String fieldName = predOrOutField.getValue();
            final Object predValue = EvaluatorUtil.decode(targetValue);
            eventBuilder.put(fieldName, predValue);
            LOG.debug(msg, fieldName, predValue);
            inserted.add(fieldName);
        }
    }
}
