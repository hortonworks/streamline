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
package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.catalog.AbstractStorable;
import com.hortonworks.iotas.topology.component.rule.Rule;
import com.hortonworks.iotas.topology.component.rule.action.Action;
import com.hortonworks.iotas.topology.component.rule.condition.Window;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A rule as represented in the UI layout
 */
public class RuleInfo extends AbstractStorable {
    public static final String NAMESPACE = "ruleinfos";

    public static final String ID = "id";
    public static final String TOPOLOGY_ID = "topologyId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String SQL = "sql";
    public static final String PARSED_RULE_STR = "parsedRuleStr";
    public static final String WINDOW = "window";
    public static final String ACTIONS = "actions";

    private Long id;
    private Long topologyId;
    private String name;
    private String description;
    private String sql;
    private String parsedRuleStr;
    private Window window;
    private List<Action> actions;

    // for jackson
    public RuleInfo() {
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

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

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Long getTopologyId() {
        return topologyId;
    }

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
    }

    @JsonIgnore
    public String getParsedRuleStr() {
        return parsedRuleStr;
    }

    @JsonIgnore
    public void setParsedRuleStr(String parsedRuleStr) {
        this.parsedRuleStr = parsedRuleStr;
    }

    @JsonIgnore
    public Rule getRule() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Rule rule = mapper.readValue(getParsedRuleStr(), Rule.class);
        return rule;
    }

    public Window getWindow() {
        return window;
    }

    public void setWindow(Window window) {
        this.window = window;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    @JsonIgnore
    @Override
    public Schema getSchema() {
        return Schema.of(
                Schema.Field.of(ID, Schema.Type.LONG),
                Schema.Field.of(TOPOLOGY_ID, Schema.Type.LONG),
                Schema.Field.of(NAME, Schema.Type.STRING),
                Schema.Field.of(DESCRIPTION, Schema.Type.STRING),
                Schema.Field.of(SQL, Schema.Type.STRING),
                Schema.Field.of(PARSED_RULE_STR, Schema.Type.STRING),
                Schema.Field.of(WINDOW, Schema.Type.STRING),
                Schema.Field.of(ACTIONS, Schema.Type.STRING)
        );
    }

    @Override
    public Map<String, Object> toMap() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = super.toMap();
        try {
            map.put(WINDOW, window != null ? mapper.writeValueAsString(window) : "");
            map.put(ACTIONS, actions != null ? mapper.writeValueAsString(actions) : "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    @Override
    public Storable fromMap(Map<String, Object> map) {
        setId((Long) map.get(ID));
        setTopologyId((Long) map.get(TOPOLOGY_ID));
        setName((String) map.get(NAME));
        setDescription((String) map.get(DESCRIPTION));
        setSql((String) map.get(SQL));
        setParsedRuleStr((String) map.get(PARSED_RULE_STR));
        try {
            ObjectMapper mapper = new ObjectMapper();
            String windowStr = (String) map.get(WINDOW);
            if (!StringUtils.isEmpty(windowStr)) {
                Window window = mapper.readValue(windowStr, Window.class);
                setWindow(window);
            }
            String actionsStr = (String) map.get(ACTIONS);
            if (!StringUtils.isEmpty(actionsStr)) {
                List<Action> actions = mapper.readValue(actionsStr, new TypeReference<List<Action>>() {
                });
                setActions(actions);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RuleInfo ruleInfo = (RuleInfo) o;

        if (id != null ? !id.equals(ruleInfo.id) : ruleInfo.id != null) return false;
        return topologyId != null ? topologyId.equals(ruleInfo.topologyId) : ruleInfo.topologyId == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (topologyId != null ? topologyId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RuleInfo{" +
                "id=" + id +
                ", topologyId=" + topologyId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", sql='" + sql + '\'' +
                ", parsedRuleStr='" + parsedRuleStr + '\'' +
                ", window=" + window +
                ", actions=" + actions +
                '}';
    }
}
