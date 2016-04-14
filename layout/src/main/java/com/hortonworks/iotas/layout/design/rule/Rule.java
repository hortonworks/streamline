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

package com.hortonworks.iotas.layout.design.rule;

import com.google.common.collect.ImmutableList;
import com.hortonworks.iotas.layout.design.rule.action.Action;
import com.hortonworks.iotas.layout.design.rule.condition.Condition;

import java.io.Serializable;
import java.util.List;

/**
 *
 * A rule as represented in the UI layout
 */
public class Rule implements Serializable {
    private Long id;
    private String name;
    private String description;
    private String ruleProcessorName;

    private Condition condition;
    private List<Action> actions;

    public Rule() {     //TODO Check
        // For JSON serializer
    }

    // ====== Metadata =======

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRuleProcessorName() {
        return ruleProcessorName;
    }

    public void setRuleProcessorName(String ruleProcessorName) {
        this.ruleProcessorName = ruleProcessorName;
    }

    // ====== Design time =======

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = ImmutableList.copyOf(actions);
    }

    public String getOutputStreamNameForAction(Action action) {
        return ruleProcessorName + "." + name + "." + id + "." + action.getName();
    }

    @Override
    public String toString() {
        return "Rule{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", ruleProcessorName='" + ruleProcessorName + '\'' +
                ", condition=" + condition +
                ", actions=" + actions +
                '}';
    }
}

