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
package com.hortonworks.streamline.streams.runtime.storm.bolt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.exception.ProcessingException;
import com.hortonworks.streamline.streams.runtime.CustomProcessorRuntime;
import org.apache.commons.lang.StringUtils;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Bolt for supporting custom processors components in an Streamline topology
 */
public class CustomProcessorBolt extends AbstractProcessorBolt {
    private static final Logger LOG = LoggerFactory.getLogger(CustomProcessorBolt.class);
    private static final ConcurrentHashMap<String, Class<CustomProcessorRuntime>> customProcessorConcurrentHashMap = new ConcurrentHashMap<>();
    private CustomProcessorRuntime customProcessorRuntime;
    private String customProcessorImpl;
    private Map<String, Object> config;
    // this map is used for mapping field names from input schema registered by CP to field names in the input schema from component connected to CP
    // Use case is, if kafka source has output schema with field name driver and CP operates on driverId then we provide a way to map those so CP can be used
    // in different contexts in SAM application. Assumption is that the type of both the fields will be same. Also the map is per input stream. For now UI
    // will add a constraint that only one component(or input stream for rule processor, etc) can be connected to CP. In future we can change to allow
    // multiple input streams in UI using the same code
    private Map<String, Map<String, String>> inputSchemaMap;
    private Map<String, Schema> outputSchema = new HashMap<>();
    private static final Function<String, Class<CustomProcessorRuntime>> customProcessorRuntimeFunction = new CustomProcessorBolt
            .GetCustomProcessorRuntimeFunction();

    public CustomProcessorBolt customProcessorImpl (String customProcessorImpl) {
        String message;
        if (customProcessorImpl == null || StringUtils.isEmpty(customProcessorImpl)) {
            message = "Custom processor implementation class not specified.";
            LOG.error(message);
            throw new RuntimeException(message);
        }
        this.customProcessorImpl = customProcessorImpl;
        return this;
    }

