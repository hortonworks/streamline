package org.apache.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.streamline.common.Schema;
import org.apache.streamline.storage.PrimaryKey;
import org.apache.streamline.storage.Storable;
import org.apache.streamline.storage.catalog.AbstractStorable;
import org.apache.streamline.streams.layout.component.rule.Rule;
import org.apache.streamline.streams.layout.component.rule.action.Action;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A branch rule as represented in the UI layout
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BranchRuleInfo extends AbstractStorable {
    public static final String NAMESPACE = "branchruleinfos";

    public static final String ID = "id";
    public static final String TOPOLOGY_ID = "topologyId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String STREAM = "stream";
    public static final String CONDITION = "condition";
    public static final String PARSED_RULE_STR = "parsedRuleStr";
    public static final String ACTIONS = "actions";

    private Long id;
    private Long topologyId;
    private String name;
    private String description;
    /*
     * A branch info object will have either
     * 1. the input stream and
     * 2. the condition string
     *    its translated into a select * from <stream> where <condition>
     */
    private String stream;
    private String condition;
    private String parsedRuleStr;
    private List<Action> actions;

    // for jackson
    public BranchRuleInfo() {
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
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

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
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
        return mapper.readValue(getParsedRuleStr(), Rule.class);
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
                Schema.Field.of(STREAM, Schema.Type.STRING),
                Schema.Field.of(CONDITION, Schema.Type.STRING),
                Schema.Field.of(PARSED_RULE_STR, Schema.Type.STRING),
                Schema.Field.of(ACTIONS, Schema.Type.STRING)
        );
    }

    @Override
    public Map<String, Object> toMap() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = super.toMap();
        try {
            map.put(STREAM, stream != null ? mapper.writeValueAsString(stream) : "");
            map.put(ACTIONS, actions != null ? mapper.writerFor(new TypeReference<List<Action>>() {
            }).writeValueAsString(actions) : "");
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
        setParsedRuleStr((String) map.get(PARSED_RULE_STR));
        setStream((String) map.get(STREAM));
        try {
            ObjectMapper mapper = new ObjectMapper();
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

        BranchRuleInfo brRuleInfo = (BranchRuleInfo) o;

        if (id != null ? !id.equals(brRuleInfo.id) : brRuleInfo.id != null) return false;
        return topologyId != null ? topologyId.equals(brRuleInfo.topologyId) : brRuleInfo.topologyId == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (topologyId != null ? topologyId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BranchRuleInfo{" +
                "id=" + id +
                ", topologyId=" + topologyId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", stream=" + stream +
                ", condition=" + condition +
                ", parsedRuleStr='" + parsedRuleStr + '\'' +
                ", actions=" + actions +
                "}";
    }
}