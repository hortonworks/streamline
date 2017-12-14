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

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;

import java.util.Map;

public class TestRunProcessorBolt extends BaseRichBolt {
    private final BaseRichBolt processorBolt;
    private final String eventLogFilePath;

    public TestRunProcessorBolt(BaseRichBolt processorBolt, String eventLogFilePath) {
        this.processorBolt = processorBolt;
        this.eventLogFilePath = eventLogFilePath;
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        EventLoggingOutputCollector collector = new EventLoggingOutputCollector(topologyContext, outputCollector,
                TestRunEventLogger.getEventLogger(eventLogFilePath));
        processorBolt.prepare(map, topologyContext, collector);
    }

    @Override
    public void execute(Tuple tuple) {
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
