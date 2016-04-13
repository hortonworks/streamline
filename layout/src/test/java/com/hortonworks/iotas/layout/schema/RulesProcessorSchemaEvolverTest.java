/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.design.rule.action.Action;
import com.hortonworks.iotas.layout.design.rule.action.TransformAction;
import com.hortonworks.iotas.layout.design.rule.action.NotifierAction;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RulesProcessorSchemaEvolverTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testEvolveWithoutTransformation() throws IOException, BadComponentConfigException {
        final RulesProcessorSchemaEvolver evolver = new RulesProcessorSchemaEvolver();

        String componentConfig = buildRulesProcessorComponentConfigWithNoTransformation();
        Stream stream = EvolvingSchemaTestObject.inputStream();

        Set<Stream> streams = evolver.apply(componentConfig, stream);
        assertEquals(streams.size(), 4);

        assertTrue(streams.contains(new Stream("RULE1.RULE1.1.HBASE", stream.getSchema())));
        assertTrue(streams.contains(new Stream("RULE1.RULE1.1.HDFS", stream.getSchema())));
        assertTrue(streams.contains(new Stream("RULE1.RULE2.2.HBASE", stream.getSchema())));
        assertTrue(streams.contains(new Stream("RULE1.RULE2.2.HDFS", stream.getSchema())));
    }

    @Test
    public void testEvolveWithTransformation() throws IOException, BadComponentConfigException {
        final RulesProcessorSchemaEvolver evolver = new RulesProcessorSchemaEvolver();

        String componentConfig = buildRulesProcessorComponentConfigWithTransformation();
        Stream stream = EvolvingSchemaTestObject.inputStream();

        Set<Stream> streams = evolver.apply(componentConfig, stream);
        assertEquals(streams.size(), 1);

        Schema expectedSchema = new Schema.SchemaBuilder()
                .fields(new Schema.Field("field1", Schema.Type.STRING),
                        new Schema.Field("newfield", Schema.Type.STRING))
                .build();

        assertTrue(streams.contains(new Stream("RULE1.RULE1.1.NOTIFICATION", expectedSchema)));
    }

    private String buildRulesProcessorComponentConfigWithNoTransformation() throws IOException {
        TransformAction action = createForwardAction("HBASE");
        TransformAction action2 = createForwardAction("HDFS");

        String ruleProcessorName = "RULE1";

        Rule rule = createRule(1L, "RULE1", ruleProcessorName, Lists.<Action>newArrayList(action, action2));
        Rule rule2 = createRule(2L, "RULE2", ruleProcessorName, Lists.<Action>newArrayList(action, action2));

        RulesProcessor rulesProcessor = createRulesProcessor(ruleProcessorName, Lists.newArrayList(rule, rule2));

        String rulesProcessorJson = objectMapper.writeValueAsString(rulesProcessor);

        Map<String, Object> componentConfig = buildComponentConfig(rulesProcessorJson);
        return objectMapper.writeValueAsString(componentConfig);
    }

    private String buildRulesProcessorComponentConfigWithTransformation() throws IOException {
        NotifierAction action = createNotifierAction("NOTIFICATION");
        action.setNotifierName("NOTIFIER");

        Map<String, Object> outputFieldsAndDefaults = Maps.newHashMap();
        // for testing same field with different type
        outputFieldsAndDefaults.put("field1", 1L);
        // for testing new field
        outputFieldsAndDefaults.put("newfield", "hello");
        action.setOutputFieldsAndDefaults(outputFieldsAndDefaults);

        String ruleProcessorName = "RULE1";
        Rule rule = createRule(1L, "RULE1", ruleProcessorName, Lists.<Action>newArrayList(action));
        RulesProcessor rulesProcessor = createRulesProcessor(ruleProcessorName, Lists.newArrayList(rule));

        String rulesProcessorJson = objectMapper.writeValueAsString(rulesProcessor);

        Map<String, Object> componentConfig = buildComponentConfig(rulesProcessorJson);
        return objectMapper.writeValueAsString(componentConfig);
    }

    private TransformAction createForwardAction(String actionName) {
        TransformAction action = new TransformAction();
        action.setName(actionName);
        return action;
    }

    private NotifierAction createNotifierAction(String actionName) {
        NotifierAction action = new NotifierAction();
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
        rulesProcessor.setId("1234");
        rulesProcessor.setName(ruleProcessorName);
        rulesProcessor.setRules(rules);
        return rulesProcessor;
    }

    private Map<String, Object> buildComponentConfig(String rulesProcessorJson) throws IOException {
        Map<String, Object> componentConfig = Maps.newHashMap();
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_UINAME, "RULE1");
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_TYPE, "RULE");
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_TRANSFORMATION_CLASS, "dummy");

        Map<String, Object> configMap = Maps.newHashMap();
        configMap.put(TopologyLayoutConstants.JSON_KEY_PARALLELISM, 1);
        configMap.put(TopologyLayoutConstants.JSON_KEY_RULES_PROCESSOR_CONFIG, objectMapper.readValue(rulesProcessorJson, Map.class));

        componentConfig.put("config", configMap);
        return componentConfig;
    }
}
