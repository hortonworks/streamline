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
import com.hortonworks.iotas.layout.design.rule.exception.ConditionEvaluationException;
import com.hortonworks.iotas.layout.runtime.ActionRuntime;
import com.hortonworks.iotas.layout.runtime.script.Script;
import com.hortonworks.iotas.common.errors.ProcessingException;
import com.hortonworks.iotas.processor.ProcessorRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hortonworks.iotas.common.Result;

/**
 * Represents a rule runtime
 */
public class RuleRuntime implements Serializable, ProcessorRuntime {
    protected static final Logger LOG = LoggerFactory.getLogger(RuleRuntime.class);

    protected final Rule rule;
    protected final Script<IotasEvent, IotasEvent, ?> script;     // Script used to evaluate the condition
    protected final List<ActionRuntime> actions;

    RuleRuntime(Rule rule, Script<IotasEvent, IotasEvent, ?> script, List<ActionRuntime> actions) {
        this.rule = rule;
        this.script = script;
        this.actions = actions;
    }

    public IotasEvent evaluate(IotasEvent input) {
        try {
            return script.evaluate(input);
        } catch (ScriptException e) {
            throw new ConditionEvaluationException("Exception occurred when evaluating rule condition. " + this, e);
        }
    }

    /**
     * Executes a {@link Rule}'s Action
     *
     * @param input runtime input to this rule
     */
    @Override
    public List<Result> process (IotasEvent input) throws ProcessingException {
        LOG.debug("process invoked with IotasEvent {}", input);
        List<Result> results = new ArrayList<>();
        try {
            for (ActionRuntime action : actions) {
                Result result = action.execute(input);
                LOG.debug("Applied action {}, Result {}", action, result);
                results.add(result);
            }
        } catch (Exception e) {
            String message = "Error evaluating rule with id:" + rule.getId();
            LOG.error(message);
            throw new ProcessingException(message, e);
        }
        LOG.debug("Returning results {}", results);
        return results;
    }

    @Override
    public void initialize(Map<String, Object> config) {

    }

    @Override
    public void cleanup() {

    }

    public List<String> getStreams() {
        LOG.debug("in getStreams");
        List<String> streams = new ArrayList<>();
        for(ActionRuntime action: actions) {
            LOG.debug("Action {}, Stream {}", action, action.getStream());
            streams.add(action.getStream());
        }
        LOG.debug("Returning streams {}", streams);
        return streams;
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
