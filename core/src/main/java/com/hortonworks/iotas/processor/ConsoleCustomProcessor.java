package com.hortonworks.iotas.processor;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Console Custom Processor
 */
public class ConsoleCustomProcessor implements CustomProcessor {
    protected static final Logger LOG = LoggerFactory.getLogger(ConsoleCustomProcessor.class);
    public static final String CONFIG_FIELD_NAME = "configField";
    Map<String, Object> config;
    @Override
    public void initialize () {
        LOG.info("Initializing console custom processor with config field " + CONFIG_FIELD_NAME + " = " + config.get(CONFIG_FIELD_NAME).toString());
    }

    @Override
    public void config(Map<String, Object> config) {
        LOG.info("Config received for console custom processor is " + config);
        this.config = config;
    }

    @Override
    public void validateConfig () throws ConfigException {
        LOG.info("Validating config for console custom processor ");
        if (!config.containsKey(CONFIG_FIELD_NAME)) {
            throw new ConfigException("Missing config field: " + CONFIG_FIELD_NAME);
        }
        LOG.info("Config provided for console custom processor is valid");
    }

    @Override
    public List<Result> process(IotasEvent iotasEvent) throws ProcessingException {
        LOG.info("Processing inside console custom processor");
        List<Result> results = new ArrayList<>();
        String ambientTempValue = iotasEvent.getFieldsAndValues().get("ambient_temperature_f").toString();
        System.out.println("Printing value for ambient temperature from custom processor -> " + ambientTempValue);
        results.add(new Result("stream1",Arrays.asList(iotasEvent)));
        return results;
    }

    @Override
    public void cleanup() {
        LOG.info("Cleaning up console custom processor");
    }
}
