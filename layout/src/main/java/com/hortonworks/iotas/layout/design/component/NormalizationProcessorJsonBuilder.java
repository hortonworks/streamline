package com.hortonworks.iotas.layout.design.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NormalizationProcessorJsonBuilder implements ComponentBuilder<NormalizationProcessor> {
    private static final Logger LOG = LoggerFactory.getLogger(NormalizationProcessorJsonBuilder.class);
    private final String normalizationProcessorJson;

    public NormalizationProcessorJsonBuilder(String normalizationProcessorJson) {
        this.normalizationProcessorJson = normalizationProcessorJson;
    }

    @Override
    public NormalizationProcessor build() {
        ObjectMapper mapper = new ObjectMapper();
        NormalizationProcessor normalizationProcessor = null;
        try {
            normalizationProcessor = mapper.readValue(normalizationProcessorJson, NormalizationProcessor.class);
        } catch (IOException e) {
            LOG.error("Error during deserialization of JSON string: {}", normalizationProcessorJson, e);
        }
        return normalizationProcessor;
    }

}
