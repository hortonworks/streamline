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

package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.streams.layout.component.rule.action.Action;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Window;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A rule as represented in the UI layout
 */
@StorableEntity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopologyRule extends BaseTopologyRule {
    public static final String NAMESPACE = "topology_rule";

    public static final String ID = "id";
    public static final String VERSIONID = "versionId";
    public static final String TOPOLOGY_ID = "topologyId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String STREAMS = "streams";
    public static final String OUTPUT_STREAMS = "outputStreams";
    public static final String PROJECTIONS = "projections";
    public static final String CONDITION = "condition";
    public static final String SQL = "sql";
    public static final String PARSED_RULE_STR = "parsedRuleStr";
    public static final String WINDOW = "window";
    public static final String ACTIONS = "actions";

    private Long id;
    private Long versionId;
    private Long topologyId;
    private String name;
    private String description;
    /*
     * A rule info object can have either
     * 1. the full sql string or
     * 2. the streams and condition string, in which case
     *    its translated into a select * from <stream> where <condition>
     */
    private List<String> streams;
    private List<Projection> projections;
    private String condition;
    private String sql;
    private String parsedRuleStr;
    private Window window;
    private List<Action> actions;
    private Long versionTimestamp;
    // optional list of output streams that this rule emits to
    private List<String> outputStreams;

    // for jackson
    public TopologyRule() {
    }

    public TopologyRule(TopologyRule other) {
        if (other != null) {
            setId(other.getId());
            setVersionId(other.getVersionId());
            setTopologyId(other.getTopologyId());
            setName(other.getName());
            setDescription(other.getDescription());
            setCondition(other.getCondition());
            setSql(other.getSql());
            setParsedRuleStr(other.getParsedRuleStr());
            if(other.getProjections() != null) {
                setProjections(new ArrayList<>(other.getProjections()));
            }
            if (other.getWindow() != null) {
                setWindow(new Window(other.getWindow()));
            }
            if (other.getStreams() != null) {
                setStreams(new ArrayList<>(other.getStreams()));
            }
            if (other.getActions() != null) {
                setActions(other.getActions().stream().map(Action::copy).collect(Collectors.toList()));
            }
            if (other.getOutputStreams() != null) {
                setOutputStreams(new ArrayList<>(other.getOutputStreams()));
            }
            setVersionTimestamp(other.getVersionTimestamp());
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("timestamp")
    public Long getVersionTimestamp() {
        return versionTimestamp;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("timestamp")
    public void setVersionTimestamp(Long versionTimestamp) {
        this.versionTimestamp = versionTimestamp;
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(ID, Schema.Type.LONG), this.id);
        fieldToObjectMap.put(new Schema.Field(VERSIONID, Schema.Type.LONG), this.versionId);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
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

    public List<Projection> getProjections() {
        return projections;
    }

    public void setProjections(List<Projection> projections) {
        this.projections = projections;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public List<String> getStreams() {
        return streams;
    }

    public void setStreams(List<String> streams) {
        this.streams = streams;
    }

    public List<String> getOutputStreams() {
        return outputStreams;
    }

    public void setOutputStreams(List<String> outputStreams) {
        this.outputStreams = outputStreams;
    }

    @JsonIgnore
    @Override
    public String getParsedRuleStr() {
        return parsedRuleStr;
    }

    @JsonIgnore
    public void setParsedRuleStr(String parsedRuleStr) {
        this.parsedRuleStr = parsedRuleStr;
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
        return Schema.unionOf(
                super.getSchema(),
                Schema.of(
                        Schema.Field.of(ID, Schema.Type.LONG),
                        Schema.Field.of(VERSIONID, Schema.Type.LONG),
                        Schema.Field.of(TOPOLOGY_ID, Schema.Type.LONG),
                        Schema.Field.of(NAME, Schema.Type.STRING),
                        Schema.Field.of(DESCRIPTION, Schema.Type.STRING),
                        Schema.Field.of(STREAMS, Schema.Type.STRING),
                        Schema.Field.of(OUTPUT_STREAMS, Schema.Type.STRING),
                        Schema.Field.of(PROJECTIONS, Schema.Type.STRING),
                        Schema.Field.of(CONDITION, Schema.Type.STRING),
                        Schema.Field.of(SQL, Schema.Type.STRING),
                        Schema.Field.of(PARSED_RULE_STR, Schema.Type.STRING),
                        Schema.Field.of(WINDOW, Schema.Type.STRING),
                        Schema.Field.of(ACTIONS, Schema.Type.STRING),
                        Schema.Field.of(RECONFIGURE, Schema.Type.BOOLEAN)
                ));
    }

    @Override
    public Map<String, Object> toMap() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = super.toMap();
        try {
            map.put(STREAMS, streams != null ? mapper.writeValueAsString(streams) : "");
            map.put(OUTPUT_STREAMS, outputStreams != null ? mapper.writeValueAsString(outputStreams) : "");
            map.put(WINDOW, window != null ? mapper.writeValueAsString(window) : "");

            map.put(PROJECTIONS,
                    projections != null
                            ? mapper.writerFor(new TypeReference<List<Projection>>() {}).writeValueAsString(projections)
                            : "");

            map.put(ACTIONS,
                    actions != null
                            ? mapper.writerFor(new TypeReference<List<Action>>() {}).writeValueAsString(actions)
                            : "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    @Override
    public Storable fromMap(Map<String, Object> map) {
        super.fromMap(map);
        setId((Long) map.get(ID));
        setVersionId((Long) map.get(VERSIONID));
        setTopologyId((Long) map.get(TOPOLOGY_ID));
        setName((String) map.get(NAME));
        setDescription((String) map.get(DESCRIPTION));
        setCondition((String) map.get(CONDITION));
        setSql((String) map.get(SQL));
        setParsedRuleStr((String) map.get(PARSED_RULE_STR));
        try {
            ObjectMapper mapper = new ObjectMapper();
            String streamsStr = (String) map.get(STREAMS);
            if (!StringUtils.isEmpty(streamsStr)) {
                List<String> streams = mapper.readValue(streamsStr, new TypeReference<List<String>>() {
                });
                setStreams(streams);
            }
            String outputStreamsStr = (String) map.get(OUTPUT_STREAMS);
            if (!StringUtils.isEmpty(outputStreamsStr)) {
                List<String> outputStreams = mapper.readValue(outputStreamsStr, new TypeReference<List<String>>() {
                });
                setOutputStreams(outputStreams);
            }
            String windowStr = (String) map.get(WINDOW);
            if (!StringUtils.isEmpty(windowStr)) {
                Window window = mapper.readValue(windowStr, Window.class);
                setWindow(window);
            }

            String projectionsStr = (String) map.get(PROJECTIONS);
            if (!StringUtils.isEmpty(projectionsStr)) {
                setProjections(mapper.readValue(projectionsStr, new TypeReference<List<Projection>>() {}));
            }

            String actionsStr = (String) map.get(ACTIONS);
            if (!StringUtils.isEmpty(actionsStr)) {
                setActions(mapper.readValue(actionsStr, new TypeReference<List<Action>>() {}));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public Set<String> getInputStreams() {
        return new HashSet<>(streams);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopologyRule topologyRule = (TopologyRule) o;

        if (id != null ? !id.equals(topologyRule.id) : topologyRule.id != null) return false;
        return versionId != null ? versionId.equals(topologyRule.versionId) : topologyRule.versionId == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (versionId != null ? versionId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RuleInfo{" +
                "id=" + id +
                ", versionId=" + versionId +
                ", topologyId=" + topologyId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", streams=" + streams +
                ", projections=" + projections +
                ", condition='" + condition + '\'' +
                ", sql='" + sql + '\'' +
                ", parsedRuleStr='" + parsedRuleStr + '\'' +
                ", window=" + window +
                ", actions=" + actions +
                ", versionTimestamp=" + versionTimestamp +
                ", outputStreams=" + outputStreams +
                '}';
    }
}
