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
import com.hortonworks.iotas.layout.runtime.rule.condition.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.Serializable;

/**
 * Represents a rule runtime
 * @param <I> Type of runtime input to this rule, for example {@code Tuple}
 * @param <E> Type of object required to execute this rule in the underlying streaming framework e.g {@code IOutputCollector}
 */
public abstract class RuleRuntime<I, E> implements Serializable {
    protected static final Logger log = LoggerFactory.getLogger(RuleRuntime.class);

    protected final Rule rule;
    protected final Script<IotasEvent, ?> script;     // Script used to evaluate the condition

    RuleRuntime(Rule rule, Script<IotasEvent, ?> script) {
        this.rule = rule;
        this.script = script;
    }

    public boolean evaluate(IotasEvent input) {
        try {
            boolean evaluates = script.evaluate(input);
            log.debug("Rule condition evaluated to [{}].\n\t[{}]\n\tInput[{}]", evaluates, rule, input);
            return evaluates;
        } catch (ScriptException e) {
            throw new ConditionEvaluationException("Exception occurred when evaluating rule condition. " + this, e);
        }
    }

    /**
     * Executes a {@link Rule}'s Action
     * @param input runtime input to this rule, for example, {@code Tuple} for {@code Storm}
     * @param executor object required to execute this rule's action in the underlying streaming framework e.g {@code OutputCollector} for {@code Storm}
     */
    public abstract void execute(I input, E executor);

    @Override
    public String toString() {
        return "RuleRuntime{" + rule + ", " + script + '}';
    }
}
