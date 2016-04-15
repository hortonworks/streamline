package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.layout.design.component.NormalizationProcessor;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.layout.design.normalization.NormalizationConfig;
import com.hortonworks.iotas.layout.design.normalization.NormalizationProcessorJsonBuilder;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;

import java.util.Map;
import java.util.Set;

/**
 * Implementation of EvolvingSchema which supports 'Normalization Processor' component.
 */
public class NormalizationProcessorSchemaEvolver implements EvolvingSchema {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Set<Stream> apply(String config, Stream inputStream) throws BadComponentConfigException {
        try {
            Map<String, Object> componentConfig = objectMapper.readValue(config, Map.class);
            Map<String, Object> customProcessorConfig = (Map<String, Object>) componentConfig.get(
                    TopologyLayoutConstants.JSON_KEY_CONFIG);
            Map<String, Object> normalizationProcessorConfig = (Map<String, Object>) customProcessorConfig.get(
                    TopologyLayoutConstants.JSON_KEY_NORMALIZATION_PROCESSOR_CONFIG);

            NormalizationProcessor normalizationProcessor = buildNormalizationProcessor(normalizationProcessorConfig);

            Set<Stream> streams = Sets.newHashSet();
            Map<String, NormalizationConfig> normalizationConfigMap = normalizationProcessor.getInputStreamsWithNormalizationConfig();
            if (normalizationConfigMap.containsKey(inputStream.getId())) {
                // matched input stream: it only has one output stream and stream id will be replaced to default
                Stream outputStream = normalizationProcessor.getOutputStreams().iterator().next();
                streams.add(new Stream(IotasEventImpl.DEFAULT_SOURCE_STREAM, outputStream.getSchema()));
            } else {
                // mismatched input stream: preserve input schema but stream id will be replaced to default
                streams.add(new Stream(IotasEventImpl.DEFAULT_SOURCE_STREAM, inputStream.getSchema()));
            }
            return streams;
        } catch (Exception e) {
            throw new BadComponentConfigException("Exception while simulating evolution of custom processor schema", e);
        }
    }

    private NormalizationProcessor buildNormalizationProcessor(Map<String, Object> normalizationProcessorConfig) throws JsonProcessingException {
        String normalizationProcessorConfigJson = objectMapper.writeValueAsString(normalizationProcessorConfig);
        NormalizationProcessorJsonBuilder normalizationProcessorBuilder = new NormalizationProcessorJsonBuilder(normalizationProcessorConfigJson);
        return normalizationProcessorBuilder.build();
    }

}
