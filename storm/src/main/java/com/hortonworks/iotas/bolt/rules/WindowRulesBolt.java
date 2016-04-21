package com.hortonworks.iotas.bolt.rules;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.layout.design.rule.condition.Window;
import com.hortonworks.iotas.layout.runtime.processor.RuleProcessorRuntime;
import com.hortonworks.iotas.layout.runtime.rule.RulesBoltDependenciesFactory;
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

import static com.hortonworks.iotas.layout.design.rule.condition.Window.WINDOW_ID;
import static com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransform.HEADER_FIELD_DATASOURCE_IDS;
import static com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransform.HEADER_FIELD_EVENT_IDS;

/**
 * A windowed rules bolt
 */
public class WindowRulesBolt extends BaseWindowedBolt {
    private static final Logger LOG = LoggerFactory.getLogger(RulesBolt.class);

    private RuleProcessorRuntime ruleProcessorRuntime;

    private final RulesBoltDependenciesFactory boltDependenciesFactory;

    private OutputCollector collector;

    private long windowId = 0;

    public WindowRulesBolt(RulesBoltDependenciesFactory boltDependenciesFactory) {
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
        LOG.debug("Window activated, window id {}", windowId);
        try {
            IotasEvent event;
            for (Tuple input : inputWindow.get()) {
                if ((event = getIotasEventFromTuple(input)) != null) {
                    LOG.debug("++++++++ Executing tuple [{}] which contains IotasEvent [{}]", input, event);
                    ruleProcessorRuntime.process(iotasEventWithWindowId((IotasEvent) event));
                }
            }
            // force evaluation of group by emitting null
            for (Result result : ruleProcessorRuntime.process(null)) {
                for (IotasEvent e : result.events) {
                    // TODO: updateHeaders can be handled at ruleProcessorRuntime.process stage passing context info.
                    collector.emit(result.stream, new Values(updateHeaders(e, inputWindow.get())));
                }
            }
        } catch (Exception e) {
            collector.reportError(e);
            LOG.debug("", e);                        // useful to debug unit tests
        }
    }

    private IotasEvent updateHeaders(IotasEvent event, List<Tuple> tuples) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADER_FIELD_EVENT_IDS, getEventIds(tuples));
        headers.put(HEADER_FIELD_DATASOURCE_IDS, getDataSourceIds(tuples));
        event = event.putHeaders(headers);
        return event;
    }

    private List<String> getEventIds(List<Tuple> tuples) {
        Set<String> res = new HashSet<>();
        IotasEvent event;
        for (Tuple tuple : tuples) {
            if ((event = getIotasEventFromTuple(tuple)) != null) {
                res.add(event.getId());
            }
        }
        return new ArrayList<>(res);
    }

    private List<String> getDataSourceIds(List<Tuple> tuples) {
        Set<String> res = new HashSet<>();
        IotasEvent event;
        for (Tuple tuple : tuples) {
            if ((event = getIotasEventFromTuple(tuple)) != null) {
                res.add(event.getDataSourceId());
            }
        }
        return new ArrayList<>(res);
    }

    private IotasEvent getIotasEventFromTuple(Tuple tuple) {
        final Object iotasEvent = tuple.getValueByField(IotasEvent.IOTAS_EVENT);
        if (iotasEvent instanceof IotasEvent) {
            return (IotasEvent) iotasEvent;
        } else {
            LOG.debug("Invalid tuple received. Tuple disregarded and rules not evaluated.\n\tTuple [{}]." +
                              "\n\tIotasEvent [{}].", tuple, iotasEvent);
        }
        return null;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        for (String stream : boltDependenciesFactory.createRuleProcessorRuntime().getStreams()) {
            declarer.declareStream(stream, new Fields(IotasEvent.IOTAS_EVENT));
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

    private IotasEvent iotasEventWithWindowId(final IotasEvent event) {
        return event.putFieldsAndValues(Collections.<String, Object>singletonMap(WINDOW_ID, windowId));
    }
}
