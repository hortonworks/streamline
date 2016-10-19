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

package org.apache.streamline.streams.layout.topology.component.rule;

import org.apache.streamline.common.Schema;
import org.apache.streamline.common.Schema.Field;
import org.apache.streamline.streams.layout.component.ComponentBuilder;
import org.apache.streamline.streams.layout.component.IotasSink;
import org.apache.streamline.streams.layout.component.Sink;
import org.apache.streamline.streams.layout.component.impl.RulesProcessor;
import org.apache.streamline.streams.layout.component.rule.Rule;
import org.apache.streamline.streams.layout.component.rule.action.Action;
import org.apache.streamline.streams.layout.component.rule.action.TransformAction;
import org.apache.streamline.streams.layout.component.rule.expression.BinaryExpression;
import org.apache.streamline.streams.layout.component.rule.expression.Condition;
import org.apache.streamline.streams.layout.component.rule.expression.Expression;
import org.apache.streamline.streams.layout.component.rule.expression.FieldExpression;
import org.apache.streamline.streams.layout.component.rule.expression.Literal;
import org.apache.streamline.streams.layout.component.rule.expression.Operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuleProcessorMockBuilder implements ComponentBuilder<RulesProcessor> {
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
        rulesProcessor.setId(String.valueOf(ruleProcessorId));
        rulesProcessor.setName(RULE_PROCESSOR + "_" + ruleProcessorId);
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

    private Rule buildRule(long ruleId, Condition condition, TransformAction action) {
        Rule rule = new Rule();
        rule.setId(ruleId);
        rule.setName(RULE + "_" + ruleId);
        rule.setDescription(RULE + "_" + ruleId + "_desc");
        rule.setRuleProcessorName(RULE_PROCESSOR + "_" + ruleProcessorId);
        rule.setCondition(condition);
        rule.setActions(Collections.singletonList((Action) action));
        return rule;
    }

    private TransformAction buildAction(List<Sink> sinks) {
        return new TransformAction();
    }

    private List<Sink> buildSinks() {
        List<Sink> sinks = new ArrayList<>();
        for (int i = 1; i <= numSinks; i++) {
            sinks.add(buildSink(i));
        }
        return sinks;
    }

    private Sink buildSink(long sinkId) {
        IotasSink sink = new IotasSink();
        sink.setId(String.valueOf(ruleProcessorId));
        sink.setName(SINK + "_" + sinkId);
        return sink;
    }

    private Condition buildCondition(int idx) {
        Condition condition = new Condition();
        if (idx % 2 == 0) {
            condition.setExpression(comparisonOperation(Operator.GREATER_THAN));// temperature  > 100  &&  humidity  > 50
        } else {
            condition.setExpression(comparisonOperation(Operator.LESS_THAN));        // temperature  < 100  &&  humidity  < 50
        }
        return condition;
    }

    private Expression comparisonOperation(Operator operator) {
        return binaryOperation(Operator.AND,
                               binaryOperation(operator, new FieldExpression(Field.of("temperature", Schema.Type.INTEGER)),
                                               new Literal("100")),
                               binaryOperation(operator, new FieldExpression(Field.of("humidity", Schema.Type.INTEGER)),
                                               new Literal("50")));
    }

    private Expression binaryOperation(Operator operator, Expression left, Expression right) {
        return new BinaryExpression(operator, left, right);
    }
}
