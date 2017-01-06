/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.streamline.streams.runtime.storm.bolt;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.common.utils.ShellContext;
import com.hortonworks.streamline.streams.runtime.processor.MultiLangProcessorRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class StreamsShellBolt implements IRichBolt {

    public static final Logger LOG = LoggerFactory.getLogger(StreamsShellBolt.class);

    private OutputCollector collector;
    private TopologyContext context;
    private List<String> outputStreams;
    private String command;
    private Random rand;
    private int processTimeoutInMs;
    Map<String, String> envMap = new HashMap<>();

    private MultiLangProcessorRuntime processorRuntime ;

    public StreamsShellBolt(String command, int processTimeoutInMs) {
        if(command == null || command.isEmpty())
            throw new IllegalArgumentException("process command can not be empty");
        this.command = command;
        this.processTimeoutInMs = processTimeoutInMs;
    }

    public StreamsShellBolt withOutputStreams(List<String> outputStreams) {
        this.outputStreams = outputStreams;
        return this;
    }

    public StreamsShellBolt withEnvMap(Map<String, String> envMap) {
        this.envMap = envMap;
        return this;
    }

    public void prepare(Map stormConf, TopologyContext context,
                        final OutputCollector collector) {

        this.collector = collector;
        this.context = context;
        rand = new Random();

        ShellContext shellContext = getShellContext(context);

        Map<String, Object> processConfigMap = new HashMap<>();
        processConfigMap.put(MultiLangProcessorRuntime.COMMAND, command.split(" "));
        processConfigMap.put(MultiLangProcessorRuntime.PROCESS_CONFIG, stormConf);
        processConfigMap.put(MultiLangProcessorRuntime.SHELL_CONTEXT, shellContext);
        processConfigMap.put(MultiLangProcessorRuntime.OUTPUT_STREAMS, outputStreams);
        processConfigMap.put(MultiLangProcessorRuntime.PROCESS_TIMEOUT_MILLS, processTimeoutInMs);
        processConfigMap.put(MultiLangProcessorRuntime.SHELL_ENVIRONMENT, envMap);

        processorRuntime = new MultiLangProcessorRuntime();
        processorRuntime.initialize(processConfigMap);
    }

    private ShellContext getShellContext(TopologyContext context) {
        ShellContext shellContext = new ShellContext();
        shellContext.setCodeDir(context.getCodeDir());
        shellContext.setPidDir(context.getPIDDir());
        shellContext.setComponentId(context.getThisComponentId());
        return shellContext;
    }

    public void execute(Tuple input) {
        try {
            final Object tupleField = input.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
            if (tupleField instanceof StreamlineEvent) {
                //just need an id
                String genId = Long.toString(rand.nextLong());
                StreamlineEvent event = (StreamlineEvent) tupleField;
                StreamlineEvent eventWithStream = getStreamlineEventWithStream(event, input, genId);
                for (Result result : processorRuntime.process(eventWithStream)) {
                    for (StreamlineEvent e : result.events) {
                        collector.emit(result.stream, input, new Values(e));
                    }
                }
            } else {
                LOG.debug("Invalid tuple received. Tuple disregarded and rules not evaluated.\n\tTuple [{}]." +
                        "\n\tStreamlineEvent [{}].", input, tupleField);
            }
            collector.ack(input);
        } catch (Exception e) {
            collector.fail(input);
            collector.reportError(e);
            LOG.debug("", e);
        }
    }

    private StreamlineEvent getStreamlineEventWithStream(StreamlineEvent event, Tuple tuple, String genId) {
        return new StreamlineEventImpl(event,
                event.getDataSourceId(), genId,
                event.getHeader(), tuple.getSourceStreamId(), event.getAuxiliaryFieldsAndValues());
    }

    @Override
    public void cleanup() {
        processorRuntime.cleanup();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        for (String outputStream: outputStreams) {
            outputFieldsDeclarer.declareStream(outputStream, new Fields(StreamlineEvent.STREAMLINE_EVENT));
        }
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        return null;
    }
}