    /**
     * Associate output schema that is a json string
     * @param outputSchemaJson
     * @return
     */
    public CustomProcessorBolt outputSchema (String outputSchemaJson) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Schema> outputSchema = new HashMap<>();
        try {
            Map<String, Map> output = mapper.readValue(outputSchemaJson, Map.class);
            for (Map.Entry<String, Map> entry: output.entrySet()) {
                outputSchema.put(entry.getKey(), Utils.getSchemaFromConfig(entry.getValue()));
            }
        } catch (IOException e) {
            LOG.error("Error during deserialization of output schema JSON string: {}", outputSchemaJson, e);
            throw new RuntimeException(e);
        }
        return outputSchema(outputSchema);
    }

    /**
     * Associate output schema
     * @param outputSchema
     * @return
     */
    public CustomProcessorBolt outputSchema (Map<String, Schema> outputSchema) {
        // remove check for != 1 below when supporting multiple output streams later and change related code in this class
        if (outputSchema == null || outputSchema.isEmpty() || outputSchema.size() != 1) {
            String msg = "Output schema for custom processor is not correct. Only one output stream and corresponding output schema is allowed";
            LOG.error(msg);
            throw new RuntimeException(msg);
        }
        this.outputSchema = outputSchema;
        return this;
    }

    /**
     * Associcate input schema mapping. e.g. driverId in input schema maps to driver. Hence The mapping will look like {'inputStream': {'driverId': 'driver'}}
     * @param inputSchemaMap
     * @return
     */
    public CustomProcessorBolt inputSchemaMap (Map<String, Map<String, String>> inputSchemaMap) {
        this.inputSchemaMap = inputSchemaMap;
        return this;
    }

    /**
     * Associate input schema map that is a json string
     * @param inputSchemaMapJson
     * @return
     */
    public CustomProcessorBolt inputSchemaMap (String inputSchemaMapJson) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Map<String, String>> inputSchemaMap;
        try {
            inputSchemaMap = mapper.readValue(inputSchemaMapJson, Map.class);
        } catch (IOException e) {
            LOG.error("Error during deserialization of input schema mapping JSON string: {}", inputSchemaMapJson, e);
            throw new RuntimeException(e);
        }
        return inputSchemaMap(inputSchemaMap);
    }

    /**
     * Associate config as a json string
     * @param configJson
     * @return
     */
    public CustomProcessorBolt config (String configJson) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> config;
        try {
            config = mapper.readValue(configJson, Map.class);
        } catch (IOException e) {
            LOG.error("Error during deserialization of config JSON string: {}", configJson, e);
            throw new RuntimeException(e);
        }
        return config(config);
    }

    /**
     * Associate config as a Map of String to Object
     * @param config
     * @return
     */
    public CustomProcessorBolt config (Map<String, Object> config) {
        this.config = config;
        return this;
    }

    @Override
    public void prepare (Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        customProcessorRuntime = getCustomProcessorRuntime();
        customProcessorRuntime.initialize(config);
    }

    @Override
    protected void process (Tuple input, StreamlineEvent event) {
        try {
            StreamlineEvent toProcess = null;
            if (inputSchemaMap == null || inputSchemaMap.isEmpty() || !inputSchemaMap.containsKey(input.getSourceStreamId())) {
                toProcess = event;
            } else {
                // Create a new mapped event based on mapping to pass it to CP implementation
                Map<String, Object> mappedEventMap = new HashMap<>();
                for (Map.Entry<String, String> entry: inputSchemaMap.get(input.getSourceStreamId()).entrySet()) {
                    String lookupKey = entry.getValue();
                    String insertKey = entry.getKey();
                    findValue(lookupKey, event).ifPresent(value -> mappedEventMap.put(insertKey, value));
                }
                toProcess = StreamlineEventImpl.builder()
                        .from(event)
                        .sourceStream(input.getSourceStreamId())
                        .fieldsAndValues(mappedEventMap)
                        .build();
            }

            List<StreamlineEvent> results = customProcessorRuntime.process(toProcess);
            if (results != null) {
                Schema schema = outputSchema.values().iterator().next();
                String outputStream = outputSchema.keySet().iterator().next();
                for (StreamlineEvent e : results) {
                    Map<String, Object> newFieldsAndValues = new HashMap<>();
                    // below output schema is at SAM application level. Fields in the schema are a union of subsets of original input schema of incoming
                    // event and CP defined output schema. UI will make sure that the fields are from one of the two sets.
                    for (Schema.Field field: schema.getFields()) {
                        // value has to be present either in the result or the input event
                        String key = field.getName();
                        findValue(key, e, event).ifPresent(value -> newFieldsAndValues.put(key, value));
                    }
                    StreamlineEvent toEmit = StreamlineEventImpl.builder()
                            .from(e)
                            .sourceStream(outputStream)
                            .fieldsAndValues(newFieldsAndValues)
                            .build();
                    collector.emit(outputStream, input, new Values(toEmit));
                }
            }
        } catch (ProcessingException e) {
            LOG.error("Custom Processor threw a ProcessingException. ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void declareOutputFields (OutputFieldsDeclarer declarer) {
        declarer.declareStream(outputSchema.keySet().iterator().next(), new Fields(StreamlineEvent.STREAMLINE_EVENT));
    }

    @Override
    public void cleanup () {
        customProcessorRuntime.cleanup();
    }

    // finds value for key in any of the events
    private Optional<?> findValue(String key, StreamlineEvent... events) {
        for (StreamlineEvent event : events) {
            Object val = event.get(key);
            if (val != null) {
                return Optional.of(val);
            }
        }
        return Optional.empty();
    }

    private CustomProcessorRuntime getCustomProcessorRuntime() {
        try {
            return customProcessorConcurrentHashMap.computeIfAbsent(customProcessorImpl, customProcessorRuntimeFunction).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to instantiate custom processor: " + customProcessorImpl, e);
        }
    }

    private static class GetCustomProcessorRuntimeFunction implements Function<String, Class<CustomProcessorRuntime>>, Serializable {
        @Override
        public Class<CustomProcessorRuntime> apply(String customProcessorImpl) {
            try {
                return (Class<CustomProcessorRuntime>) Class.forName(customProcessorImpl);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load custom processor class: " + customProcessorImpl, e);
            }
        }
    }
}
