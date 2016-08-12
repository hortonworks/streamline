package com.hortonworks.iotas.streams.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.hortonworks.iotas.streams.common.IotasEventImpl;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.streams.layout.component.Stream;
import com.hortonworks.iotas.streams.layout.component.impl.normalization.BulkNormalizationConfig;
import com.hortonworks.iotas.streams.layout.component.impl.normalization.FieldBasedNormalizationConfig;
import com.hortonworks.iotas.streams.layout.component.impl.normalization.FieldValueGenerator;
import com.hortonworks.iotas.streams.layout.component.impl.normalization.NormalizationConfig;
import com.hortonworks.iotas.streams.layout.component.impl.normalization.NormalizationProcessor;
import com.hortonworks.iotas.streams.layout.component.impl.normalization.Transformer;
import com.hortonworks.iotas.streams.schema.exception.BadComponentConfigException;
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
        return normalizationProcessorJson;
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
        return normalizationProcessorJson;
    }

}
