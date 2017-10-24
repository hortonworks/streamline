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
import com.hortonworks.registries.storage.PrimaryKey;
import com.hortonworks.registries.storage.Storable;
import com.hortonworks.registries.storage.annotation.StorableEntity;
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

@StorableEntity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopologyWindow extends BaseTopologyRule {
    public static final String NAMESPACE = "topology_window";

    public static final String ID = "id";
    public static final String VERSIONID = "versionId";
    public static final String TOPOLOGY_ID = "topologyId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String STREAMS = "streams";
    public static final String OUTPUT_STREAMS = "outputStreams";
    public static final String CONDITION = "condition";
    public static final String PARSED_RULE_STR = "parsedRuleStr";
    public static final String WINDOW = "window";
    public static final String ACTIONS = "actions";
    public static final String PROJECTIONS = "projections";
    public static final String GROUPBYKEYS = "groupbykeys";

    private Long id;
    private Long versionId;
    private Long topologyId;
    private String name = StringUtils.EMPTY;
    private String description = StringUtils.EMPTY;
    private List<String> streams;
    private String condition;
    private String parsedRuleStr;
    private Window window;
    private List<Action> actions;
    private List<Projection> projections;
    private List<String> groupbykeys;
    private Long versionTimestamp;
    // optional list of output streams that this rule emits to
    private List<String> outputStreams;

    public TopologyWindow() {
    }

    public TopologyWindow(TopologyWindow other) {
        if (other != null) {
            setId(other.getId());
            setVersionId(other.getVersionId());
            setTopologyId(other.getTopologyId());
            setName(other.getName());
            setDescription(other.getDescription());
            if (other.getStreams() != null) {
                setStreams(new ArrayList<>(other.getStreams()));
            }
            setCondition(other.getCondition());
            setParsedRuleStr(other.getParsedRuleStr());
            if (other.getWindow() != null) {
                setWindow(new Window(other.getWindow()));
            }
            if (other.getActions() != null) {
                setActions(other.getActions().stream().map(Action::copy).collect(Collectors.toList()));
            }
            if (other.getProjections() != null) {
                setProjections(other.getProjections().stream().map(Projection::new).collect(Collectors.toList()));
            }
            if (other.getGroupbykeys() != null) {
                setGroupbykeys(new ArrayList<>(other.getGroupbykeys()));
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
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
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

    public Long getTopologyId() {
        return topologyId;
    }

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
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

    public List<Projection> getProjections() {
        return projections;
    }

    public void setProjections(List<Projection> projections) {
        this.projections = projections;
    }

    public List<String> getGroupbykeys() {
        return groupbykeys;
    }

    public void setGroupbykeys(List<String> groupbykeys) {
        this.groupbykeys = groupbykeys;
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
                        Schema.Field.of(CONDITION, Schema.Type.STRING),
                        Schema.Field.of(PARSED_RULE_STR, Schema.Type.STRING),
                        Schema.Field.of(WINDOW, Schema.Type.STRING),
                        Schema.Field.of(ACTIONS, Schema.Type.STRING),
                        Schema.Field.of(PROJECTIONS, Schema.Type.STRING),
                        Schema.Field.of(GROUPBYKEYS, Schema.Type.STRING)
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
            map.put(ACTIONS, actions != null ? mapper.writerFor(new TypeReference<List<Action>>() {
            }).writeValueAsString(actions) : "");
            map.put(PROJECTIONS, projections != null ? mapper.writeValueAsString(projections) : "");
            map.put(GROUPBYKEYS, groupbykeys != null ? mapper.writeValueAsString(groupbykeys) : "");
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
            String actionsStr = (String) map.get(ACTIONS);
            if (!StringUtils.isEmpty(actionsStr)) {
                List<Action> actions = mapper.readValue(actionsStr, new TypeReference<List<Action>>() {
                });
                setActions(actions);
            }
            String projectionsStr = (String) map.get(PROJECTIONS);
            if (!StringUtils.isEmpty(projectionsStr)) {
                List<Projection> projections = mapper.readValue(projectionsStr, new TypeReference<List<Projection>>() {
                });
                setProjections(projections);
            }
            String groupbykeysStr = (String) map.get(GROUPBYKEYS);
            if (!StringUtils.isEmpty(groupbykeysStr)) {
                List<String> groupbykeys = mapper.readValue(groupbykeysStr, new TypeReference<List<String>>() {
                });
                setGroupbykeys(groupbykeys);
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

        TopologyWindow that = (TopologyWindow) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return versionId != null ? versionId.equals(that.versionId) : that.versionId == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (versionId != null ? versionId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TopologyWindow{" +
                "id=" + id +
                ", versionId=" + versionId +
                ", topologyId=" + topologyId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", streams=" + streams +
                ", condition='" + condition + '\'' +
                ", parsedRuleStr='" + parsedRuleStr + '\'' +
                ", window=" + window +
                ", actions=" + actions +
                ", projections=" + projections +
                ", groupbykeys=" + groupbykeys +
                ", versionTimestamp=" + versionTimestamp +
                ", outputStreams=" + outputStreams +
                "}";
    }
}
