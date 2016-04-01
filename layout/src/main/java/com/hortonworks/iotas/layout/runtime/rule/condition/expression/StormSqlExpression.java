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

import com.google.common.base.Joiner;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.rule.condition.Condition;
import com.hortonworks.iotas.layout.design.rule.condition.Expression;
import com.hortonworks.iotas.layout.design.rule.condition.ExpressionTranslator;
import com.hortonworks.iotas.layout.design.rule.condition.FunctionExpression;
import com.hortonworks.iotas.layout.design.rule.condition.Operator;
import com.hortonworks.iotas.layout.design.rule.condition.Projection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the expression of this {@link Condition} in Storm SQL language syntax
 **/
public class StormSqlExpression extends ExpressionRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(StormSqlExpression.class);
    public static final String RULE_SCHEMA = "RULESCHEMA";  // _ underscores not supported by Storm SQL framework
    public static final String RULE_TABLE = "RULETABLE";
    private static final String CREATE_EXTERNAL_TABLE = "CREATE EXTERNAL TABLE ";
    private static final String CREATE_FUNCTION = "CREATE FUNCTION ";
    private static final String SELECT_STREAM = "SELECT STREAM ";
    private static final String FROM = "FROM ";
    private static final String WHERE = "WHERE ";
    private static final String LOCATION = "LOCATION";
    private static final String AS = "AS";
    private final LinkedHashSet<Schema.Field> fieldsToEmit = new LinkedHashSet<>();
    private final LinkedHashSet<FunctionExpression.Function> functions = new LinkedHashSet<>();
    private final List<String> projectedFields = new ArrayList<>();

    public StormSqlExpression(Condition condition) {
        this(condition, null);
    }

    public StormSqlExpression(Condition condition, Projection projection) {
        super(condition, projection);
        if (projection != null) {
            for (Expression expr : projection.getExpressions()) {
                ExpressionTranslator translator = new StormSqlExpressionTranslator();
                expr.accept(translator);
                fieldsToEmit.addAll(translator.getFields());
                functions.addAll(translator.getFunctions());
                projectedFields.add(translator.getTranslatedExpression());
            }
        }
        ExpressionTranslator conditionTranslator = new StormSqlExpressionTranslator();
        condition.getExpression().accept(conditionTranslator);
        fieldsToEmit.addAll(conditionTranslator.getFields());
        functions.addAll(conditionTranslator.getFunctions());
        expression = conditionTranslator.getTranslatedExpression();
        LOG.debug("Built expression [{}] for condition [{}]", expression, condition);
    }


    @Override
    public String asString() {
        return expression;
    }

    /*
     * e.g. [CREATE FUNCTION TEST_FN AS 'com.hortonworks.iotas.builtin.TestFn']
     */
    public List<String> createFunctions() {
        List<String> result = new ArrayList<>();
        for(FunctionExpression.Function fn: functions) {
            if (fn.isUdf()) {
                result.add(CREATE_FUNCTION + fn.getName() + " " + AS + " " + "'" + fn.getClassName() + "'");
            }
        }
        return result;
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
        return SELECT_STREAM + buildSelectExpression() + " " + FROM + tableName + " " + WHERE + asString();
    }

    // F1 INTEGER or F2 STRING or ...
    private String buildCreateDefinition() {
        final StringBuilder builder = new StringBuilder("");
        int count = 0;
        for (Schema.Field field : fieldsToEmit) {
            String fieldName = field.getName();
            if (++count > 1) {
                builder.append(", ");
            }
            builder.append(fieldName).append(" ")
                    .append(getType(field));
        }
        return builder.toString();
    }

    private String buildSelectExpression() {
        String result;
        if (projection != null) {
            result = Joiner.on(", ").join(projectedFields);
        } else {
            List<String> fields = new ArrayList<>();
            for(Schema.Field field: fieldsToEmit) {
                fields.add(field.getName());
            }
            result = Joiner.on(", ").join(fields);
        }
        return result;
    }

    private static class StormSqlExpressionTranslator extends ExpressionTranslator {
        protected String getOperator(Operator operator) {
            switch (operator) {
                case AND:
                    return " AND ";
                case OR:
                    return " OR ";
                case EQUALS:
                    return " = ";
                case NOT_EQUAL:
                    return " <> ";
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

    public List<Schema.Field> getFieldsToEmit() {
        return new ArrayList<>(fieldsToEmit);
    }

    public List<String> getProjectedFields() {
        return projectedFields;
    }

    @Override
    protected String getType(Schema.Field field) {
        switch (field.getType()) {
            case NESTED:
            case ARRAY:
                return "ANY";
            case STRING:
                return "VARCHAR";
            default:
                return super.getType(field);
        }
    }

}
