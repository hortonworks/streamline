package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.exception.ParserException;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of EvolvingSchema which supports 'Custom Processor' component.
 */
public class CustomProcessorSchemaEvolver implements EvolvingSchema {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Set<Stream> apply(String config, Stream inputStream) throws BadComponentConfigException {
        try {
            Map<String, Object> componentConfig = objectMapper.readValue(config, Map.class);
            Map<String, Object> customProcessorConfig = (Map<String, Object>) componentConfig.get(
                    TopologyLayoutConstants.JSON_KEY_CONFIG);
            Map<String, Object> outputStreamToSchemaConfig = (Map<String, Object>) customProcessorConfig.get(
                    TopologyLayoutConstants.JSON_KEY_OUTPUT_STREAMS_SCHEMA);

            return convertOutputStreamToSchemaConfigToStreams(outputStreamToSchemaConfig);
        } catch (Exception e) {
            throw new BadComponentConfigException("Exception while simulating evolution of custom processor schema", e);
        }
    }

    private Set<Stream> convertOutputStreamToSchemaConfigToStreams(Map<String, Object> outputStreamToSchemaConfig) throws ParserException, IOException {
        Set<Stream> streams = Sets.newHashSet();
        for (Map.Entry<String, Object> streamToSchema : outputStreamToSchemaConfig.entrySet()) {
            String streamId = streamToSchema.getKey();
            Schema schema = objectMapper.readValue(objectMapper.writeValueAsString(streamToSchema.getValue()), Schema.class);
            streams.add(new Stream(streamId, schema));
        }
        return streams;
    }
}
