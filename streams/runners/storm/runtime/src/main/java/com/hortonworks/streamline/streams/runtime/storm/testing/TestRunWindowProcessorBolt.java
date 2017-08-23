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
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.windowing.TupleWindow;

import java.util.Map;

public class TestRunWindowProcessorBolt extends BaseWindowedBolt {
    private final BaseWindowedBolt processorBolt;
    private final String eventLogFilePath;

    public TestRunWindowProcessorBolt(BaseWindowedBolt processorBolt, String eventLogFilePath) {
        this.processorBolt = processorBolt;
        this.eventLogFilePath = eventLogFilePath;
    }

    @Override
    public void execute(TupleWindow tupleWindow) {
        processorBolt.execute(tupleWindow);
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        EventLoggingOutputCollector outputCollector = new EventLoggingOutputCollector(context, collector,
                TestRunEventLogger.getEventLogger(eventLogFilePath));
        processorBolt.prepare(stormConf, context, outputCollector);
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
