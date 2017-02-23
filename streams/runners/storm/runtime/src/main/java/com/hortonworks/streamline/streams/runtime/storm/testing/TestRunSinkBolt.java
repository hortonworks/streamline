package com.hortonworks.streamline.streams.runtime.storm.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import org.apache.commons.lang3.StringUtils;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class TestRunSinkBolt extends BaseRichBolt {
    private static final Logger LOG = LoggerFactory.getLogger(TestRunSinkBolt.class);

    private transient OutputCollector collector;
    private transient ObjectMapper objectMapper;

    private final TestRunSink testRunSink;

    public TestRunSinkBolt(String testRunSinkJson) {
        this(Utils.createObjectFromJson(testRunSinkJson, TestRunSink.class));
    }

    public TestRunSinkBolt(TestRunSink testRunSink) {
        this.testRunSink = testRunSink;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        if (this.testRunSink == null) {
            throw new RuntimeException("testRunSink cannot be null");
        }
        if (StringUtils.isEmpty(this.testRunSink.getOutputFilePath())) {
            throw new RuntimeException("output file path cannot be null or empty");
        }

        this.collector = collector;
        this.objectMapper = new ObjectMapper();

        String outputFilePath = testRunSink.getOutputFilePath();
        try (FileWriter fw = new FileWriter(outputFilePath, true)) {
            // no op
        } catch (IOException e) {
            LOG.error("Can't open file for preparing to write: " + outputFilePath);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(Tuple input) {
        String outputFilePath = testRunSink.getOutputFilePath();
        try (FileWriter fw = new FileWriter(outputFilePath, true)) {
            LOG.info("writing event to file " + outputFilePath);
            Object value = input.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
            StreamlineEvent event = (StreamlineEvent) value;
            fw.write(objectMapper.writeValueAsString(event) + "\n");
            fw.flush();
            collector.ack(input);
        } catch (FileNotFoundException e) {
            LOG.error("Can't open file for write: " + outputFilePath);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOG.error("Fail to write event to output file " + outputFilePath + " : exception occurred.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        // nothing to emit
    }
}
