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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.layout.runtime.rule.action;

import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.design.rule.action.Action;

/**
 * This class contains the context information for an Action. This is passed in {@link ActionRuntime#setActionRuntimeContext(ActionRuntimeContext)}
 */
public class ActionRuntimeContext {
    private final Rule rule;
    private final Action action;

    public ActionRuntimeContext(Rule rule, Action action) {
        this.rule = rule;
        this.action = action;
    }

    /**
     * @return Rule for which the current Action is executed.
     */
    public Rule getRule() {
        return rule;
    }

    /**
     * @return Current Action which is getting executed
     */
    public Action getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "ActionRuntimeContext{" +
                "rule=" + rule +
                ", action=" + action +
                '}';
    }
}
