package com.hortonworks.streamline.streams.runtime.storm.testing;

import com.hortonworks.streamline.streams.StreamlineEvent;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.windowing.TupleWindow;

import java.util.List;
import java.util.Map;

public class TestRunWindowProcessorBolt extends BaseWindowedBolt {
    private final String componentName;
    private final BaseWindowedBolt processorBolt;
    private final String eventLogFilePath;
    private transient TestRunEventLogger eventLogger;

    public TestRunWindowProcessorBolt(String componentName, BaseWindowedBolt processorBolt, String eventLogFilePath) {
        this.componentName = componentName;
        this.processorBolt = processorBolt;
        this.eventLogFilePath = eventLogFilePath;
    }

    @Override
    public void execute(TupleWindow tupleWindow) {
        List<Tuple> tuples = tupleWindow.get();

        if (tuples != null && !tuples.isEmpty()) {
            tuples.forEach(t -> {
                Object value = t.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
                StreamlineEvent event = (StreamlineEvent) value;
                eventLogger.writeEvent(System.currentTimeMillis(), componentName, event);
            });
        }

        processorBolt.execute(tupleWindow);
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        processorBolt.prepare(stormConf, context, collector);

        eventLogger = TestRunEventLogger.getEventLogger(eventLogFilePath);
    }

    @Override
    public void cleanup() {
        processorBolt.cleanup();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        processorBolt.declareOutputFields(declarer);
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return processorBolt.getComponentConfiguration();
    }
}
