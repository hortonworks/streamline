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

import com.hortonworks.iotas.layout.design.rule.condition.Condition;

import java.util.Arrays;

public class SqlStreamExpression extends Expression {

    public SqlStreamExpression(Condition condition) {
        super(condition);
    }

    @Override
    public String getExpression() {
        final StringBuilder builder = new StringBuilder("");
        for (Condition.ConditionElement element : condition.getConditionElements()) {
            builder.append(getType(element.getFirstOperand()))              // Integer
                    .append(getName(element.getFirstOperand()))             // x
                    .append(getOperation(element.getOperation()))           // =, !=, >, <, ...
                    .append(element.getSecondOperand());                    // 5 - it is a constant

            if (element.getLogicalOperator() != null) {
                builder.append(" ");
                builder.append(getLogicalOperator(element.getLogicalOperator()));   // AND or OR
                builder.append(" ");
            }
        }
        final String expression = builder.toString();                              // Integer x = 5 [AND or OR]
        log.debug("Built expression [{}] for condition [{}]", expression, condition);
        return expression;
    }


    private String getLogicalOperator(Condition.ConditionElement.LogicalOperator logicalOperator) {
        switch(logicalOperator) {
            case AND:
                return " AND ";
            case OR:
                return " OR ";
            default:
                throw new UnsupportedOperationException(String.format("Operator [%s] not supported. List of supported operators: %s",
                        logicalOperator, Arrays.toString(Condition.ConditionElement.LogicalOperator.values())));
        }
    }

    private String getOperation(Condition.ConditionElement.Operation operation) {
        switch(operation) {
            case EQUALS:
                return " = ";
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
                throw new UnsupportedOperationException(String.format("Operation [%s] not supported. List of supported operations: %s",
                        operation, Arrays.toString(Condition.ConditionElement.Operation.values())));
        }
    }
}
