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
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.runtime.rule.condition.expression.StormSqlExpression;
import com.hortonworks.iotas.layout.runtime.rule.sql.StormSqlEngine;
import com.hortonworks.iotas.layout.runtime.rule.sql.StormSqlScript;

public class StormSqlRuleRuntimeBuilder extends AbstractRuleRuntimeBuilder {
    private Rule rule;
    private StormSqlExpression stormSqlExpression;
    private StormSqlEngine stormSqlEngine;
    private StormSqlScript<IotasEvent> stormSqlScript;

    @Override
    public void setRule(Rule rule) {
        this.rule = rule;
    }

    @Override
    public void buildExpression() {
        stormSqlExpression = new StormSqlExpression(rule.getCondition(), rule.getProjection());
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
