package com.hortonworks.streamline.streams.runtime.storm.testing;

import com.hortonworks.streamline.streams.StreamlineEvent;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;

import java.util.Map;

public class TestRunProcessorBolt extends BaseRichBolt {
    private final String componentName;
    private final BaseRichBolt processorBolt;
    private final String eventLogFilePath;
    private transient TestRunEventLogger eventLogger;

    public TestRunProcessorBolt(String componentName, BaseRichBolt processorBolt, String eventLogFilePath) {
        this.componentName = componentName;
        this.processorBolt = processorBolt;
        this.eventLogFilePath = eventLogFilePath;
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        processorBolt.prepare(map, topologyContext, outputCollector);

        eventLogger = TestRunEventLogger.getEventLogger(eventLogFilePath);
    }

    @Override
    public void execute(Tuple tuple) {
        Object value = tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        StreamlineEvent event = (StreamlineEvent) value;
        eventLogger.writeEvent(System.currentTimeMillis(), componentName, event);

        processorBolt.execute(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        processorBolt.declareOutputFields(outputFieldsDeclarer);
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return processorBolt.getComponentConfiguration();
    }

    @Override
    public void cleanup() {
        processorBolt.cleanup();
    }
}
