package com.hortonworks.streamline.streams.runtime.storm.testing;

import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class TestRunSourceSpout extends BaseRichSpout {
    private static final Logger LOG = LoggerFactory.getLogger(TestRunSourceSpout.class);
    private SpoutOutputCollector _collector;

    private final TestRunSource testRunSource;
    private final Queue<Map<String, Object>> testRecordsQueue;

    public TestRunSourceSpout(String testRunSourceJson) {
        this(Utils.createObjectFromJson(testRunSourceJson, TestRunSource.class));
    }

    public TestRunSourceSpout(TestRunSource testRunSource) {
        this.testRunSource = testRunSource;
        if (testRunSource != null) {
            testRecordsQueue = new LinkedList<>(testRunSource.getTestRecords());
        } else {
            testRecordsQueue = new LinkedList<>();
        }
    }

    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        if (this.testRunSource == null) {
            throw new RuntimeException("testRunSource cannot be null");
        }
        _collector = collector;
    }

    @Override
    public void nextTuple() {
        Map<String, Object> record = testRecordsQueue.poll();
        if (record == null) {
            LOG.info("No more records, sleeping...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // no-op
            }
            return;
        }

        testRunSource.getOutputStreams().forEach(stream -> {
            StreamlineEventImpl streamlineEvent = new StreamlineEventImpl(record, testRunSource.getId());
            LOG.info("Emitting event {} to stream {}", streamlineEvent, stream.getId());
            _collector.emit(stream.getId(), new Values(streamlineEvent), streamlineEvent.getId());
        });
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        if (testRunSource == null) {
            throw new RuntimeException("testRunSource cannot be null");
        }
        Fields outputFields = getOutputFields();
        testRunSource.getOutputStreams().forEach(s -> declarer.declareStream(s.getId(), outputFields));
    }

    public Fields getOutputFields() {
        return new Fields(StreamlineEvent.STREAMLINE_EVENT);
    }
}
