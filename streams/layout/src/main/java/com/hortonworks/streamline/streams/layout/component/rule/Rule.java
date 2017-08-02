/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/


package com.hortonworks.streamline.streams.layout.component.rule;

import com.google.common.collect.ImmutableList;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.layout.component.rule.action.Action;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Condition;
import com.hortonworks.streamline.streams.layout.component.rule.expression.GroupBy;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Having;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Projection;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Udf;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Window;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * A rule as represented in the UI layout
 */
public class Rule implements Serializable {
    private static final Set<String> DEFAULT_STREAM = Collections.singleton(StreamlineEvent.DEFAULT_SOURCE_STREAM);
    private Long id;
    private String name;
    private String description;
    private String ruleProcessorName;
    private Set<String> streams;
    private Projection projection;
    private Condition condition;
    private GroupBy groupBy;
    private Having having;
    private Window window;
    private List<Action> actions;
    // quick access to user defined functions used in this rule
    private Set<Udf> referredUdfs = Collections.emptySet();

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

    public Projection getProjection() {
        return projection;
    }

    public void setProjection(Projection projection) {
        this.projection = projection;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        if (actions != null) {
            this.actions = ImmutableList.copyOf(actions);
        }
    }

    public Set<String> getStreams() {
        return streams != null ? streams : DEFAULT_STREAM;
    }

    public void setStreams(Set<String> streams) {
        this.streams = streams;
    }

    public String getOutputStreamNameForAction(Action action) {
        return ruleProcessorName + "." + name + "." + id + "." + action.getName();
    }

    public Having getHaving() {
        return having;
    }

    public void setHaving(Having having) {
        this.having = having;
    }

    public Window getWindow() {
        return window;
    }

    public void setWindow(Window window) {
        this.window = window;
    }

    public GroupBy getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(GroupBy groupBy) {
        this.groupBy = groupBy;
    }

    public void setReferredUdfs(Set<Udf> referredUdfs) {
        this.referredUdfs = new HashSet<>(referredUdfs);
    }

    public Set<Udf> getReferredUdfs() {
        return referredUdfs;
    }

    @Override
    public String toString() {
        return "Rule{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", ruleProcessorName='" + ruleProcessorName + '\'' +
                ", streams=" + streams +
                ", projection=" + projection +
                ", condition=" + condition +
                ", groupBy=" + groupBy +
                ", having=" + having +
                ", window=" + window +
                ", actions=" + actions +
                ", referredUdfs=" + referredUdfs +
                '}';
    }
}

