package com.hortonworks.iotas.bolt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.processor.CustomProcessor;
import com.hortonworks.iotas.common.errors.ProcessingException;
import com.hortonworks.iotas.util.CoreUtils;
import com.hortonworks.iotas.util.ReflectionHelper;
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
import java.util.Map;

/**
 * Bolt for supporting custom processors compoents in an IoTaS topology
 */
public class CustomProcessorBolt extends BaseRichBolt {
    private static final Logger LOG = LoggerFactory.getLogger(CustomProcessorBolt.class);
    private OutputCollector collector;
    private CustomProcessor customProcessor;
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
                outputSchema.put(entry.getKey(), CoreUtils.getSchemaFromConfig(entry.getValue()));
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
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        if (StringUtils.isEmpty(customProcessorImpl)) {
            String message = "Custom processor implementation class not specified.";
            LOG.error(message);
            throw new RuntimeException(message);
        }
        String errorMessage = "Error instantiating custom processor implementation " + customProcessorImpl;
        try {
            //IOT-141: Deals with being able to add or register custom processors with IoTaS. Below line assumes that the implementation class is in class path
            // With IOT-141 the jar containing custom processor implementation will be downloaded and cached on worker node
            customProcessor = (CustomProcessor) ReflectionHelper.newInstance(customProcessorImpl);
        } catch (ClassNotFoundException|IllegalAccessException|InstantiationException e) {
            LOG.error(errorMessage);
            throw new RuntimeException(errorMessage, e);
        }
        customProcessor.initialize(config);
    }

    @Override
    public void execute(Tuple input) {
        try {
            final Object iotasEvent = input.getValueByField(IotasEvent.IOTAS_EVENT);
            if (iotasEvent instanceof IotasEvent) {
                for (Result result: customProcessor.process((IotasEvent) iotasEvent)) {
                    for (IotasEvent event: result.events) {
                        collector.emit(result.stream, input, new Values(event));
                    }
                }
            } else {
                LOG.debug("Invalid tuple received. Tuple disregarded and not sent to custom processor for processing.\n\tTuple [{}]." +
                        "\n\tIotasEvent [{}].", input, iotasEvent);
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
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        if (outputSchema == null || outputSchema.keySet().isEmpty()) {
            String message = "Custom processor config must have at least one output stream and associated schema.";
            LOG.error(message);
            throw new RuntimeException(message);
        }
        for (String outputStream: outputSchema.keySet()) {
            declarer.declareStream(outputStream, new Fields(IotasEvent.IOTAS_EVENT));
        }
    }

    @Override
    public void cleanup () {
        customProcessor.cleanup();
    }

}
