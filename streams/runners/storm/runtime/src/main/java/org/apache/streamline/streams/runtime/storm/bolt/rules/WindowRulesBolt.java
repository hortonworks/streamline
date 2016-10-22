package org.apache.streamline.streams.runtime.storm.bolt.rules;

import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.Result;
import org.apache.streamline.streams.common.StreamlineEventImpl;
import org.apache.streamline.streams.exception.ProcessingException;
import org.apache.streamline.streams.layout.component.rule.expression.Window;
import org.apache.streamline.streams.runtime.processor.RuleProcessorRuntime;
import org.apache.streamline.streams.runtime.rule.RulesDependenciesFactory;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.apache.streamline.streams.common.StreamlineEventImpl.GROUP_BY_TRIGGER_EVENT;
import static org.apache.streamline.streams.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_DATASOURCE_IDS;
import static org.apache.streamline.streams.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_EVENT_IDS;

/**
 * A windowed rules bolt
 */
public class WindowRulesBolt extends BaseWindowedBolt {
    private static final Logger LOG = LoggerFactory.getLogger(WindowRulesBolt.class);

    private RuleProcessorRuntime ruleProcessorRuntime;

    private final RulesDependenciesFactory boltDependenciesFactory;

    private OutputCollector collector;

    private long windowId;

    public WindowRulesBolt(RulesDependenciesFactory boltDependenciesFactory) {
        this.boltDependenciesFactory = boltDependenciesFactory;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        ruleProcessorRuntime = boltDependenciesFactory.createRuleProcessorRuntime();
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
        List<Tuple> curGroup = new ArrayList<>();
        try {
            StreamlineEvent event;
            for (Tuple input : inputWindow.get()) {
                if ((event = getStreamlineEventFromTuple(input)) != null) {
                    LOG.debug("++++++++ Executing tuple [{}] which contains StreamlineEvent [{}]", input, event);
                    processAndEmit(event, curGroup);
                    curGroup.add(input);
                }
            }
            // force evaluation of the last group by
            processAndEmit(GROUP_BY_TRIGGER_EVENT, curGroup);
        } catch (Exception e) {
            collector.reportError(e);
            LOG.debug("", e);                        // useful to debug unit tests
        }
    }

    private void processAndEmit(StreamlineEvent event, List<Tuple> curGroup) throws ProcessingException {
        for (Result result : ruleProcessorRuntime.process(eventWithWindowId(event))) {
            for (StreamlineEvent e : result.events) {
                // TODO: updateHeaders can be handled at ruleProcessorRuntime.process stage passing context info.
                collector.emit(result.stream, new Values(updateHeaders(e, curGroup)));
            }
            curGroup.clear(); // current group is processed and result emitted
        }
    }

    private StreamlineEvent updateHeaders(StreamlineEvent event, List<Tuple> tuples) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADER_FIELD_EVENT_IDS, getEventIds(tuples));
        headers.put(HEADER_FIELD_DATASOURCE_IDS, getDataSourceIds(tuples));
        event = event.addHeaders(headers);
        return event;
    }

    private List<String> getEventIds(List<Tuple> tuples) {
        Set<String> res = new HashSet<>();
        StreamlineEvent event;
        for (Tuple tuple : tuples) {
            if ((event = getStreamlineEventFromTuple(tuple)) != null) {
                res.add(event.getId());
            }
        }
        return new ArrayList<>(res);
    }

    private List<String> getDataSourceIds(List<Tuple> tuples) {
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
        return new StreamlineEventImpl(event.getFieldsAndValues(),
                event.getDataSourceId(), event.getId(),
                event.getHeader(), tuple.getSourceStreamId(), event.getAuxiliaryFieldsAndValues());
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        for (String stream : boltDependenciesFactory.createRuleProcessorRuntime().getStreams()) {
            declarer.declareStream(stream, new Fields(StreamlineEvent.STREAMLINE_EVENT));
        }
    }

    public void withWindowConfig(Window windowConfig) throws IOException {
        if (windowConfig.getWindowLength() instanceof Window.Duration) {
            Duration windowLength = new Duration(((Window.Duration) windowConfig.getWindowLength()).getDurationMs(), TimeUnit.MILLISECONDS);
            if (windowConfig.getSlidingInterval() instanceof Window.Duration) {
                Duration slidingInterval = new Duration(((Window.Duration) windowConfig.getSlidingInterval()).getDurationMs(), TimeUnit.MILLISECONDS);
                withWindow(windowLength, slidingInterval);
            } else if (windowConfig.getSlidingInterval() instanceof Window.Count) {
                Count slidingInterval = new Count(((Window.Count) windowConfig.getSlidingInterval()).getCount());
                withWindow(windowLength, slidingInterval);
            } else {
                withWindow(windowLength);
            }
        } else if (windowConfig.getWindowLength() instanceof Window.Count) {
            Count windowLength = new Count(((Window.Count) windowConfig.getWindowLength()).getCount());
            if (windowConfig.getSlidingInterval() instanceof Window.Duration) {
                Duration slidingInterval = new Duration(((Window.Duration) windowConfig.getWindowLength()).getDurationMs(), TimeUnit.MILLISECONDS);
                withWindow(windowLength, slidingInterval);
            } else if (windowConfig.getSlidingInterval() instanceof Window.Count) {
                Count slidingInterval = new Count(((Window.Count) windowConfig.getWindowLength()).getCount());
                withWindow(windowLength, slidingInterval);
            } else {
                withWindow(windowLength);
            }
        }

        if (windowConfig.getLagMs() != 0) {
            withLag(new Duration(windowConfig.getLagMs(), TimeUnit.MILLISECONDS));
        }

        if (windowConfig.getTsField() != null) {
            withTimestampField(windowConfig.getTsField());
        }
    }

    private StreamlineEvent eventWithWindowId(final StreamlineEvent event) {
        if (event == GROUP_BY_TRIGGER_EVENT) {
            return event;
        }
        return event.addFieldsAndValues(Collections.<String, Object>singletonMap(Window.WINDOW_ID, windowId));
    }
}
