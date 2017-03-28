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
