package com.hortonworks.iotas.streams.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hortonworks.iotas.streams.layout.component.rule.action.Action;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Window;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A domain transfer object for capturing just the windowing and
 * aggregation function parameters from UI.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WindowDto {
    private Long id;
    private String name;
    private String description;
    private List<String> streams;
    private List<Projection> projections;
    private List<String> groupbykeys;
    private Window window;
    private List<Action> actions;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Projection {
        public String name;
        public String functionName;
        public String outputFieldName;

        // for jackson
        public Projection() {
        }

        public Projection(String name, String functionName, String outputFieldName) {
            this.name = name;
            this.functionName = functionName;
            this.outputFieldName = outputFieldName;
        }

        @Override
        public String toString() {
            String str;
            if (!StringUtils.isEmpty(functionName)) {
                str = functionName + "(" + name + ")";
            } else {
                str = name;
            }
            if (!StringUtils.isEmpty(outputFieldName)) {
                str += " AS " + outputFieldName;
            }
            return str;
        }
    }

    public WindowDto() {

    }

    public WindowDto(RuleInfo ruleInfo) {
        this.id = ruleInfo.getId();
        this.name = ruleInfo.getName();
        this.description = ruleInfo.getDescription();
        this.streams = new ArrayList<>(ruleInfo.getStreams());
        if (ruleInfo.getProjections() != null) {
            this.projections = new ArrayList<>(ruleInfo.getProjections());
        }
        if (ruleInfo.getGroupbykeys() != null) {
            this.groupbykeys = new ArrayList<>(ruleInfo.getGroupbykeys());
        }
        this.window = new Window(ruleInfo.getWindow());
        this.actions = new ArrayList<>(ruleInfo.getActions());
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

    public List<String> getStreams() {
        return streams;
    }

    public void setStreams(List<String> streams) {
        this.streams = streams;
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
}
