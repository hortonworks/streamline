package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.design.rule.action.Action;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RulesSchemaEvolverTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testEvolveWithoutTransformation() throws IOException, BadComponentConfigException {
        final RulesSchemaEvolver evolver = new RulesSchemaEvolver();

        String componentConfig = buildRulesProcessorComponentConfigWithNoTransformation();
        Schema schema = buildInputSchema();

        Map<String, Schema> schemaMap = evolver.apply(componentConfig, schema);
        assertEquals(schemaMap.size(), 4);

        assertOutputStreamWithSchema(schemaMap, "RULE1.RULE1.1.HBASE", schema);
        assertOutputStreamWithSchema(schemaMap, "RULE1.RULE1.1.HDFS", schema);
        assertOutputStreamWithSchema(schemaMap, "RULE1.RULE2.2.HBASE", schema);
        assertOutputStreamWithSchema(schemaMap, "RULE1.RULE2.2.HDFS", schema);
    }

    @Test
    public void testEvolveWithTransformation() throws IOException, BadComponentConfigException {
        final RulesSchemaEvolver evolver = new RulesSchemaEvolver();

        String componentConfig = buildRulesProcessorComponentConfigWithTransformation();
        Schema schema = buildInputSchema();

        Map<String, Schema> schemaMap = evolver.apply(componentConfig, schema);
        assertEquals(schemaMap.size(), 1);

        Schema expectedSchema = new Schema.SchemaBuilder()
                .fields(new Schema.Field("field1", Schema.Type.STRING),
                        new Schema.Field("newfield", Schema.Type.STRING))
                .build();

        assertOutputStreamWithSchema(schemaMap, "RULE1.RULE1.1.NOTIFICATION", expectedSchema);
    }

    private void assertOutputStreamWithSchema(Map<String, Schema> schemaMap, String expectedStreamName, Schema schema) {
        assertTrue(schemaMap.containsKey(expectedStreamName));
        Schema streamSchema = schemaMap.get(expectedStreamName);
        assertEquals(schema, streamSchema);
    }

    private String buildRulesProcessorComponentConfigWithNoTransformation() throws IOException {
        Action action = createAction("HBASE");
        Action action2 = createAction("HDFS");

        String ruleProcessorName = "RULE1";

        Rule rule = createRule(1L, "RULE1", ruleProcessorName, Lists.newArrayList(action, action2));
        Rule rule2 = createRule(2L, "RULE2", ruleProcessorName, Lists.newArrayList(action, action2));

        RulesProcessor rulesProcessor = createRulesProcessor(ruleProcessorName, Lists.newArrayList(rule, rule2));

        String rulesProcessorJson = objectMapper.writeValueAsString(rulesProcessor);

        Map<String, Object> componentConfig = buildComponentConfig(rulesProcessorJson);
        return objectMapper.writeValueAsString(componentConfig);
    }

    private String buildRulesProcessorComponentConfigWithTransformation() throws IOException {
        Action action = createAction("NOTIFICATION");
        action.setNotifierName("NOTIFIER");

        Map<String, Object> outputFieldsAndDefaults = Maps.newHashMap();
        // for testing same field with different type
        outputFieldsAndDefaults.put("field1", 1L);
        // for testing new field
        outputFieldsAndDefaults.put("newfield", "hello");
        action.setOutputFieldsAndDefaults(outputFieldsAndDefaults);
        action.setIncludeMeta(true);

        String ruleProcessorName = "RULE1";
        Rule rule = createRule(1L, "RULE1", ruleProcessorName, Lists.newArrayList(action));
        RulesProcessor rulesProcessor = createRulesProcessor(ruleProcessorName, Lists.newArrayList(rule));

        String rulesProcessorJson = objectMapper.writeValueAsString(rulesProcessor);

        Map<String, Object> componentConfig = buildComponentConfig(rulesProcessorJson);
        return objectMapper.writeValueAsString(componentConfig);
    }

    private Action createAction(String actionName) {
        Action action = new Action();
        action.setName(actionName);
        return action;
    }

    private Rule createRule(long id, String ruleName, String ruleProcessorName, ArrayList<Action> actions) {
        Rule rule = new Rule();
        rule.setId(id);
        rule.setName(ruleName);
        rule.setRuleProcessorName(ruleProcessorName);
        rule.setActions(actions);
        return rule;
    }

    private RulesProcessor createRulesProcessor(String ruleProcessorName, List<Rule> rules) {
        RulesProcessor rulesProcessor = new RulesProcessor();
        rulesProcessor.setId(1234L);
        rulesProcessor.setName(ruleProcessorName);
        rulesProcessor.setRules(rules);
        return rulesProcessor;
    }

    private Map<String, Object> buildComponentConfig(String rulesProcessorJson) throws IOException {
        Map<String, Object> componentConfig = Maps.newHashMap();
        componentConfig.put("uiname", "RULE1");
        componentConfig.put("type", "RULE");
        componentConfig.put("transformationClass", "dummy");

        Map<String, Object> configMap = Maps.newHashMap();
        configMap.put("parallelism", 1);
        configMap.put("rulesProcessorConfig", objectMapper.readValue(rulesProcessorJson, Map.class));

        componentConfig.put("config", configMap);
        return componentConfig;
    }

    private Schema buildInputSchema() {
        Schema.SchemaBuilder schemaBuilder = new Schema.SchemaBuilder();

        schemaBuilder.field(new Schema.Field("field1", Schema.Type.STRING));
        schemaBuilder.field(new Schema.Field("field2", Schema.Type.LONG));
        schemaBuilder.field(new Schema.Field("field3", Schema.Type.STRING));

        return schemaBuilder.build();
    }
}
