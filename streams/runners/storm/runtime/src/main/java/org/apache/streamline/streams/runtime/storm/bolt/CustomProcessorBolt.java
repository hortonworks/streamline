package org.apache.streamline.streams.runtime.storm.bolt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.common.Schema;
import org.apache.streamline.common.util.Utils;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.Result;
import org.apache.streamline.streams.common.StreamlineEventImpl;
import org.apache.streamline.streams.exception.ProcessingException;
import org.apache.streamline.streams.runtime.CustomProcessorRuntime;
import org.apache.commons.lang.StringUtils;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bolt for supporting custom processors components in an Streamline topology
 */
public class CustomProcessorBolt extends BaseRichBolt {
    private static final Logger LOG = LoggerFactory.getLogger(CustomProcessorBolt.class);
    private static final ConcurrentHashMap<String, CustomProcessorRuntime> customProcessorConcurrentHashMap = new ConcurrentHashMap<>();
    private OutputCollector collector;
    private CustomProcessorRuntime customProcessorRuntime;
    private String customProcessorImpl;
    private Map<String, Object> config;
    private Schema inputSchema;
    private Map<String, Schema> outputSchema = new HashMap<>();

    public CustomProcessorBolt customProcessorImpl (String customProcessorImpl) {
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
        this.outputSchema = outputSchema;
        return this;
    }

    /**
     * Associate input schema that is a json string
     * @param inputSchemaJson
     * @return
     */
    public CustomProcessorBolt inputSchema (String inputSchemaJson) {
        ObjectMapper mapper = new ObjectMapper();
        Schema inputSchema;
        try {
            inputSchema = mapper.readValue(inputSchemaJson, Schema.class);
        } catch (IOException e) {
            LOG.error("Error during deserialization of input schema JSON string: {}", inputSchemaJson, e);
            throw new RuntimeException(e);
        }
        return inputSchema(inputSchema);
    }

    /**
     * Associcate input schema
     * @param inputSchema
     * @return
     */
    public CustomProcessorBolt inputSchema (Schema inputSchema) {
        this.inputSchema = inputSchema;
        return this;
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
        this.collector = collector;
        String message;
        if (StringUtils.isEmpty(customProcessorImpl)) {
            message = "Custom processor implementation class not specified.";
            LOG.error(message);
            throw new RuntimeException(message);
        }
        customProcessorRuntime = getCustomProcessorRuntime();
        customProcessorRuntime.initialize(config);
    }

    @Override
    public void execute (Tuple input) {
        try {
            final Object tupleField = input.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
            if (tupleField instanceof StreamlineEvent) {
                StreamlineEvent event = (StreamlineEvent) tupleField;
                List<Result> results = customProcessorRuntime.process(new StreamlineEventImpl(event, event.getDataSourceId(), event
                        .getId(), event.getHeader(), input.getSourceStreamId()));
                if (results != null) {
                    for (Result result : results) {
                        for (StreamlineEvent e : result.events) {
                            collector.emit(result.stream, input, new Values(e));
                        }
                    }
                }
            } else {
                LOG.debug("Invalid tuple received. Tuple disregarded and not sent to custom processor for processing.\n\tTuple [{}]." +
                        "\n\tStreamlineEvent [{}].", input, tupleField);
            }
            collector.ack(input);
        } catch (ProcessingException e) {
            LOG.error("Custom Processor threw a ProcessingException. ", e);
            collector.fail(input);
            collector.reportError(e);
        } catch (Exception e) {
            collector.fail(input);
            collector.reportError(e);
        }
    }

    @Override
    public void declareOutputFields (OutputFieldsDeclarer declarer) {
        if (outputSchema == null || outputSchema.keySet().isEmpty()) {
            String message = "Custom processor config must have at least one output stream and associated schema.";
            LOG.error(message);
            throw new RuntimeException(message);
        }
        for (String outputStream: outputSchema.keySet()) {
            declarer.declareStream(outputStream, new Fields(StreamlineEvent.STREAMLINE_EVENT));
        }
    }

    @Override
    public void cleanup () {
        customProcessorRuntime.cleanup();
    }

    private CustomProcessorRuntime getCustomProcessorRuntime() {
        CustomProcessorRuntime customProcessorRuntime = customProcessorConcurrentHashMap.get(customProcessorImpl);
        if (customProcessorRuntime == null) {
            try {
                customProcessorRuntime = (CustomProcessorRuntime) Class.forName(customProcessorImpl).newInstance();
                customProcessorConcurrentHashMap.put(customProcessorImpl, customProcessorRuntime);
            } catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {
                throw new RuntimeException("Failed to load custom processor: " + customProcessorImpl, e);
            }
        }
        return customProcessorRuntime;
    }
}
