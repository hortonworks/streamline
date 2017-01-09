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

package com.hortonworks.streamline.streams.layout.component.impl;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;
import com.hortonworks.streamline.streams.layout.component.rule.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a design time rules processor.
 */
public class RulesProcessor extends StreamlineProcessor {     //TODO: Rename to RuleProcessor
    private static final Logger log = LoggerFactory.getLogger(RulesProcessor.class);

    public static final String CONFIG_KEY_RULES = "rules";
    public static final String CONFIG_PROCESS_ALL = "processAll";
    private List<Rule> rules;
    private boolean processAll = true;

    public RulesProcessor() {
    }

    public RulesProcessor(RulesProcessor other) {
        super(other);
        this.rules = new ArrayList<>(other.getRules());
        this.processAll = other.processAll;
    }

    public boolean getProcessAll() {
        return processAll;
    }

    public void setProcessAll(boolean processAll) {
        this.processAll = processAll;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;

        Set<String> streamIds = new HashSet<>(Collections2.transform(getOutputStreams(), new Function<Stream, String>() {
            @Override
            public String apply(Stream input) {
                return input.getId();
            }
        }));

        for (Rule rule : rules) {
            for (Action action : rule.getActions()) {
                for (String stream : action.getOutputStreams()) {
                    if(!streamIds.contains(stream)) {
                        String errorMsg = String.format("Action [%s] stream [%s] does not exist in processor [%s]'s output streams [%s]", action, stream, getName(), streamIds);
                        log.error(errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "RulesProcessor{" +
                "rules=" + rules +
                '}';
    }

    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}
