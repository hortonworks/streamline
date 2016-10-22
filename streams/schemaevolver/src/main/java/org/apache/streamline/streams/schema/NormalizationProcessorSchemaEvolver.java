package org.apache.streamline.streams.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.streamline.streams.common.StreamlineEventImpl;
import org.apache.streamline.streams.layout.component.Stream;
import org.apache.streamline.streams.layout.component.impl.normalization.NormalizationConfig;
import org.apache.streamline.streams.layout.component.impl.normalization.NormalizationProcessor;
import org.apache.streamline.streams.layout.component.impl.normalization.NormalizationProcessorJsonBuilder;
import org.apache.streamline.streams.schema.exception.BadComponentConfigException;

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

            NormalizationProcessor normalizationProcessor = buildNormalizationProcessor(componentConfig);

            Set<Stream> streams = Sets.newHashSet();
            Map<String, NormalizationConfig> normalizationConfigMap = normalizationProcessor.getInputStreamsWithNormalizationConfig();
            if (normalizationConfigMap.containsKey(inputStream.getId())) {
                // matched input stream: it only has one output stream and stream id will be replaced to default
                Stream outputStream = normalizationProcessor.getOutputStreams().iterator().next();
                streams.add(new Stream(StreamlineEventImpl.DEFAULT_SOURCE_STREAM, outputStream.getSchema()));
            } else {
                // mismatched input stream: preserve input schema but stream id will be replaced to default
                streams.add(new Stream(StreamlineEventImpl.DEFAULT_SOURCE_STREAM, inputStream.getSchema()));
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
