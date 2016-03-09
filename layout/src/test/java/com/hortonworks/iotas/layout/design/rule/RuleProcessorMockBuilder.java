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

package com.hortonworks.iotas.layout.design.rule;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.common.Schema.Field;
import com.hortonworks.iotas.layout.design.component.Component;
import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.design.component.RulesProcessorBuilder;
import com.hortonworks.iotas.layout.design.component.Sink;
import com.hortonworks.iotas.layout.design.rule.action.Action;
import com.hortonworks.iotas.layout.design.rule.condition.Condition;
import com.hortonworks.iotas.layout.design.rule.condition.Condition.ConditionElement.LogicalOperator;
import com.hortonworks.iotas.layout.design.rule.condition.Condition.ConditionElement.Operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuleProcessorMockBuilder implements RulesProcessorBuilder {
    public static final String TEMPERATURE = "temperature";
    public static final String HUMIDITY = "humidity";
    public static final String RULE_PROCESSOR = "rule_processor";
    public static final String RULE = "rule";
    public static final String SINK = "sink";

    private final long ruleProcessorId;
    private final int numRules;
    private final int numSinks;
    private List<Field> declaredInputsOutputs;

    public RuleProcessorMockBuilder(long ruleProcessorId, int numRules, int numSinksPerRule) {
        this.ruleProcessorId = ruleProcessorId;
        this.numRules = numRules;
        this.numSinks = numSinksPerRule;
    }

    @Override
    public RulesProcessor build() {
        RulesProcessor rulesProcessor = new RulesProcessor();
        rulesProcessor.setDeclaredInput(buildDeclaredInputsOutputs());
        rulesProcessor.setId(ruleProcessorId);
        rulesProcessor.setName(RULE_PROCESSOR + "_" + ruleProcessorId);
        rulesProcessor.setDescription(RULE_PROCESSOR + "_" + ruleProcessorId + "_desc");
        rulesProcessor.setRules(buildRules());
        return rulesProcessor;
    }

    private List<Field> buildDeclaredInputsOutputs() {
        final Schema declaredInputsOutputs = new Schema.SchemaBuilder().fields(new ArrayList<Field>() {{
            add(new Field(TEMPERATURE, Schema.Type.INTEGER));
            add(new Field(HUMIDITY, Schema.Type.INTEGER));
        }}).build();

        this.declaredInputsOutputs = declaredInputsOutputs.getFields();
        return declaredInputsOutputs.getFields();
    }

    private List<Rule> buildRules() {
        List<Rule> rules = new ArrayList<>();
        for (int i = 1; i <= numRules; i++) {
            rules.add(buildRule(i, buildCondition(i), buildAction(buildSinks())));
        }
        return rules;
    }

    private Rule buildRule(long ruleId, Condition condition, Action action) {
        Rule rule = new Rule();
        rule.setId(ruleId);
        rule.setName(RULE + "_" + ruleId);
        rule.setDescription(RULE + "_" + ruleId + "_desc");
        rule.setRuleProcessorName(RULE_PROCESSOR + "_" + ruleProcessorId);
        rule.setCondition(condition);
        rule.setActions(Collections.singletonList(action));
        return rule;
    }

    private Action buildAction(List<Component> sinks) {
        Action action = new Action();
        return action;
    }

    private List<Component> buildSinks() {
        List<Component> sinks = new ArrayList<>();
        for (int i = 1; i <= numSinks; i++) {
            sinks.add(buildSink(i));
        }
        return sinks;
    }

    private Sink buildSink(long sinkId) {
        Sink sink = new Sink();
        sink.setId(ruleProcessorId);
        sink.setName(SINK + "_" + sinkId);
        sink.setDescription(SINK + "_" + sinkId + "_desc");
        sink.setDeclaredInput(declaredInputsOutputs);
        return sink;
    }

    private Condition buildCondition(int idx) {
        if (idx % 2 == 0) {
            return buildCondition(buildConditionElements(Operation.GREATER_THAN)); // temperature  > 100  &&  humidity  > 50
        }
        return buildCondition(buildConditionElements(Operation.LESS_THAN));        // temperature  < 100  &&  humidity  < 50
    }

    private Condition buildCondition(List<Condition.ConditionElement> conditionElements) {
        Condition condition = new Condition();
        condition.setConditionElements(conditionElements);
        return condition;
    }

    private List<Condition.ConditionElement> buildConditionElements(Operation operation) {
        List<Condition.ConditionElement> conditionElements = new ArrayList<>();
        conditionElements.add(buildConditionElement(TEMPERATURE, operation, "100", LogicalOperator.AND));
        conditionElements.add(buildConditionElement(HUMIDITY, operation, "50", null));
        return conditionElements;
    }

    private Condition.ConditionElement buildConditionElement(
            String firstOperand, Operation operation, String secondOperand,
            LogicalOperator logicalOperator) {

        final Condition.ConditionElement conditionElement = new Condition.ConditionElement();
        final Field firstOperandField = new Field(firstOperand, Schema.Type.INTEGER);
        conditionElement.setFirstOperand(firstOperandField);
        conditionElement.setOperation(operation);
        conditionElement.setSecondOperand(secondOperand);
        if (logicalOperator != null) {
            conditionElement.setLogicalOperator(logicalOperator);
        }
        return conditionElement;
    }
}
