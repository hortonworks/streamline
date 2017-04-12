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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class TestRunSourceSpout extends BaseRichSpout {
    private static final Logger LOG = LoggerFactory.getLogger(TestRunSourceSpout.class);
    private SpoutOutputCollector _collector;

    private final TestRunSource testRunSource;
    private final Map<String, Queue<Map<String, Object>>> testRecordsQueueMap;

    public TestRunSourceSpout(String testRunSourceJson) {
        this(Utils.createObjectFromJson(testRunSourceJson, TestRunSource.class));
    }

    public TestRunSourceSpout(TestRunSource testRunSource) {
        this.testRunSource = testRunSource;
        testRecordsQueueMap = new HashMap<>();
        if (testRunSource != null) {
            Map<String, List<Map<String, Object>>> testRecords = testRunSource.getTestRecordsForEachStream();
            for (Map.Entry<String, List<Map<String, Object>>> entry : testRecords.entrySet()) {
                testRecordsQueueMap.put(entry.getKey(), new LinkedList<>(entry.getValue()));
            }
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
        int emitCount = 0;

        // loop output stream and emit at most one record per output stream
        for (Map.Entry<String, Queue<Map<String, Object>>> entry : testRecordsQueueMap.entrySet()) {
            String outputStream = entry.getKey();
            Queue<Map<String, Object>> queue = entry.getValue();

            Map<String, Object> record = queue.poll();
            if (record != null) {
                StreamlineEventImpl streamlineEvent = new StreamlineEventImpl(record, testRunSource.getId());
                LOG.info("Emitting event {} to stream {}", streamlineEvent, outputStream);
                _collector.emit(outputStream, new Values(streamlineEvent), streamlineEvent.getId());

                emitCount++;
            }
        }

        if (emitCount == 0) {
            LOG.info("No more records, sleeping...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // no-op
            }
            return;
        }
    }

    @Override
    public void ack(Object msgId) {
        LOG.info("Receive ack for msgid " + msgId);
    }

    @Override
    public void fail(Object msgId) {
        LOG.info("Receive fail for msgid " + msgId);
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
