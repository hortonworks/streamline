package org.apache.streamline.examples.processors;

import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.Result;
import org.apache.streamline.streams.exception.ConfigException;
import org.apache.streamline.streams.exception.ProcessingException;
import org.apache.streamline.streams.runtime.CustomProcessorRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Console Processor is a sample custom processor to test the storm topology with custom processor bolt
 */
public class ConsoleCustomProcessor implements CustomProcessorRuntime {
    protected static final Logger LOG = LoggerFactory.getLogger(ConsoleCustomProcessor.class);
    public static final String CONFIG_FIELD_NAME = "configField";
    Map<String, Object> config = new HashMap<>();
 
    public void initialize(Map<String, Object> config) {
        if (config != null) {
            this.config = config;
        }
        LOG.info("Initializing with config field " + CONFIG_FIELD_NAME + " = " + this.config.get(CONFIG_FIELD_NAME));
    }

    
    public void validateConfig(Map<String, Object> config) throws ConfigException {
        LOG.debug("Validating config ");
        if (!config.containsKey(CONFIG_FIELD_NAME)) {
            throw new ConfigException("Missing config field: " + CONFIG_FIELD_NAME);
        }
        LOG.debug("Config valid ");
    }


    public List<Result> process(StreamlineEvent event) throws ProcessingException {
        LOG.info("Processing {}", event);
        List<Result> results = new ArrayList<>();
        results.add(new Result("stream1",Arrays.asList(event)));
        return results;
    }

   
    public void cleanup() {
        LOG.debug("Cleaning up");
    }
}
