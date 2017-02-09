/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/


package com.hortonworks.streamline.streams.runtime.storm.layout.runtime.rule.topology;

import com.google.common.collect.ImmutableList;
import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.common.Schema.Field;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.Sink;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;
import com.hortonworks.streamline.streams.layout.component.rule.action.Action;
import com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction;
import com.hortonworks.streamline.streams.layout.component.rule.expression.BinaryExpression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Condition;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Expression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.FieldExpression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.FunctionExpression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Literal;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Operator;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Projection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RulesProcessorMock {
    public static final String TEMPERATURE = "temperature";
    public static final String HUMIDITY = "humidity";
    public static final String RULE_PROCESSOR = "rule_processor";
    public static final String RULE = "rule";
    public static final String SINK = "sink";

    private final long ruleProcessorId;
    private final int numRules;
    private final int numSinks;

    public RulesProcessorMock(long ruleProcessorId, int numRules, int numSinksPerRule) {
        this.ruleProcessorId = ruleProcessorId;
        this.numRules = numRules;
        this.numSinks = numSinksPerRule;
    }

    public RulesProcessor get() {
        RulesProcessor rulesProcessor = new RulesProcessor();
        rulesProcessor.setId(String.valueOf(ruleProcessorId));
        rulesProcessor.setName(RULE_PROCESSOR + "_" + ruleProcessorId);
        rulesProcessor.setRules(getRules());
        return rulesProcessor;
    }

    private List<Rule> getRules() {
        List<Rule> rules = new ArrayList<>();
        for (int i = 1; i <= numRules; i++) {
            rules.add(getRule(i, getCondition(i), getAction(getSinks())));
        }
        return rules;
    }

    public static class Incr {
        public static Integer evaluate(Integer input, Integer incr) {
            return input + incr;
        }
    }

    private Rule getRule(long ruleId, Condition condition, TransformAction action) {
        Rule rule = new Rule();
        rule.setId(ruleId);
        rule.setName(RULE + "_" + ruleId);
        rule.setDescription(RULE + "_" + ruleId + "_desc");
        rule.setRuleProcessorName(RULE_PROCESSOR + "_" + ruleProcessorId);
        rule.setCondition(condition);
        if (ruleId % 2 == 0) {
            Projection projection = new Projection();
            Expression humidity = new FieldExpression(Field.of("humidity", Schema.Type.INTEGER));
            Expression deviceName = new FieldExpression(Field.of("devicename", Schema.Type.STRING));
            Expression incr = new FunctionExpression("INCR",
                                                     "com.hortonworks.streamline.streams.runtime.storm.layout.runtime.rule.topology.RulesProcessorMock$Incr",
                                                     ImmutableList.<Expression>of(humidity, new Literal("10")));
            Expression upper = new FunctionExpression("UPPER", ImmutableList.<Expression>of(deviceName));
            projection.setExpressions(ImmutableList.<Expression>of(humidity, incr, upper));
            rule.setProjection(projection);
        }
        rule.setActions(Collections.singletonList((Action) action));
        return rule;
    }

    private TransformAction getAction(List<Sink> sinks) {
        return new TransformAction();
    }

    private List<Sink> getSinks() {
        List<Sink> sinks = new ArrayList<>();
        for (int i = 1; i <= numSinks; i++) {
            sinks.add(getSink(i));
        }
        return sinks;
    }

    private Sink getSink(long sinkId) {
        StreamlineSink sink = new StreamlineSink();
        sink.setId(String.valueOf(ruleProcessorId));
        sink.setName(SINK + "_" + sinkId);
        return sink;
    }

    private Condition getCondition(int idx) {
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
