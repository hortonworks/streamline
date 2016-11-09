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

package org.apache.streamline.streams.runtime.processor;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.Result;
import org.apache.streamline.streams.exception.ProcessingException;
import org.apache.streamline.streams.layout.component.Stream;
import org.apache.streamline.streams.layout.component.impl.RulesProcessor;
import org.apache.streamline.streams.runtime.ProcessorRuntime;
import org.apache.streamline.streams.runtime.rule.RuleRuntime;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.streamline.streams.common.StreamlineEventImpl.GROUP_BY_TRIGGER_EVENT;

/**
 * Represents a runtime rules processor
 */
public class RuleProcessorRuntime implements Serializable, ProcessorRuntime {
    protected static final Logger LOG = LoggerFactory.getLogger(RuleProcessorRuntime.class);

    protected final RulesProcessor rulesProcessor;
    protected final List<RuleRuntime> rulesRuntime;
    private Map<String, List<RuleRuntime>> streamToRuleRuntimes;
    private List<RuleRuntime> allRuleRuntimes;
    private boolean processAll = true;

    public RuleProcessorRuntime(RuleProcessorRuntimeDependenciesBuilder builder) {
        this.rulesProcessor = builder.getRulesProcessor();
        this.rulesRuntime = builder.getRulesRuntime();
        buildStreamToRulesRuntime();
        this.processAll = rulesProcessor.getProcessAll();
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
        ImmutableSet.Builder<RuleRuntime> builder = ImmutableSet.builder();
        for(List<RuleRuntime> ruleRuntimes: streamToRuleRuntimes.values()) {
            builder.addAll(ruleRuntimes);
        }
        allRuleRuntimes = builder.build().asList();

    }

    public RulesProcessor getRulesProcessor() {
        return rulesProcessor;
    }

    public List<RuleRuntime> getRulesRuntime() {
        return Collections.unmodifiableList(rulesRuntime);
    }

    public List<String> getStreams() {
        Collection<String> streams = Collections2.transform(rulesProcessor.getOutputStreams(), new Function<Stream, String>(){
            @Override
            public String apply(Stream input) {
                return input.getId();
            }
        });
        LOG.info("Rule processor output streams {}", streams);
        return ImmutableList.copyOf(streams);
    }

    @Override
    public String toString() {
        return "RuleProcessorRuntime{" + rulesProcessor + ", " + rulesRuntime + '}';
    }

    @Override
    public List<Result> process(StreamlineEvent event) throws ProcessingException {
        List<Result> results = new ArrayList<>();
        try {
            List<RuleRuntime> ruleRuntimes = getRulesRuntime(event);
            LOG.debug("Process event {}, rule runtimes {}", event, ruleRuntimes);
            for (RuleRuntime rr : ruleRuntimes) {
                boolean succeeded = false;
                for (StreamlineEvent result : rr.evaluate(event)) {
                    if (result != null) {
                        results.addAll(rr.process(result));
                        succeeded = true;
                    }
                }
                if(!processAll && succeeded)
                    break;
            }
        } catch (Exception e) {
            String message = String.format("Error evaluating rule processor with id: %s, error: %s",
                    rulesProcessor.getId(), e.getMessage());
            LOG.error(message, e);
            throw new ProcessingException(message, e);
        }
        return results;
    }

    private List<RuleRuntime> getRulesRuntime(StreamlineEvent event) throws ProcessingException {
        if (event == GROUP_BY_TRIGGER_EVENT) {
            return allRuleRuntimes;
        }
        String inputStream = event.getSourceStream();
        if (StringUtils.isEmpty(inputStream)) {
            throw new ProcessingException("Event SourceStream is empty");
        }
        List<RuleRuntime> result = streamToRuleRuntimes.get(inputStream);
        if (result == null) {
            LOG.debug("Could not find matching rules for input stream {}. Will not process event.", inputStream);
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        for (RuleRuntime ruleRuntime : rulesRuntime) {
            ruleRuntime.initialize(config);
        }
    }

    @Override
    public void cleanup() {

    }
}

