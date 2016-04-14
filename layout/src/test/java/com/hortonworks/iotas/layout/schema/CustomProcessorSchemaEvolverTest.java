package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class CustomProcessorSchemaEvolverTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    public CustomProcessorSchemaEvolverTest() {
    }

    @Test
    public void testEvolveCustomProcessor() throws IOException, BadComponentConfigException {
        final CustomProcessorSchemaEvolver evolver = new CustomProcessorSchemaEvolver();

        Stream inputStream = EvolvingSchemaTestObject.inputStream();
        Map<String, Schema> outputStreamMap = Maps.newHashMap();
        outputStreamMap.put("stream1", inputStream.getSchema());
        outputStreamMap.put("stream2", inputStream.getSchema());

        String outputStreamToSchemaJson = objectMapper.writeValueAsString(outputStreamMap);
        String configJson = buildComponentConfig(outputStreamToSchemaJson);

        Set<Stream> streams = evolver.apply(configJson, inputStream);

        Set<Stream> expectStreams = Sets.newHashSet();
        expectStreams.add(new Stream("stream1", inputStream.getSchema()));
        expectStreams.add(new Stream("stream2", inputStream.getSchema()));

        assertEquals(expectStreams, streams);
    }

    private String buildComponentConfig(String outputStreamToSchemaJson) throws IOException {
        return objectMapper.writeValueAsString(buildComponentConfigMap(outputStreamToSchemaJson));
    }

    private Map<String, Object> buildComponentConfigMap(String outputStreamToSchemaJson) throws IOException {
        Map<String, Object> componentConfig = Maps.newHashMap();
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_UINAME, "consoleCustomProcessor");
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_TYPE, "CUSTOM");
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_TRANSFORMATION_CLASS, "dummy");

        Map<String, Object> configMap = Maps.newHashMap();
        configMap.put(TopologyLayoutConstants.JSON_KEY_PARALLELISM, 1);
        configMap.put(TopologyLayoutConstants.JSON_KEY_OUTPUT_STREAMS_SCHEMA, objectMapper.readValue(outputStreamToSchemaJson, Map.class));

        componentConfig.put("config", configMap);
        return componentConfig;
    }
}
