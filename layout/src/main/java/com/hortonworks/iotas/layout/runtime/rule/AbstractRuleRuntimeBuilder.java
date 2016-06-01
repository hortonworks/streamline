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

package com.hortonworks.iotas.layout.runtime.rule;

import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.design.rule.action.Action;
import com.hortonworks.iotas.layout.runtime.rule.action.ActionRuntime;
import com.hortonworks.iotas.layout.runtime.rule.action.ActionRuntimeContext;
import com.hortonworks.iotas.layout.runtime.transform.ActionRuntimeService;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRuleRuntimeBuilder implements RuleRuntimeBuilder {
    protected List<ActionRuntime> actions;

    @Override
    public void buildActions() {
        List<ActionRuntime> runtimeActions = new ArrayList<>();
        Rule rule = getRule();
        for (Action action : rule.getActions()) {
            final ActionRuntime actionRuntime = ActionRuntimeService.get().get(action);
            actionRuntime.setActionRuntimeContext(new ActionRuntimeContext(getRule(), action));
            runtimeActions.add(actionRuntime);
        }
        actions = runtimeActions;
    }

    protected abstract Rule getRule();
}
