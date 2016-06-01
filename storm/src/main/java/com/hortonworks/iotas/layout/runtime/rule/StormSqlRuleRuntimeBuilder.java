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

package com.hortonworks.iotas.layout.runtime.rule;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.design.rule.condition.Expression;
import com.hortonworks.iotas.layout.design.rule.condition.FieldExpression;
import com.hortonworks.iotas.layout.design.rule.condition.GroupBy;
import com.hortonworks.iotas.layout.runtime.rule.condition.expression.StormSqlExpression;
import com.hortonworks.iotas.layout.runtime.rule.sql.StormSqlEngine;
import com.hortonworks.iotas.layout.runtime.rule.sql.StormSqlScript;

import java.util.ArrayList;
import java.util.List;

import static com.hortonworks.iotas.layout.design.rule.condition.Window.WINDOW_ID;

public class StormSqlRuleRuntimeBuilder extends AbstractRuleRuntimeBuilder {
    private Rule rule;
    private StormSqlExpression stormSqlExpression;
    private StormSqlEngine stormSqlEngine;
    private StormSqlScript<IotasEvent> stormSqlScript;
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
    }

    @Override
    public void buildScriptEngine() {
        stormSqlEngine = new StormSqlEngine();
    }

    @Override
    public void buildScript() {
        stormSqlScript = new StormSqlScript<>(stormSqlExpression, stormSqlEngine);
        stormSqlScript.setValuesConverter(new StormSqlScript.ValuesToIotasEventConverter(stormSqlScript.getProjectedFields()));
    }

    @Override
    protected Rule getRule() {
        return rule;
    }

    @Override
    public RuleRuntime buildRuleRuntime() {
        return new RuleRuntime(rule, stormSqlScript, actions);
    }

    @Override
    public String toString() {
        return "StormSqlRuleRuntimeBuilder{" +
                ", stormSqlEngine=" + stormSqlEngine +
                ", stormSqlScript=" + stormSqlScript +
                '}';
    }
}
