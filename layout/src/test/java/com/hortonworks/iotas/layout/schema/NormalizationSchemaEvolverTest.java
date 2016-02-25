package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.NormalizationProcessor;
import com.hortonworks.iotas.layout.design.normalization.FieldValueGenerator;
import com.hortonworks.iotas.layout.design.normalization.Transformer;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NormalizationSchemaEvolverTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testEvolveFullNormalization() throws IOException, BadComponentConfigException {
        final NormalizationSchemaEvolver evolver = new NormalizationSchemaEvolver();

        Schema inputSchema = EvolverSchemaTestObject.inputSchema();

        NormalizationProcessor processor = new NormalizationProcessor();

        // transform
        Transformer transformer1 = new Transformer(
                Schema.Field.of("field1", Schema.Type.STRING),
                Schema.Field.of("field4", Schema.Type.STRING));
        processor.setTransformers(Lists.newArrayList(transformer1));

        // filter
        processor.setFieldsToBeFiltered(Lists.newArrayList("field2"));

        // output field - value generate
        FieldValueGenerator generator = new FieldValueGenerator(
                Schema.Field.of("field5", Schema.Type.STRING), "world");
        processor.setNewFieldValueGenerators(Lists.newArrayList(generator));

        String normalizationJson = objectMapper.writeValueAsString(processor);
        String componentConfig = buildComponentConfig(normalizationJson);

        Map<String, Schema> schemaMap = evolver.apply(componentConfig, inputSchema);
        assertEquals(schemaMap.size(), 1);

        Schema expectedSchema = new Schema.SchemaBuilder()
                .field(Schema.Field.of("field3", Schema.Type.STRING))
                .field(Schema.Field.of("field4", Schema.Type.STRING))
                .field(Schema.Field.of("field5", Schema.Type.STRING))
                .build();

        assertOutputStreamWithSchema(schemaMap, "default", expectedSchema);
    }

    @Test(expected = BadComponentConfigException.class)
    public void testEvolveTransformationWithoutHavingInputFieldAsInputSchema() throws IOException, BadComponentConfigException {
        final NormalizationSchemaEvolver evolver = new NormalizationSchemaEvolver();

        Schema inputSchema = EvolverSchemaTestObject.inputSchema();

        NormalizationProcessor processor = new NormalizationProcessor();

        // transform
        Transformer transformer1 = new Transformer(
                Schema.Field.of("field4", Schema.Type.STRING),
                Schema.Field.of("field6", Schema.Type.STRING));
        processor.setTransformers(Lists.newArrayList(transformer1));

        processor.setFieldsToBeFiltered(Lists.<String>newArrayList());
        processor.setNewFieldValueGenerators(Lists.<FieldValueGenerator>newArrayList());

        String normalizationJson = objectMapper.writeValueAsString(processor);
        String componentConfig = buildComponentConfig(normalizationJson);

        evolver.apply(componentConfig, inputSchema);
    }

    @Test
    public void testEvolveTransformationHavingInputFieldAsInputSchemaButDifferentType() throws IOException, BadComponentConfigException {
        final NormalizationSchemaEvolver evolver = new NormalizationSchemaEvolver();

        Schema inputSchema = EvolverSchemaTestObject.inputSchema();

        NormalizationProcessor processor = new NormalizationProcessor();

        // transform
        // NOTE: type of field1 in input schema is STRING
        Transformer transformer1 = new Transformer(
                Schema.Field.of("field1", Schema.Type.LONG),
                Schema.Field.of("field4", Schema.Type.LONG));
        processor.setTransformers(Lists.newArrayList(transformer1));

        processor.setFieldsToBeFiltered(Lists.<String>newArrayList());
        processor.setNewFieldValueGenerators(Lists.<FieldValueGenerator>newArrayList());

        String normalizationJson = objectMapper.writeValueAsString(processor);
        String componentConfig = buildComponentConfig(normalizationJson);

        Map<String, Schema> schemaMap = evolver.apply(componentConfig, inputSchema);
        assertEquals(schemaMap.size(), 1);

        Schema expectedSchema = new Schema.SchemaBuilder()
                .field(Schema.Field.of("field2", Schema.Type.LONG))
                .field(Schema.Field.of("field3", Schema.Type.STRING))
                .field(Schema.Field.of("field4", Schema.Type.LONG))
                .build();

        assertOutputStreamWithSchema(schemaMap, "default", expectedSchema);
    }

    @Test
    public void testEvolveFilterNotExistField() throws IOException, BadComponentConfigException {
        final NormalizationSchemaEvolver evolver = new NormalizationSchemaEvolver();

        Schema inputSchema = EvolverSchemaTestObject.inputSchema();

        NormalizationProcessor processor = new NormalizationProcessor();

        processor.setTransformers(Lists.<Transformer>newArrayList());

        // filter
        // NOTE: field99 does not exist in input schema
        processor.setFieldsToBeFiltered(Lists.newArrayList("field99"));

        processor.setNewFieldValueGenerators(Lists.<FieldValueGenerator>newArrayList());

        String normalizationJson = objectMapper.writeValueAsString(processor);
        String componentConfig = buildComponentConfig(normalizationJson);

        Map<String, Schema> schemaMap = evolver.apply(componentConfig, inputSchema);
        assertEquals(schemaMap.size(), 1);

        // expected that it has no effect
        assertOutputStreamWithSchema(schemaMap, "default", inputSchema);
    }

    @Test
    public void testEvolveOutputFieldAlreadyExistButDifferentTpe() throws IOException, BadComponentConfigException {
        final NormalizationSchemaEvolver evolver = new NormalizationSchemaEvolver();

        Schema inputSchema = EvolverSchemaTestObject.inputSchema();

        NormalizationProcessor processor = new NormalizationProcessor();

        // output field - value generate
        // NOTE: type of field3 in input schema is STRING
        processor.setTransformers(Lists.<Transformer>newArrayList());

        processor.setFieldsToBeFiltered(Lists.<String>newArrayList());

        FieldValueGenerator generator = new FieldValueGenerator(
                Schema.Field.of("field3", Schema.Type.LONG), 1L);
        processor.setNewFieldValueGenerators(Lists.newArrayList(generator));

        String normalizationJson = objectMapper.writeValueAsString(processor);
        String componentConfig = buildComponentConfig(normalizationJson);

        Map<String, Schema> schemaMap = evolver.apply(componentConfig, inputSchema);
        assertEquals(schemaMap.size(), 1);

        // expected that it has no effect
        assertOutputStreamWithSchema(schemaMap, "default", inputSchema);
    }

    private void assertOutputStreamWithSchema(Map<String, Schema> schemaMap, String expectedStreamName, Schema schema) {
        assertTrue(schemaMap.containsKey(expectedStreamName));
        Schema streamSchema = schemaMap.get(expectedStreamName);
        assertEquals(schema, streamSchema);
    }

    private String buildComponentConfig(String normalizationProcessorJson) throws IOException {
        return objectMapper.writeValueAsString(buildComponentConfigMap(normalizationProcessorJson));
    }

    private Map<String, Object> buildComponentConfigMap(String normalizationProcessorJson) throws IOException {
        Map<String, Object> componentConfig = Maps.newHashMap();
        componentConfig.put("uiname", "NORMALIZE1");
        componentConfig.put("type", "NORMALIZE");
        componentConfig.put("transformationClass", "dummy");

        Map<String, Object> configMap = Maps.newHashMap();
        configMap.put("parallelism", 1);
        configMap.put("normalizationProcessorConfig", objectMapper.readValue(normalizationProcessorJson, Map.class));

        componentConfig.put("config", configMap);
        return componentConfig;
    }
}
