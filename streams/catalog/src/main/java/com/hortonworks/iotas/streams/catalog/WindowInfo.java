package com.hortonworks.iotas.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.catalog.AbstractStorable;
import com.hortonworks.iotas.streams.layout.component.rule.Rule;
import com.hortonworks.iotas.streams.layout.component.rule.action.Action;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Window;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WindowInfo extends AbstractStorable {
    public static final String NAMESPACE = "windowinfos";

    public static final String ID = "id";
    public static final String TOPOLOGY_ID = "topologyId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String STREAMS = "streams";
    public static final String CONDITION = "condition";
    public static final String PARSED_RULE_STR = "parsedRuleStr";
    public static final String WINDOW = "window";
    public static final String ACTIONS = "actions";
    public static final String PROJECTIONS = "projections";
    public static final String GROUPBYKEYS = "groupbykeys";

    private Long id;
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

    /**
     * A Projection can be either an expression or a function with zero or more args.
     * <ol>
     * <li> Expression - an expression can be
     *    <ul>
     *    <li> A simple one that selects the input field itself. <br/>
     *        E.g. {@code {"expr": "temperature"}}
     *    <li> An expression involving one or more input fields or functions. <br/>
     *        E.g. {@code {"expr": "temperature*9/5 + 32"}}
     *    </ul>
     * <li> A function with zero or more args <br/>
     *    E.g. {@code {"functionName":"topN", "args":["5", "temperature"]}}
     * </ol>
     * In both of the above cases an optional output field name can be specified so the
     * result would be emitted with that field name. For E.g. <br/>
     *  <ul> <li> {@code {"expr": "temperature*9/5 + 32", "outputFieldName": "temp_farenhiet"}} <br/>
     *       <li> {@code {"functionName":"topN", "args":["5", "temperature"], "outputFieldName": "top_5_temp"}}
     *  </ul>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Projection {
        public String expr;
        public String functionName;
        public List<String> args;
        public String outputFieldName;

        // for jackson
        public Projection() {
        }

        public Projection(String expr, String functionName, List<String> args, String outputFieldName) {
            this.expr = expr;
            this.functionName = functionName;
            this.args = new ArrayList<>(args);
            this.outputFieldName = outputFieldName;
        }

        @Override
        public String toString() {
            String str;
            if (!StringUtils.isEmpty(functionName)) {
                str = functionName + "(" + Joiner.on(",").join(args) + ")";
            } else if (!StringUtils.isEmpty(expr)) {
                str = expr;
            } else {
                throw new IllegalArgumentException("expr or functionName should be specified");
            }
            if (!StringUtils.isEmpty(outputFieldName)) {
                str += " AS " + outputFieldName;
            }
            return str;
        }
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
        return Schema.of(
                Schema.Field.of(ID, Schema.Type.LONG),
                Schema.Field.of(TOPOLOGY_ID, Schema.Type.LONG),
                Schema.Field.of(NAME, Schema.Type.STRING),
                Schema.Field.of(DESCRIPTION, Schema.Type.STRING),
                Schema.Field.of(STREAMS, Schema.Type.STRING),
                Schema.Field.of(CONDITION, Schema.Type.STRING),
                Schema.Field.of(PARSED_RULE_STR, Schema.Type.STRING),
                Schema.Field.of(WINDOW, Schema.Type.STRING),
                Schema.Field.of(ACTIONS, Schema.Type.STRING),
                Schema.Field.of(PROJECTIONS, Schema.Type.STRING),
                Schema.Field.of(GROUPBYKEYS, Schema.Type.STRING)
        );
    }

    @Override
    public Map<String, Object> toMap() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = super.toMap();
        try {
            map.put(STREAMS, streams != null ? mapper.writeValueAsString(streams) : "");
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
        setId((Long) map.get(ID));
        setTopologyId((Long) map.get(TOPOLOGY_ID));
        setName((String) map.get(NAME));
        setDescription((String) map.get(DESCRIPTION));
        setParsedRuleStr((String) map.get(PARSED_RULE_STR));
        try {
            ObjectMapper mapper = new ObjectMapper();
            String streamsStr = (String) map.get(STREAMS);
            if (!StringUtils.isEmpty(streamsStr)) {
                List<String> streams = mapper.readValue(streamsStr, new TypeReference<List<String>>() {
                });
                setStreams(streams);
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
}
