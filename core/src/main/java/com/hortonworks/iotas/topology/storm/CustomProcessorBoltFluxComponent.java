package com.hortonworks.iotas.topology.storm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.common.errors.ConfigException;
import com.hortonworks.iotas.processor.CustomProcessor;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.util.CoreUtils;
import com.hortonworks.iotas.util.ReflectionHelper;
import com.hortonworks.iotas.util.exception.BadTopologyLayoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation for CustomProcessorBolt
 */
public class CustomProcessorBoltFluxComponent extends AbstractFluxComponent {

    private static final Logger LOG = LoggerFactory.getLogger(CustomProcessorBoltFluxComponent.class);

    @Override
    protected void generateComponent () {
        String boltId = "customProcessorBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.iotas.bolt.CustomProcessorBolt";
        String[] configMethodNames = {"customProcessorImpl", "outputSchema", "inputSchema", "config"};
        Object[] values = new Object[configMethodNames.length];
        values[0] = (String) conf.get(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_IMPL);
        try {
            values[1] = getOutputSchemaJson();
            values[2] = getInputSchemaJson();
            values[3] = getObjectAsJson(getCustomConfig());
        } catch (JsonProcessingException e) {
            String message = "Error while parsing input/output/config for custom processor config while generating yaml.";
            LOG.error(message);
            throw new RuntimeException(message, e);
        }
        List configMethods = getConfigMethodsYaml(configMethodNames, values);
        component = createComponent(boltId, boltClassName, null, null, configMethods);
        addParallelismToComponent();
    }

    @Override
    public void validateConfig () throws BadTopologyLayoutException {
        super.validateConfig();
        validateStringFields();
        String fieldToValidate = TopologyLayoutConstants.JSON_KEY_INPUT_SCHEMA;
        try {
            Map inputSchema = (Map) conf.get(fieldToValidate);
            Schema input = CoreUtils.getSchemaFromConfig(inputSchema);
            fieldToValidate = TopologyLayoutConstants.JSON_KEY_OUTPUT_STREAMS_SCHEMA;
            Map<String, Map> outputSchema = (Map) conf.get(fieldToValidate);
            if (outputSchema == null || outputSchema.keySet().isEmpty()) {
                throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldToValidate));
            }
            for (Map.Entry<String, Map> entry: outputSchema.entrySet()) {
                CoreUtils.getSchemaFromConfig(entry.getValue());
            }
            CustomProcessor customProcessor = ReflectionHelper.newInstance((String) conf.get(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_IMPL));
            customProcessor.validateConfig(getCustomConfig());
        } catch (ClassCastException|IOException e) {
            throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldToValidate));
        } catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {
            throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_CP_IMPL_INSTANTIATION, (String) conf.get
                    (TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_IMPL)));
        } catch (ConfigException e) {
            throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_CP_CONFIG_EXCEPTION, (String) conf.get
                    (TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_IMPL)) + " Message from implementation is: " + e.getMessage(), e);
        }
    }

    private void validateStringFields () throws BadTopologyLayoutException {
        String[] requiredStringFields = {
            TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_IMPL
        };
        validateStringFields(requiredStringFields, true);
    }

    private String getInputSchemaJson () throws JsonProcessingException {
        return getObjectAsJson(conf.get(TopologyLayoutConstants.JSON_KEY_INPUT_SCHEMA));
    }

    private String getOutputSchemaJson () throws JsonProcessingException {
        return getObjectAsJson(conf.get(TopologyLayoutConstants.JSON_KEY_OUTPUT_STREAMS_SCHEMA));
    }

    private Map<String, Object> getCustomConfig () throws JsonProcessingException {
        Map<String, Object> config = new HashMap<>();
        Set<String> keysToDrop = new HashSet<>();
        keysToDrop.add(TopologyLayoutConstants.JSON_KEY_PARALLELISM);
        keysToDrop.add(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_IMPL);
        keysToDrop.add(TopologyLayoutConstants.JSON_KEY_INPUT_SCHEMA);
        keysToDrop.add(TopologyLayoutConstants.JSON_KEY_OUTPUT_STREAMS_SCHEMA);
        for (Map.Entry entry: conf.entrySet()) {
            if (!keysToDrop.contains(entry.getKey())) {
                config.put(entry.getKey().toString().replaceFirst(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_PREFIX_REGEX, ""), entry.getValue());
            }
        }
        return config;
    }

    private String getObjectAsJson (Object arg) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(arg);
    }
}
