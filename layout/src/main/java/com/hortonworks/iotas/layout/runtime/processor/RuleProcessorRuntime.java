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

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntime;
import com.hortonworks.iotas.common.errors.ProcessingException;
import com.hortonworks.iotas.processor.ProcessorRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Represents a runtime rules processor
 */
public class RuleProcessorRuntime implements Serializable, ProcessorRuntime {
    protected static final Logger log = LoggerFactory.getLogger(RuleProcessorRuntime.class);

    protected RulesProcessor rulesProcessor;
    protected List<RuleRuntime> rulesRuntime;

    public RuleProcessorRuntime(RuleProcessorRuntimeDependenciesBuilder builder) {
        this.rulesProcessor = builder.getRulesProcessor();
        this.rulesRuntime = builder.getRulesRuntime();
    }

    public RulesProcessor getRulesProcessor() {
        return rulesProcessor;
    }

    public List<RuleRuntime> getRulesRuntime() {
        return Collections.unmodifiableList(rulesRuntime);
    }

    public List<String> getStreams() {
        List<String> streams = new ArrayList<>();
        for(RuleRuntime ruleRuntime: rulesRuntime) {
            streams.addAll(ruleRuntime.getStreams());
        }
        return streams;
    }

    @Override
    public String toString() {
        return "RuleProcessorRuntime{" + rulesProcessor + ", " + rulesRuntime + '}';
    }

    @Override
    public List<Result> process(IotasEvent iotasEvent) throws ProcessingException {
        List<Result> results = new ArrayList<>();
        try {
            IotasEvent result;
            for (RuleRuntime rule : rulesRuntime) {
                if ((result = rule.evaluate(iotasEvent)) != null) {
                    results.addAll(rule.process(result));
                }
            }
        } catch (Exception e) {
            String message = "Error evaluating rule processor with id:" + rulesProcessor.getId();
            log.error(message);
            throw new ProcessingException(message, e);
        }
        return results;
    }

    @Override
    public void initialize(Map<String, Object> config) {

    }

    @Override
    public void cleanup() {

    }
}

