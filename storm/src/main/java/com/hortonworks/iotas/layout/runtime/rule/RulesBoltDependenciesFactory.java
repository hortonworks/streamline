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

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import com.hortonworks.iotas.layout.design.component.RulesProcessorBuilder;
import com.hortonworks.iotas.layout.runtime.processor.RuleProcessorRuntimeDependenciesBuilder;
import com.hortonworks.iotas.layout.runtime.processor.RuleProcessorRuntimeStorm;

import java.io.Serializable;
import java.util.List;

public class RulesBoltDependenciesFactory implements Serializable {
    public enum ScriptType {GROOVY, SQL}

    private RulesProcessorBuilder rulesProcessorBuilder;
    private ScriptType scriptType;

    public RulesBoltDependenciesFactory(RulesProcessorBuilder rulesProcessorBuilder, ScriptType scriptType) {
        this.rulesProcessorBuilder = rulesProcessorBuilder;
        this.scriptType = scriptType;
    }

    public List<RuleRuntimeStormDeclaredOutput> createDeclaredOutputs() {
        return createRuleProcessorRuntimeStorm().getDeclaredOutputs();
    }

    public RuleProcessorRuntimeStorm createRuleProcessorRuntimeStorm() {
        final RuleRuntimeBuilder<Tuple, OutputCollector> ruleRuntimeBuilder = createRuleRuntimeBuilder();
        RuleProcessorRuntimeDependenciesBuilder<Tuple, OutputCollector> dependenciesBuilder =
                new RuleProcessorRuntimeDependenciesBuilder<>(rulesProcessorBuilder, ruleRuntimeBuilder);
        return new RuleProcessorRuntimeStorm(dependenciesBuilder);
    }

    private RuleRuntimeBuilder<Tuple, OutputCollector> createRuleRuntimeBuilder() {
        switch(scriptType) {
            case GROOVY:
                return new GroovyRuleRuntimeBuilder();
            case SQL:
                return new StormSqlRuleRuntimeBuilder();
            default:
                throw new RuntimeException("Unsupported RuleRuntimeBuilder");
        }
    }
}
