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

package com.hortonworks.iotas.layout.runtime.rule.condition.expression;

import com.hortonworks.iotas.topology.component.rule.condition.Condition;
import com.hortonworks.iotas.topology.component.rule.condition.ExpressionTranslator;
import com.hortonworks.iotas.topology.component.rule.condition.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Represents the expression of this {@link Condition} in Groovy language syntax
 **/
public class GroovyExpression extends ExpressionRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(GroovyExpression.class);


    public GroovyExpression(Condition condition) {
        super(condition);
    }

    @Override
    public String asString() {
        if (expression == null) {           // Builds and caches the expression string the first time it is called
            GroovyExpressionTranslator expressionTranslator = new GroovyExpressionTranslator();
            condition.getExpression().accept(expressionTranslator);
            expression = expressionTranslator.getTranslatedExpression();
        }
        return expression;
    }

    private static class GroovyExpressionTranslator extends ExpressionTranslator {
        protected String getOperator(Operator operator) {
            switch (operator) {
                case AND:
                    return " && ";
                case OR:
                    return " || ";
                case EQUALS:
                    return " == ";
                case NOT_EQUAL:
                    return " != ";
                case GREATER_THAN:
                    return " > ";
                case LESS_THAN:
                    return " < ";
                case GREATER_THAN_EQUALS_TO:
                    return " >= ";
                case LESS_THAN_EQUALS_TO:
                    return " <= ";
                default:
                    throw new UnsupportedOperationException(
                            String.format("Operator [%s] not supported. List of supported operators: %s",
                                          operator, Arrays.toString(Operator.values())));
            }
        }
    }
}
