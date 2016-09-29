package com.hortonworks.iotas.streams.catalog;

import com.hortonworks.iotas.streams.layout.component.rule.action.Action;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A domain transfer object for capturing just the windowing and
 * aggregation function parameters.
 */
public class WindowDto {
    private Long id;
    private String name;
    private String description;
    private List<String> streams;
    private List<String> projections;
    private List<String> groupbykeys;
    private Window window;
    private List<Action> actions;

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

    public List<String> getProjections() {
        return projections;
    }

    public void setProjections(List<String> projections) {
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
