package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.design.component.RulesProcessorJsonBuilder;
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.design.rule.action.Action;
import com.hortonworks.iotas.parser.ParseException;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;

import java.util.List;
import java.util.Map;

public class RulesSchemaEvolver implements EvolvingSchema {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Schema> apply(String config, Schema inputSchema) throws BadComponentConfigException {
        try {
            Map<String, Object> componentConfig = objectMapper.readValue(config, Map.class);
            Map<String, Object> rulesConfig = (Map<String, Object>) componentConfig.get(
                    TopologyLayoutConstants.JSON_KEY_CONFIG);
            Map<String, Object> rulesProcessorConfig = (Map<String, Object>) rulesConfig.get(
                    TopologyLayoutConstants.JSON_KEY_RULES_PROCESSOR_CONFIG);

            RulesProcessor rulesProcessor = buildRulesProcessor(rulesProcessorConfig);
            Map<String, Schema> schemaMap = Maps.newHashMap();
            for (Rule rule : rulesProcessor.getRules()) {
                List<SchemaForStream> schemasForStreams = extractSchemasFromRule(inputSchema, rule);
                for (SchemaForStream schemasForStream : schemasForStreams) {
                    schemaMap.put(schemasForStream.getStreamId(), schemasForStream.getSchema());
                }
            }

            return schemaMap;
        } catch (Exception e) {
            throw new BadComponentConfigException("Exception while simulating evolution of rules schema", e);
        }
    }

    private RulesProcessor buildRulesProcessor(Map<String, Object> rulesProcessorConfig) throws JsonProcessingException {
        String rulesProcessorConfigJson = objectMapper.writeValueAsString(rulesProcessorConfig);
        RulesProcessorJsonBuilder rulesProcessorJsonBuilder = new RulesProcessorJsonBuilder(rulesProcessorConfigJson);
        return rulesProcessorJsonBuilder.build();
    }

    private List<SchemaForStream> extractSchemasFromRule(Schema inputSchema, Rule rule) throws ParseException {
        List<SchemaForStream> schemaForStreams = Lists.newArrayList();

        for (Action action : rule.getActions()) {
            SchemaForStream schemaForStream = extractSchemaFromAction(inputSchema, rule.getRuleProcessorName(),
                    rule.getName(), rule.getId(), action);
            schemaForStreams.add(schemaForStream);
        }

        return schemaForStreams;
    }

    private SchemaForStream extractSchemaFromAction(Schema inputSchema, String ruleProcessorName, String ruleName, Long ruleId,
                                         Action action) throws ParseException {
        String actionName = action.getName();

        // FIXME: how to know stream id without runtime initialization??
        String streamId = buildStreamName(ruleProcessorName, ruleName, ruleId, actionName);

        Map<String, Object> outputFieldsAndDefaults = action.getOutputFieldsAndDefaults();

        if (outputFieldsAndDefaults != null && !outputFieldsAndDefaults.isEmpty()) {
            Schema schema = simulateFieldsProjection(inputSchema, outputFieldsAndDefaults);
            return new SchemaForStream(streamId, schema);
        } else {
            // no projection
            return new SchemaForStream(streamId, inputSchema);
        }
    }

    private Schema simulateFieldsProjection(Schema inputSchema, Map<String, Object> outputFieldsAndDefaults) throws ParseException {
        Schema.SchemaBuilder schemaBuilder = new Schema.SchemaBuilder();

        // projection
        for (Map.Entry<String, Object> fieldAndDefault : outputFieldsAndDefaults.entrySet()) {
            String fieldName = fieldAndDefault.getKey();
            Object defaultValue = fieldAndDefault.getValue();
            Schema.Field fieldFromDefault = Schema.Field.of(fieldName, Schema.fromJavaType(defaultValue));

            Schema.Field foundField = findFieldByName(inputSchema.getFields(), fieldFromDefault.getName());
            if (null != foundField) {
                schemaBuilder.field(foundField);
            } else {
                schemaBuilder.field(fieldFromDefault);
            }
        }

        return schemaBuilder.build();
    }

    private Schema.Field findFieldByName(List<Schema.Field> fields, String fieldName) {
        for (Schema.Field field : fields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }

        return null;
    }

    private String buildStreamName(String ruleProcessorName, String ruleName, Long ruleId, String actionName) {
        // FIXME: stream name is built on runtime, but EvolvingSchema should play with design time...
        // how to extract it into design time?
        return ruleProcessorName + "." + ruleName + "." + ruleId + "." + actionName;
    }

    class SchemaForStream {
        private final String streamId;
        private final Schema schema;

        public SchemaForStream(String streamId, Schema schema) {
            this.streamId = streamId;
            this.schema = schema;
        }

        public String getStreamId() {
            return streamId;
        }

        public Schema getSchema() {
            return schema;
        }
    }
}
