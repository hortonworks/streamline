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

import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.common.IdPreservedStreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.common.event.correlation.EventCorrelationInjector;
import com.hortonworks.streamline.streams.exception.ProcessingException;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Window;
import com.hortonworks.streamline.streams.runtime.processor.RuleProcessorRuntime;
import com.hortonworks.streamline.streams.runtime.storm.bolt.AbstractWindowedProcessorBolt;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;
import com.hortonworks.streamline.streams.runtime.storm.bolt.StreamlineWindowedBolt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hortonworks.streamline.streams.common.StreamlineEventImpl.GROUP_BY_TRIGGER_EVENT;
import static com.hortonworks.streamline.streams.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_DATASOURCE_IDS;
import static com.hortonworks.streamline.streams.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_EVENT_IDS;

/**
 * A windowed rules bolt
 */
public class WindowRulesBolt extends AbstractWindowedProcessorBolt {
    private static final Logger LOG = LoggerFactory.getLogger(WindowRulesBolt.class);

    private RuleProcessorRuntime ruleProcessorRuntime;
    private final RulesProcessor rulesProcessor;
    private final RuleProcessorRuntime.ScriptType scriptType;
    private long windowId;

    public WindowRulesBolt(RulesProcessor rulesProcessor, RuleProcessorRuntime.ScriptType scriptType) {
        this.rulesProcessor = rulesProcessor;
        this.scriptType = scriptType;
    }

    public WindowRulesBolt(String rulesProcessorJson, RuleProcessorRuntime.ScriptType scriptType) {
        this(Utils.createObjectFromJson(rulesProcessorJson, RulesProcessor.class), scriptType);
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);

        if (this.rulesProcessor == null) {
            throw new RuntimeException("rulesProcessor cannot be null");
        }
        ruleProcessorRuntime = new RuleProcessorRuntime(rulesProcessor, scriptType);
        Map<String, Object> config = Collections.emptyMap();
        ruleProcessorRuntime.initialize(config);
    }

    /**
     * Process the tuple window and optionally emit new tuples based on the tuples in the input window.
     *
     * @param inputWindow
     */
    @Override
    public void execute(TupleWindow inputWindow) {
        ++windowId;
        LOG.debug("Window activated, window id {}, number of tuples in window {}", windowId, inputWindow.get().size());

        Map<String, Tuple> eventIdToTupleMap = new HashMap<>();
        try {
            StreamlineEvent event;
            for (Tuple input : inputWindow.get()) {
                if ((event = getStreamlineEventFromTuple(input)) != null) {
                    LOG.debug("++++++++ Executing tuple [{}] which contains StreamlineEvent [{}]", input, event);
                    eventIdToTupleMap.put(event.getId(), input);
                    processAndEmit(event, eventIdToTupleMap);
                }
            }
            // force evaluation of the last group by
            processAndEmit(GROUP_BY_TRIGGER_EVENT, eventIdToTupleMap);

            // current group is processed and result emitted
            eventIdToTupleMap.clear();
        } catch (Exception e) {
            collector.reportError(e);
            LOG.error("", e);
        }
    }

    private void processAndEmit(StreamlineEvent event, Map<String, Tuple> curGroup) throws ProcessingException {
        List<Result> results = ruleProcessorRuntime.process(eventWithWindowId(event));
        for (Result result : results) {
            for (StreamlineEvent e : result.events) {
                // TODO: updateHeaders can be handled at ruleProcessorRuntime.process stage passing context info.

                // anchor parent events if such information is available
                if (EventCorrelationInjector.containsParentIds(e)) {
                    Set<String> parentIds = EventCorrelationInjector.getParentIds(e);

                    List<Tuple> parents = parentIds.stream().map(pid -> {
                        if (!curGroup.containsKey(pid)) {
                            throw new IllegalStateException("parents should be in grouped tuples");
                        }
                        return curGroup.get(pid);
                    }).collect(Collectors.toList());
                    collector.emit(result.stream, parents, new Values(updateHeaders(e, parents)));
                } else {
                    // put all events in current group if there's no information
                    collector.emit(result.stream, curGroup.values(), new Values(updateHeaders(e, curGroup.values())));
                }
            }
        }
    }

    private StreamlineEvent updateHeaders(StreamlineEvent event, Collection<Tuple> tuples) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADER_FIELD_EVENT_IDS, getEventIds(tuples));
        headers.put(HEADER_FIELD_DATASOURCE_IDS, getDataSourceIds(tuples));
        event = event.addHeaders(headers);
        return event;
    }

    private List<String> getEventIds(Collection<Tuple> tuples) {
        Set<String> res = new HashSet<>();
        StreamlineEvent event;
        for (Tuple tuple : tuples) {
            if ((event = getStreamlineEventFromTuple(tuple)) != null) {
                res.add(event.getId());
            }
        }
        return new ArrayList<>(res);
    }

    private List<String> getDataSourceIds(Collection<Tuple> tuples) {
        Set<String> res = new HashSet<>();
        StreamlineEvent event;
        for (Tuple tuple : tuples) {
            if ((event = getStreamlineEventFromTuple(tuple)) != null) {
                res.add(event.getDataSourceId());
            }
        }
        return new ArrayList<>(res);
    }

    private StreamlineEvent getStreamlineEventFromTuple(Tuple tuple) {
        final Object event = tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        if (event instanceof StreamlineEvent) {
            return getStreamlineEventWithStream((StreamlineEvent) event, tuple);
        } else {
            LOG.debug("Invalid tuple received. Tuple disregarded and rules not evaluated.\n\tTuple [{}]." +
                              "\n\tStreamlineEvent [{}].", tuple, event);
        }
        return null;
    }

    private StreamlineEvent getStreamlineEventWithStream(StreamlineEvent event, Tuple tuple) {
        StreamlineEventImpl newEvent = StreamlineEventImpl.builder().from(event).sourceStream(tuple.getSourceStreamId()).build();
        return new IdPreservedStreamlineEvent(newEvent, event.getId());
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


    private StreamlineEvent eventWithWindowId(final StreamlineEvent event) {
        if (event == GROUP_BY_TRIGGER_EVENT) {
            return event;
        }
        StreamlineEvent newEvent = event.addFieldsAndValues(Collections.<String, Object>singletonMap(Window.WINDOW_ID, windowId));
        return new IdPreservedStreamlineEvent(newEvent, event.getId());
    }
}
