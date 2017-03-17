package com.hortonworks.streamline.streams.actions.topology.state;

import com.hortonworks.streamline.common.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class TopologyStateFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyStateFactory.class);

    private final Map<String, TopologyState> states = new HashMap<>();
    private final Map<TopologyState, String> stateNames = new HashMap<>();

    private static final TopologyStateFactory INSTANCE = new TopologyStateFactory();

    public static TopologyStateFactory getInstance() {
        return INSTANCE;
    }

    private TopologyStateFactory() {
        try {
            for (Field field : TopologyStates.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        && TopologyState.class.isAssignableFrom(field.getType())) {
                    TopologyState state = (TopologyState) field.get(null);
                    String name = field.getName();
                    states.put(name, state);
                    stateNames.put(state, name);
                    LOG.debug("Registered topology state {}", name);
                }
            }
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    public TopologyState getTopologyState(String stateName) {
        Utils.requireNonEmpty(stateName, "State name cannot be empty");
        TopologyState state = states.get(stateName);
        if (state == null) {
            throw new IllegalArgumentException("No such state " + stateName);
        }
        return state;
    }

    public String getTopologyStateName(TopologyState state) {
        String name = stateNames.get(state);
        if (name == null) {
            throw new IllegalArgumentException("Unknown state " + state);
        }
        return name;
    }
}
