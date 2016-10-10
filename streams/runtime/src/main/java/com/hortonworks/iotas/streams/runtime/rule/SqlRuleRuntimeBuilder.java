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

package com.hortonworks.iotas.streams.runtime.rule;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.streams.layout.component.rule.Rule;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Expression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.FieldExpression;
import com.hortonworks.iotas.streams.layout.component.rule.expression.GroupBy;
import com.hortonworks.iotas.streams.runtime.rule.condition.expression.StormSqlExpression;
import com.hortonworks.iotas.streams.runtime.rule.sql.RulesDataSourcesProvider;
import com.hortonworks.iotas.streams.runtime.rule.sql.SqlEngine;

import static com.hortonworks.iotas.streams.runtime.rule.condition.expression.StormSqlExpression.RULE_SCHEMA;
import static com.hortonworks.iotas.streams.runtime.rule.condition.expression.StormSqlExpression.RULE_TABLE;
import static com.hortonworks.iotas.streams.runtime.rule.sql.SqlScript.ValuesToIotasEventConverter;

import com.hortonworks.iotas.streams.runtime.rule.sql.SqlScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.hortonworks.iotas.streams.layout.component.rule.expression.Window.WINDOW_ID;

public class SqlRuleRuntimeBuilder extends AbstractRuleRuntimeBuilder {
    protected static final Logger LOG = LoggerFactory.getLogger(SqlRuleRuntimeBuilder.class);

    private Rule rule;
    private StormSqlExpression stormSqlExpression;
    private SqlEngine sqlEngine;
    private SqlScript sqlScript;
    private static final GroupBy GROUP_BY_WINDOWID = new GroupBy(new FieldExpression(Schema.Field.of(WINDOW_ID, Schema.Type.LONG)));
    @Override
    public void setRule(Rule rule) {
        this.rule = rule;
    }

    @Override
    public void buildExpression() {
        List<Expression> groupByExpressions = new ArrayList<>();
        if (rule.getWindow() != null) {
            groupByExpressions.addAll(GROUP_BY_WINDOWID.getExpressions());
        }
        if (rule.getGroupBy() != null) {
            groupByExpressions.addAll(rule.getGroupBy().getExpressions());
        }
        stormSqlExpression = new StormSqlExpression(rule.getCondition(),
                                                    rule.getProjection(),
                                                    groupByExpressions.isEmpty() ? null : new GroupBy(groupByExpressions),
                                                    groupByExpressions.isEmpty() ? null : rule.getHaving());
        LOG.info("Built stormSqlExpression {}", stormSqlExpression);
    }

    @Override
    public void buildScriptEngine() {
        sqlEngine = new SqlEngine();
        List<Schema.Field> fields = stormSqlExpression.getStormSqlFields();
        if (!fields.isEmpty()) {
            sqlEngine.compileQuery(createQuery(stormSqlExpression));
        }
        LOG.info("Built sqlEngine {}", sqlEngine);
    }

    private List<String> createQuery(StormSqlExpression expression) {
        final List<String> statements = new ArrayList<>(2);
        statements.add(expression.createTable(RULE_SCHEMA, RULE_TABLE));
        statements.addAll(expression.createFunctions());
        statements.add(expression.select(RULE_TABLE));
        return statements;
    }

    @Override
    public void buildScript() {
        sqlScript = new SqlScript(stormSqlExpression, sqlEngine);
        LOG.info("Built SqlScript {}", sqlScript);
        ValuesToIotasEventConverter valuesConverter = new ValuesToIotasEventConverter(sqlScript.getOutputFields());
        sqlScript.setValuesConverter(valuesConverter);
        LOG.info("valuesConverter {}", valuesConverter);
    }

    @Override
    protected Rule getRule() {
        return rule;
    }

    @Override
    public RuleRuntime buildRuleRuntime() {
        return new RuleRuntime(rule, sqlScript, actions);
    }

    @Override
    public String toString() {
        return "SqlRuleRuntimeBuilder{" +
                ", sqlEngine=" + sqlEngine +
                ", sqlScript=" + sqlScript +
                '}';
    }
}
