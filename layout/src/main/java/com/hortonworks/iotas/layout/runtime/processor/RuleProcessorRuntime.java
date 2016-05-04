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

import com.google.common.collect.ImmutableMap;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntime;
import com.hortonworks.iotas.common.errors.ProcessingException;
import com.hortonworks.iotas.processor.ProcessorRuntime;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a runtime rules processor
 */
public class RuleProcessorRuntime implements Serializable, ProcessorRuntime {
    protected static final Logger LOG = LoggerFactory.getLogger(RuleProcessorRuntime.class);

    protected RulesProcessor rulesProcessor;
    protected List<RuleRuntime> rulesRuntime;
    private Map<String, List<RuleRuntime>> streamToRuleRuntimes;

    public RuleProcessorRuntime(RuleProcessorRuntimeDependenciesBuilder builder) {
        this.rulesProcessor = builder.getRulesProcessor();
        this.rulesRuntime = builder.getRulesRuntime();
        buildStreamToRulesRuntime();
    }

    private void buildStreamToRulesRuntime() {
        Map<String, List<RuleRuntime>> map = new HashMap<>();
        for (RuleRuntime rr : rulesRuntime) {
            for (String streamId : rr.getRule().getStreams()) {
                List<RuleRuntime> ruleRuntimes = map.get(streamId);
                if (ruleRuntimes == null) {
                    ruleRuntimes = new ArrayList<>();
                    map.put(streamId, ruleRuntimes);
                }
                ruleRuntimes.add(rr);
            }
        }
        streamToRuleRuntimes = ImmutableMap.copyOf(map);
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
            List<RuleRuntime> ruleRuntimes = getRulesRuntime(iotasEvent.getSourceStream());
            LOG.debug("Process event {}, rule runtimes {}", iotasEvent, ruleRuntimes);
            for (RuleRuntime rr : ruleRuntimes) {
                if ((result = rr.evaluate(iotasEvent)) != null) {
                    results.addAll(rr.process(result));
                }
            }
        } catch (Exception e) {
            String message = String.format("Error evaluating rule processor with id: %s, error: %s",
                                           rulesProcessor.getId(), e.getMessage());
            LOG.error(message);
            throw new ProcessingException(message, e);
        }
        return results;
    }

    private List<RuleRuntime> getRulesRuntime(String inputStream) throws ProcessingException {
        if (StringUtils.isEmpty(inputStream)) {
            throw new ProcessingException("Event SourceStream is empty");
        }
        List<RuleRuntime> result = streamToRuleRuntimes.get(inputStream);
        if (result == null) {
            LOG.debug("Could not find matching rules for input stream {}. Will not process event.", inputStream);
            result = Collections.EMPTY_LIST;
        }
        return result;
    }

    @Override
    public void initialize(Map<String, Object> config) {

    }

    @Override
    public void cleanup() {

    }
}

