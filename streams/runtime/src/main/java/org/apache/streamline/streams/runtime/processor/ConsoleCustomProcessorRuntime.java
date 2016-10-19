package org.apache.streamline.streams.runtime.processor;

import org.apache.streamline.streams.IotasEvent;
import org.apache.streamline.streams.Result;
import org.apache.streamline.streams.exception.ConfigException;
import org.apache.streamline.streams.exception.ProcessingException;
import org.apache.streamline.streams.runtime.CustomProcessorRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Console Custom Processor is a sample custom processor to test the storm topology with custom processor bolt
 */
public class ConsoleCustomProcessorRuntime implements CustomProcessorRuntime {
    protected static final Logger LOG = LoggerFactory.getLogger(ConsoleCustomProcessorRuntime.class);
    public static final String CONFIG_FIELD_NAME = "configField";
    Map<String, Object> config;
    @Override
    public void initialize(Map<String, Object> config) {
        this.config = config;
        LOG.info("Initializing with config field " + CONFIG_FIELD_NAME + " = " + this.config.get(CONFIG_FIELD_NAME).toString());
    }

    @Override
    public void validateConfig(Map<String, Object> config) throws ConfigException {
        LOG.debug("Validating config ");
        if (!config.containsKey(CONFIG_FIELD_NAME)) {
            throw new ConfigException("Missing config field: " + CONFIG_FIELD_NAME);
        }
        LOG.debug("Config valid ");
    }

    @Override
    public List<Result> process(IotasEvent iotasEvent) throws ProcessingException {
        LOG.debug("Processing {}", iotasEvent);
        List<Result> results = new ArrayList<>();
        results.add(new Result("stream1",Arrays.asList(iotasEvent)));
        return results;
    }

    @Override
    public void cleanup() {
        LOG.debug("Cleaning up");
    }
}
