package com.hortonworks.iotas.processor.examples;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.common.errors.ConfigException;
import com.hortonworks.iotas.common.errors.ProcessingException;
import com.hortonworks.iotas.processor.CustomProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Console Custom Processor is a sample custom processor to test the storm topology with custom processor bolt
 */
public class RegistryNotifierProcessor implements CustomProcessor {
    protected static final Logger LOG = LoggerFactory.getLogger(RegistryNotifierProcessor.class);
    private static final String API_URL = "apiURL";
    private static final String API_KEY = "apiKey";
    Map<String, Object> config;
    private String apiUrl;
    private String apiKey;

    @Override
    public void initialize(Map<String, Object> config) {
        try {
            this.config = config;
            this.apiUrl = (String) config.get(API_URL);
            this.apiKey = (String) config.get(API_KEY);
            LOG.info("Initializing with config field " + API_URL + " = " + apiUrl);
        } catch (Exception e) {
            LOG.info("failed to initialize {}" ,e);
        }
    }

    @Override
    public void validateConfig(Map<String, Object> config) throws ConfigException {
        LOG.debug("Validating Config ");
        if (!config.containsKey(API_URL)) {
            throw new ConfigException("Missing config field: " + API_URL);
        }
        if (!config.containsKey(API_KEY)) {
            throw new ConfigException("Missing config field: " + API_KEY);
        }

        LOG.debug("Config valid ");
    }

    @Override
    public List<Result> process(IotasEvent iotasEvent) throws ProcessingException {
        String payload = ((String) iotasEvent.getFieldsAndValues().get("payload")).replaceAll("\"","");

        LOG.info("Payload {} ", payload);
        try {
            String url = apiUrl+payload;
            LOG.info("URL {} ", url);
            HttpResponse<JsonNode> jsonResponse = Unirest.patch(url)
                .header("accept", "application/json")
                .header("API-KEY", apiKey)
                .header("Cache-Control", "no-cache")
                .asJson();
            LOG.info("Response {}", jsonResponse.getBody());
        } catch (Exception e ){
            LOG.error("Failed to send payload {} ", e);
        }
        List<Result> results = new ArrayList<>();
        results.add(new Result("stream1",Arrays.asList(iotasEvent)));
        return results;
    }

    @Override
    public void cleanup() {
        LOG.debug("Cleaning up");
    }
}
