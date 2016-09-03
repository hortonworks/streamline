package com.hortonworks.iotas.processor;

import com.hortonworks.iotas.callback.SmartThingsThermostatCallback;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.common.errors.ConfigException;
import com.hortonworks.iotas.common.errors.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by Salil Kanetkar on 8/16/16.
 */
public class SmartThingsThermostatCallbackCustomProcessor implements CustomProcessor {
    protected static final Logger LOG = LoggerFactory.getLogger(SmartThingsThermostatCallbackCustomProcessor.class);
    public static final String CONFIG_FIELD_NAMES[] = {"smartThingsURL","smartThingsAPIToken"};
    Map<String, Object> config;

    @Override
    public void initialize(Map<String, Object> config) {
        this.config = config;
        for (String key: this.config.keySet()) {
            LOG.info("Initializing with config field " + key + " = " + this.config.get(key).toString());
        }
    }

    @Override
    public void validateConfig(Map<String, Object> config) throws ConfigException {
        LOG.debug("Validating config ");
        for(int i = 0; i < CONFIG_FIELD_NAMES.length; i++) {
            if (!config.containsKey(CONFIG_FIELD_NAMES[i])) {
                throw new ConfigException("Missing config field: " + CONFIG_FIELD_NAMES[i]);
            }
        }
        LOG.debug("Config valid ");
    }

    @Override
    public List<Result> process(IotasEvent iotasEvent) throws ProcessingException{
        LOG.info("SALIL KANETKAR - SmartThingsThermostatCallbackCustomProcessor!!!!!!!!!!");
        LOG.debug("Processing {}", iotasEvent);
        SmartThingsThermostatCallback smartThingsThermostatCallback = new SmartThingsThermostatCallback(config.get(CONFIG_FIELD_NAMES[0]).toString(), config.get(CONFIG_FIELD_NAMES[1]).toString());
        try {
            smartThingsThermostatCallback.process();
        }
        catch (IOException e) {
        }
        List<Result> results = new ArrayList<>();
        results.add(new Result("stream1", Arrays.asList(iotasEvent)));
        return results;
    }

    @Override
    public void cleanup() {
        LOG.debug("Cleaning up");
    }
}
