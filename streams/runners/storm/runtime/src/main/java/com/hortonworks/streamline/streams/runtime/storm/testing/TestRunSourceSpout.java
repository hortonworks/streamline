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

import com.google.common.util.concurrent.Uninterruptibles;
import com.hortonworks.streamline.common.SchemaValueConverter;
import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.layout.component.Stream;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

public class TestRunSourceSpout extends BaseRichSpout {
    private static final Logger LOG = LoggerFactory.getLogger(TestRunSourceSpout.class);
    private EventLoggingSpoutOutputCollector collector;

    private final TestRunSource testRunSource;
    private final Map<String, TestRecordsInformation> testRecordsInformationPerOutputStream;

    public TestRunSourceSpout(String testRunSourceJson) {
        this(Utils.createObjectFromJson(testRunSourceJson, TestRunSource.class));
    }

    public TestRunSourceSpout(TestRunSource testRunSource) {
        this.testRunSource = testRunSource;

        testRecordsInformationPerOutputStream = new HashMap<>();

        if (testRunSource != null) {
            Map<String, List<Map<String, Object>>> testRecords = testRunSource.getTestRecordsForEachStream();
            for (Map.Entry<String, List<Map<String, Object>>> entry : testRecords.entrySet()) {
                List<Map<String, Object>> values = entry.getValue();
                List<Map<String, Object>> schemaConformedValues = values.stream()
                        .map(v -> convertValueToConformStream(entry.getKey(), v))
                        .collect(toList());

                TestRecordsInformation testRecordsInformation = new TestRecordsInformation(
                        testRunSource.getOccurrence(), testRunSource.getSleepMsPerIteration(), schemaConformedValues);

                testRecordsInformationPerOutputStream.put(entry.getKey(), testRecordsInformation);
            }
        }
    }

    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        if (this.testRunSource == null) {
            throw new RuntimeException("testRunSource cannot be null");
        }

        this.collector = new EventLoggingSpoutOutputCollector(context, collector,
                TestRunEventLogger.getEventLogger(testRunSource.getEventLogFilePath()));
    }

    @Override
    public void nextTuple() {
        int emitCount = 0;
        boolean allOutputStreamsCompleted = true;

        // loop output stream and emit at most one record per output stream
        for (Map.Entry<String, TestRecordsInformation> entry : testRecordsInformationPerOutputStream.entrySet()) {
            String outputStream = entry.getKey();
            TestRecordsInformation info = entry.getValue();

            if (info.isCompleted()) {
                continue;
            }

            allOutputStreamsCompleted = false;

            Optional<Map<String, Object>> recordOptional = info.nextRecord();
            if (recordOptional.isPresent()) {
                Map<String, Object> record = recordOptional.get();
                StreamlineEventImpl streamlineEvent = new StreamlineEventImpl(record, testRunSource.getId());
                LOG.debug("Emitting event {} to stream {}", streamlineEvent, outputStream);
                collector.emit(outputStream, new Values(streamlineEvent), streamlineEvent.getId());
                emitCount++;
            }
        }

        if (allOutputStreamsCompleted) {
            LOG.info("All iterations are completed, sleeping...");
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
        } else if (emitCount == 0) {
            LOG.info("All output streams are finished last iteration and now in sleep phase, sleeping...");
            Uninterruptibles.sleepUninterruptibly(testRunSource.getSleepMsPerIteration(), TimeUnit.MILLISECONDS);
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

    private Map<String, Object> convertValueToConformStream(String streamId, Map<String, Object> value) {
        Stream stream = testRunSource.getOutputStream(streamId);
        if (stream == null) {
            throw new IllegalArgumentException("Stream " + streamId + " doesn't exist.");
        }

        return SchemaValueConverter.convertMap(stream.getSchema(), value);
    }

}
