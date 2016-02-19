package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.NormalizationProcessor;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.layout.design.normalization.BulkNormalizationConfig;
import com.hortonworks.iotas.layout.design.normalization.FieldBasedNormalizationConfig;
import com.hortonworks.iotas.layout.design.normalization.FieldValueGenerator;
import com.hortonworks.iotas.layout.design.normalization.NormalizationConfig;
import com.hortonworks.iotas.layout.design.normalization.Transformer;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class NormalizationProcessorSchemaEvolverTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    public NormalizationProcessorSchemaEvolverTest() {
    }

    @Test
    public void testEvolveBulkNormalizationProcessorWithMatchedInputStream() throws IOException, BadComponentConfigException {
        final NormalizationProcessorSchemaEvolver evolver = new NormalizationProcessorSchemaEvolver();
        Stream inputStream = new Stream("stream1", Schema.of(Schema.Field.of("hello", Schema.Type.INTEGER)));

        String componentConfigJson = buildBulkModeNormalizationProcessorComponentConfig();

        Set<Stream> streams = evolver.apply(componentConfigJson, inputStream);

        // given output stream is same as EvolvingSchemaTestObject.inputStream()
        Set<Stream> expectStreams = Collections.singleton(new Stream(IotasEventImpl.DEFAULT_SOURCE_STREAM, inputStream.getSchema()));

        // we can check stream id of output stream is default
        assertEquals(expectStreams, streams);
    }

    @Test
    public void testEvolveFineGrainedNormalizationProcessorWithMatchedInputStream() throws IOException, BadComponentConfigException {
        final NormalizationProcessorSchemaEvolver evolver = new NormalizationProcessorSchemaEvolver();
        Stream inputStream = new Stream("stream1", Schema.of(Schema.Field.of("hello", Schema.Type.INTEGER)));

        String componentConfigJson = buildFineGrainedModeNormalizationProcessorComponentConfig();

        Set<Stream> streams = evolver.apply(componentConfigJson, inputStream);

        // given output stream is same as EvolvingSchemaTestObject.inputStream()
        Set<Stream> expectStreams = Collections.singleton(new Stream(IotasEventImpl.DEFAULT_SOURCE_STREAM, inputStream.getSchema()));

        // we can check stream id of output stream is default
        assertEquals(expectStreams, streams);
    }

    @Test
    public void testEvolveNormalizationProcessorWithMismatchedInputStream() throws IOException, BadComponentConfigException {
        final NormalizationProcessorSchemaEvolver evolver = new NormalizationProcessorSchemaEvolver();
        Stream inputStream = new Stream("oddstream", Schema.of(Schema.Field.of("hello", Schema.Type.INTEGER)));

        String componentConfigJson = buildFineGrainedModeNormalizationProcessorComponentConfig();

        Set<Stream> streams = evolver.apply(componentConfigJson, inputStream);

        // schema is preserved when input stream is not matched, but stream id of output stream is replaced with 'default'
        Stream outputStream = new Stream(IotasEventImpl.DEFAULT_SOURCE_STREAM, EvolvingSchemaTestObject.inputStream().getSchema());
        Set<Stream> expectStreams = Collections.singleton(outputStream);

        // we can check stream id of output stream is default
        assertEquals(expectStreams, streams);
    }

    private String buildBulkModeNormalizationProcessorComponentConfig() throws IOException {
        Stream inputStream = EvolvingSchemaTestObject.inputStream();
        BulkNormalizationConfig bulkNormalizationConfig = new BulkNormalizationConfig(inputStream.getSchema(), "hello");

        Map<String, NormalizationConfig> normalizationProcessorConfig = Maps.newHashMap();
        normalizationProcessorConfig.put("stream1", bulkNormalizationConfig);
        normalizationProcessorConfig.put("stream2", bulkNormalizationConfig);

        NormalizationProcessor processor = new NormalizationProcessor(normalizationProcessorConfig,
                new Stream("outputstream", inputStream.getSchema()), NormalizationProcessor.Type.bulk);

        String normalizationProcessorJson = objectMapper.writeValueAsString(processor);
        return buildComponentConfig(normalizationProcessorJson);
    }

    private String buildFineGrainedModeNormalizationProcessorComponentConfig() throws IOException {
        Stream inputStream = EvolvingSchemaTestObject.inputStream();

        // Transformer
        Transformer transformer = new Transformer(Schema.Field.of("a", Schema.Type.STRING), Schema.Field.of("b", Schema.Type.INTEGER));
        // filter
        // FieldValueGenerator
        FieldValueGenerator fieldValueGenerator = new FieldValueGenerator(Schema.Field.of("c", Schema.Type.STRING), "hello!");

        FieldBasedNormalizationConfig fieldBasedNormalizationConfig = new FieldBasedNormalizationConfig(inputStream.getSchema(),
                Collections.singletonList(transformer), Collections.singletonList("b"),
                Collections.singletonList(fieldValueGenerator));

        Map<String, NormalizationConfig> normalizationProcessorConfig = Maps.newHashMap();
        normalizationProcessorConfig.put("stream1", fieldBasedNormalizationConfig);
        normalizationProcessorConfig.put("stream2", fieldBasedNormalizationConfig);

        NormalizationProcessor processor = new NormalizationProcessor(normalizationProcessorConfig,
                new Stream("outputstream", inputStream.getSchema()), NormalizationProcessor.Type.fineGrained);

        String normalizationProcessorJson = objectMapper.writeValueAsString(processor);
        return buildComponentConfig(normalizationProcessorJson);
    }

    private String buildComponentConfig(String normalizationProcessorJson) throws IOException {
        return objectMapper.writeValueAsString(buildComponentConfigMap(normalizationProcessorJson));
    }

    private Map<String, Object> buildComponentConfigMap(String normalizationProcessorJson) throws IOException {
        Map<String, Object> componentConfig = Maps.newHashMap();
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_UINAME, "NORMALIZATION1");
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_TYPE, "NORMALIZATION");
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_TRANSFORMATION_CLASS, "dummy");

        Map<String, Object> configMap = Maps.newHashMap();
        configMap.put(TopologyLayoutConstants.JSON_KEY_PARALLELISM, 1);
        configMap.put(TopologyLayoutConstants.JSON_KEY_NORMALIZATION_PROCESSOR_CONFIG,
                objectMapper.readValue(normalizationProcessorJson, Map.class));

        componentConfig.put("config", configMap);
        return componentConfig;
    }
}
