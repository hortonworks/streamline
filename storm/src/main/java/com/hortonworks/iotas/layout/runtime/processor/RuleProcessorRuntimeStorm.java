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

package com.hortonworks.iotas.layout.runtime.processor;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntime;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntimeStorm;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntimeStormDeclaredOutput;

import java.util.ArrayList;
import java.util.List;


/**
 *  Represents a runtime rules processor in the {@code Storm} streaming framework
 */
public class RuleProcessorRuntimeStorm extends RuleProcessorRuntime<Tuple, OutputCollector> {
    public RuleProcessorRuntimeStorm(RuleProcessorRuntimeDependenciesBuilder<Tuple, OutputCollector> builder) {
        super(builder);
    }

    public List<RuleRuntimeStormDeclaredOutput> getDeclaredOutputs() {
        final List<RuleRuntimeStormDeclaredOutput> declaredOutputs = new ArrayList<>();

        for (RuleRuntime<Tuple, OutputCollector> ruleRuntime : rulesRuntime) {
            final String streamId = ((RuleRuntimeStorm) ruleRuntime).getStreamId();
            final Fields fields = ((RuleRuntimeStorm) ruleRuntime).getFields();
            declaredOutputs.add(new RuleRuntimeStormDeclaredOutput(streamId, fields));
        }
        log.debug("Declaring output fields [{}]", declaredOutputs);
        return declaredOutputs;
    }
}

