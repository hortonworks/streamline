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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Represents the expression of this {@link Condition} in Storm SQL language syntax
 **/
public class StormSqlExpression extends Expression {
    public static final String RULE_SCHEMA = "RULESCHEMA";  // _ underscores not supported by Storm SQL framework
    public static final String RULE_TABLE = "RULETABLE";
    private static final String CREATE_EXTERNAL_TABLE = "CREATE EXTERNAL TABLE ";
    private static final String SELECT_STREAM = "SELECT STREAM ";
    private static final String FROM = "FROM ";
    private static final String WHERE = "WHERE ";
    private static final String LOCATION = "LOCATION";

    private static final Logger LOG = LoggerFactory.getLogger(StormSqlExpression.class);

    public StormSqlExpression(Condition condition) {
        super(condition);
    }

    @Override
    public String asString() {
        final StringBuilder builder = new StringBuilder("");
        for (Condition.ConditionElement element : condition.getConditionElements()) {
            builder.append(getName(element.getFirstOperand()))          // x
                .append(getOperation(element.getOperation()))           // =, !=, >, <, ...
                .append(element.getSecondOperand());                    // 5 - it is a constant

            if (element.getLogicalOperator() != null) {
                builder.append(" ");
                builder.append(getLogicalOperator(element.getLogicalOperator()));   // AND or OR
                builder.append(" ");
            }
        }
        final String expression = builder.toString();                              // x = 5 [AND or OR]
        LOG.debug("Built expression [{}] for condition [{}]", expression, condition);
        return expression;
    }

    /*
    "CREATE EXTERNAL TABLE RT (F1 INTEGER, F2 INTEGER, F3 INTEGER) LOCATION 'RTS:///RT'"
     RTS - Rules Table Schema
     RT - Rules Table
    */
    public String createTable(String schemaName, String tableName) {
        return CREATE_EXTERNAL_TABLE + tableName + " (" + buildCreateDefinition() + ") " +
                LOCATION + " '" + schemaName + ":///" + tableName +"'";
    }

    // "SELECT F1, F2, F3 FROM RT WHERE F1 < 2 AND F2 < 3 AND F3 < 4"
    public String select(String tableName) {
        return SELECT_STREAM + buildSelectExpression() + FROM + tableName + " " + WHERE + asString().toUpperCase();
    }

    // F1 INTEGER or F2 STRING or ...
    private String buildCreateDefinition() {
        final StringBuilder builder = new StringBuilder("");
        for (Condition.ConditionElement element : condition.getConditionElements()) {
            builder.append(getName(element.getFirstOperand()))
                    .append(getType(element.getFirstOperand())).append(", ");
        }
        if (builder.length() >= 2) {
            builder.setLength(builder.length() - 2);    // remove the last ", "
        }
        return builder.toString().toUpperCase();
    }

    private String buildSelectExpression() {
        final StringBuilder builder = new StringBuilder("");
        for (Condition.ConditionElement element : condition.getConditionElements()) {
            builder.append(getName(element.getFirstOperand())).append(", ");
        }
        if (builder.length() >= 2) {
            builder.setLength(builder.length() - 2);    // remove the last ", "
        }
        return builder.toString().toUpperCase();
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
