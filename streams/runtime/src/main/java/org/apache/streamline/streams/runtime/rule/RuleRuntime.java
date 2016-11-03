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

package org.apache.streamline.streams.runtime.rule;

import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.Result;
import org.apache.streamline.streams.exception.ProcessingException;
import org.apache.streamline.streams.layout.component.rule.Rule;
import org.apache.streamline.streams.layout.component.rule.exception.ConditionEvaluationException;
import org.apache.streamline.streams.runtime.ProcessorRuntime;
import org.apache.streamline.streams.runtime.rule.action.ActionRuntime;
import org.apache.streamline.streams.runtime.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Represents a rule runtime
 */
public class RuleRuntime implements Serializable, ProcessorRuntime {
    protected static final Logger LOG = LoggerFactory.getLogger(RuleRuntime.class);

    protected final Rule rule;
    protected final Script<StreamlineEvent, Collection<StreamlineEvent>, ?> script;     // Script used to evaluate the condition
    protected final List<ActionRuntime> actions;

    public RuleRuntime(Rule rule, Script<StreamlineEvent, Collection<StreamlineEvent>, ?> script, List<ActionRuntime> actions) {
        this.rule = rule;
        this.script = script;
        this.actions = actions;
    }

    public Collection<StreamlineEvent> evaluate(StreamlineEvent input) {
        try {
            LOG.debug("Evaluate {} with script {}", input, script);
            return script.evaluate(input);
        } catch (ScriptException e) {
            throw new ConditionEvaluationException("Exception occurred when evaluating rule condition. " + this, e);
        }
    }

    /**
     * Executes a {@link Rule}'s Action
     *
     * @param event runtime input to this rule
     */
    @Override
    public List<Result> process (StreamlineEvent event) throws ProcessingException {
        LOG.debug("process invoked with StreamlineEvent {}", event);
        List<Result> allResults = new ArrayList<>();
        try {
            for (ActionRuntime action : actions) {
                List<Result> actionResults = action.execute(event);
                LOG.debug("Applied action {}, Result {}", action, actionResults);
                if(actionResults != null) {
                    allResults.addAll(actionResults);
                }
            }
        } catch (Exception e) {
            String message = "Error evaluating rule with id:" + rule.getId();
            LOG.error(message);
            throw new ProcessingException(message, e);
        }
        LOG.debug("Returning allResults {}", allResults);
        return allResults;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        for (ActionRuntime action : actions) {
            action.initialize(config);
        }
    }

    @Override
    public void cleanup() {

    }

    public Rule getRule() {
        return rule;
    }

    @Override
    public String toString() {
        return "RuleRuntime{" +
                "rule=" + rule +
                ", script=" + script +
                ", actions=" + actions +
                '}';
    }
}
