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


package com.hortonworks.streamline.streams.runtime.rule.condition.expression;

import com.google.common.base.Joiner;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Condition;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Expression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.ExpressionTranslator;
import com.hortonworks.streamline.streams.layout.component.rule.expression.FunctionExpression;
import com.hortonworks.streamline.streams.layout.component.rule.expression.GroupBy;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Having;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Operator;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Projection;
import org.apache.commons.lang.StringUtils;
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
    private static final String RULE_TABLE = "RULETABLE";
    private static final String CREATE_EXTERNAL_TABLE = "CREATE EXTERNAL TABLE ";
    private static final String CREATE_FUNCTION = "CREATE FUNCTION ";
    private static final String SELECT_STREAM = "SELECT STREAM ";
    private static final String FROM = "FROM ";
    private static final String WHERE = "WHERE ";
    private static final String GROUP_BY = "GROUP BY ";
    private static final String HAVING = "HAVING ";
    private static final String LOCATION = "LOCATION";
    private static final String AS = "AS";
    private static final String QUOTE = "\"";
    private final LinkedHashSet<Schema.Field> stormSqlFields = new LinkedHashSet<>();
    private final List<Schema.Field> groupByFields = new ArrayList<>();
    private final LinkedHashSet<FunctionExpression.Function> functions = new LinkedHashSet<>();
    private final LinkedHashSet<FunctionExpression.Function> aggregateFunctions = new LinkedHashSet<>();
    private final List<String> projectedFields = new ArrayList<>();
    private final List<String> outputFields = new ArrayList<>();

    public StormSqlExpression(Condition condition) {
        this(condition, null);
    }

    public StormSqlExpression(Condition condition, Projection projection) {
        this(condition, projection, null, null);
    }

    public StormSqlExpression(Condition condition, Projection projection, GroupBy groupBy, Having having) {
        super(condition, projection, groupBy, having);
        handleProjection();
        handleFilter();
        handleGroupByHaving();
    }

    private void handleProjection() {
        if (projection != null) {
            for (Expression expr : projection.getExpressions()) {
                ExpressionTranslator translator = new StormSqlExpressionTranslator();
                expr.accept(translator);
                stormSqlFields.addAll(translator.getFields());
                functions.addAll(translator.getFunctions());
                aggregateFunctions.addAll(translator.getAggregateFunctions());
                projectedFields.add(translator.getTranslatedExpression());
                if (!translator.getAliases().isEmpty()) {
                    outputFields.add(translator.getAliases().get(0));
                } else {
                    outputFields.add(translator.getUnquotedTranslatedExpression());
                }
            }
        }
    }

    private void handleFilter() {
        if (condition != null) {
            ExpressionTranslator conditionTranslator = new StormSqlExpressionTranslator();
            condition.getExpression().accept(conditionTranslator);
            stormSqlFields.addAll(conditionTranslator.getFields());
            if (!conditionTranslator.getAggregateFunctions().isEmpty()) {
                throw new IllegalArgumentException("Cannot have aggregate functions filter condition.");
            }
            functions.addAll(conditionTranslator.getFunctions());
            expression = conditionTranslator.getTranslatedExpression();
            LOG.debug("Built expression [{}] for filter condition [{}]", expression, condition);
        }
    }

    private void handleGroupByHaving() {
        if (groupBy != null) {
            List<String> groupByExpressions = new ArrayList<>();
            for (Expression expr: groupBy.getExpressions()) {
                ExpressionTranslator groupByTranslator = new StormSqlExpressionTranslator();
                expr.accept(groupByTranslator);
                stormSqlFields.addAll(groupByTranslator.getFields());
                groupByFields.addAll(groupByTranslator.getFields());
                functions.addAll(groupByTranslator.getFunctions());
                groupByExpressions.add(groupByTranslator.getTranslatedExpression());
            }
            groupByExpression = Joiner.on(",").join(groupByExpressions);
            if (having != null) {
                ExpressionTranslator havingTranslator = new StormSqlExpressionTranslator();
                having.getExpression().accept(havingTranslator);
                stormSqlFields.addAll(havingTranslator.getFields());
                functions.addAll(havingTranslator.getFunctions());
                aggregateFunctions.addAll(havingTranslator.getAggregateFunctions());
                havingExpression = havingTranslator.getTranslatedExpression();
                LOG.debug("Built expression [{}] for having [{}]", havingExpression, having);
            }
        }
    }

    @Override
    public String asString() {
        return expression;
    }

    /*
     * e.g. [CREATE FUNCTION TEST_FN AS 'com.hortonworks.streamline.builtin.TestFn']
     */
    public List<String> createFunctions() {
        List<String> result = new ArrayList<>();
        result.addAll(doCreateFunctions(functions));
        result.addAll(doCreateFunctions(aggregateFunctions));
        return result;
    }

    private List<String> doCreateFunctions(Set<FunctionExpression.Function> functions) {
        List<String> result = new ArrayList<>();
        for (FunctionExpression.Function fn : functions) {
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
    public String createTable(String schemaName) {
        return CREATE_EXTERNAL_TABLE + RULE_TABLE + " (" + buildCreateDefinition() + ") " +
                LOCATION + " '" + schemaName + ":///" + RULE_TABLE + "'";
    }

    // "SELECT F1, F2, F3 FROM RT WHERE F1 < 2 AND F2 < 3 AND F3 < 4"
    public String select() {
        StringBuilder select = new StringBuilder(SELECT_STREAM);
        select.append(buildSelectExpression()).append(" ")
                .append(FROM).append(RULE_TABLE).append(" ");
        if (!StringUtils.isEmpty(expression)) {
            select.append(WHERE).append(asString());
        }
        if (groupBy != null) {
            select.append(" ").append(GROUP_BY).append(groupByExpression);
            if (having != null) {
                select.append(" ");
                select.append(HAVING).append(havingExpression);
            }
        }
        return select.toString();
    }

    // F1 INTEGER or F2 STRING or ...
    private String buildCreateDefinition() {
        final StringBuilder builder = new StringBuilder("");
        int count = 0;
        for (Schema.Field field : stormSqlFields) {
            String fieldName = field.getName();
            if (++count > 1) {
                builder.append(", ");
            }
            builder.append(QUOTE).append(fieldName).append(QUOTE).append(" ")
                    .append(getType(field));
            if (!groupByFields.isEmpty() && groupByFields.get(0).equals(field)) {
                /* for monotonicity of group by field, make the first group by field a "primary key"
                 * TODO: see if an option other than PK can be used for monotonicity
                 */
                builder.append(" ").append("PRIMARY KEY");
            }
        }
        return builder.toString();
    }

    private String buildSelectExpression() {
        String result;
        if (projection != null) {
            result = Joiner.on(", ").join(projectedFields);
        } else {
            List<String> fields = new ArrayList<>();
            for (Schema.Field field : stormSqlFields) {
                fields.add(QUOTE + field.getName() + QUOTE);
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
                case MULTIPLY:
                    return " * ";
                case DIVIDE:
                    return " / ";
                case PLUS:
                    return " + ";
                case MINUS:
                    return " - ";
                default:
                    throw new UnsupportedOperationException(
                            String.format("Operator [%s] not supported. List of supported operators: %s",
                                          operator, Arrays.toString(Operator.values())));
            }
        }

        @Override
        protected String getQuote() {
            return QUOTE;
        }

        @Override
        protected String getTable() {
            return RULE_TABLE;
        }
    }

    public List<Schema.Field> getStormSqlFields() {
        return new ArrayList<>(stormSqlFields);
    }

    public void addStormSqlField(Schema.Field field) {
        stormSqlFields.add(field);
    }
    public List<String> getProjectedFields() {
        return projectedFields;
    }

    public List<String> getOutputFields() {
        return outputFields;
    }

    @Override
    protected String getType(Schema.Field field) {
        switch (field.getType()) {
            case NESTED:
            case ARRAY:
                return "ANY";
            case STRING:
                return "VARCHAR";
            case LONG:
                return "BIGINT";
            default:
                return super.getType(field);
        }
    }

}
