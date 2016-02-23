package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.NormalizationProcessor;
import com.hortonworks.iotas.layout.design.component.NormalizationProcessorJsonBuilder;
import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.design.component.RulesProcessorJsonBuilder;
import com.hortonworks.iotas.layout.design.normalization.FieldValueGenerator;
import com.hortonworks.iotas.layout.design.normalization.Transformer;
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.design.rule.action.Action;
import com.hortonworks.iotas.parser.ParseException;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class NormalizationSchemaEvolver implements EvolvingSchema {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Schema> apply(String config, Schema inputSchema) throws BadComponentConfigException {
        try {
            Map<String, Object> componentConfig = objectMapper.readValue(config, Map.class);
            Map<String, Object> normalizationConfig = (Map<String, Object>) componentConfig.get(
                    TopologyLayoutConstants.JSON_KEY_CONFIG);
            Map<String, Object> normalizationProcessorConfig = (Map<String, Object>) normalizationConfig.get(
                    TopologyLayoutConstants.JSON_KEY_NORMALIZATION_PROCESSOR_CONFIG);

            NormalizationProcessor normalizationProcessor = buildNormalizationProcessor(normalizationProcessorConfig);
            Map<String, Schema> schemaMap = Maps.newHashMap();
            // FIXME: Please notice that this implementation doesn't cover IOT-64 for now
            Schema outputSchema = simulateNormalization(normalizationProcessor, inputSchema);
            // FIXME: how can evolver know about output stream information?
            schemaMap.put("default", outputSchema);

            return schemaMap;
        } catch (Exception e) {
            throw new BadComponentConfigException("Exception while simulating evolution of rules schema", e);
        }
    }

    private NormalizationProcessor buildNormalizationProcessor(Map<String, Object> normalizationProcessorConfig) throws JsonProcessingException {
        String normalizationProcessorConfigJson = objectMapper.writeValueAsString(normalizationProcessorConfig);
        NormalizationProcessorJsonBuilder processorJsonBuilder = new NormalizationProcessorJsonBuilder(normalizationProcessorConfigJson);
        return processorJsonBuilder.build();
    }

    private Schema simulateNormalization(NormalizationProcessor normalizationProcessor, Schema inputSchema) {
        List<Schema.Field> outputFields = Lists.newArrayList(inputSchema.getFields());

        applyTransform(normalizationProcessor.getTransformers(), outputFields);
        applyFilter(normalizationProcessor.getFieldsToBeFiltered(), outputFields);
        applyOutputFieldValueGenerator(normalizationProcessor.getNewFieldValueGenerators(), outputFields);

        return Schema.of(outputFields);
    }

    private void applyTransform(List<Transformer> transformers, List<Schema.Field> outputFields) {
        for (Transformer transformer : transformers) {
            Schema.Field inputField = transformer.getInputField();
            Schema.Field outputField = transformer.getOutputField();

            Schema.Field targetField = findFieldByName(outputFields, inputField.getName());
            if (targetField == null) {
                // field not found
                throw new IllegalArgumentException("Input schema does not have input field: " + inputField.getName());
            }

            outputFields.remove(targetField);
            outputFields.add(outputField);
        }
    }

    private void applyFilter(List<String> fieldsToBeFiltered, List<Schema.Field> outputFields) {
        for (String fieldNameToBeFiltered : fieldsToBeFiltered) {
            Schema.Field matchedField = findFieldByName(outputFields, fieldNameToBeFiltered);
            if (matchedField != null) {
                outputFields.remove(matchedField);
            }
        }
    }

    private void applyOutputFieldValueGenerator(List<FieldValueGenerator> newFieldValueGenerators, List<Schema.Field> outputFields) {
        for (FieldValueGenerator newFieldValueGenerator : newFieldValueGenerators) {
            Schema.Field outputField = newFieldValueGenerator.getField();
            Schema.Field foundField = findFieldByName(outputFields, outputField.getName());
            if (foundField == null) {
                // add field only if it does not exist
                outputFields.add(outputField);
            }
        }
    }

    private Schema.Field findFieldByName(List<Schema.Field> fields, String fieldName) {
        for (Schema.Field field : fields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }

        return null;
    }
}
