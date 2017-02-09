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
package com.hortonworks.streamline.streams.layout.storm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handle rules with windowing
 */
public class WindowRuleBoltFluxComponent extends RuleBoltFluxComponent {
    private final Logger log = LoggerFactory.getLogger(WindowRuleBoltFluxComponent.class);

    public WindowRuleBoltFluxComponent() {
    }

    @Override
    protected void generateComponent() {
        rulesProcessor = (RulesProcessor) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);
        String boltId = "windowruleBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.streamline.streams.runtime.storm.bolt.rules.WindowRulesBolt";
        List boltConstructorArgs = new ArrayList();
        ObjectMapper mapper = new ObjectMapper();
        String rulesProcessorJson = null;
        try {
            rulesProcessorJson = mapper.writeValueAsString(rulesProcessor);
        } catch (JsonProcessingException e) {
            log.error("Error creating json config string for RulesProcessor",
                    e);
        }
        boltConstructorArgs.add(rulesProcessorJson);
        // hardcode script type enum for now.
        boltConstructorArgs.add("SQL");
        String[] configMethodNames = {"withWindowConfig"};
        Object[] configKeys = {getRefYaml(addWindowConfig())};
        List configMethods = getConfigMethodsYaml(configMethodNames, configKeys);
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, configMethods);
        addParallelismToComponent();
    }

    private String addWindowConfig() {
        String windowId = "window" + UUID_FOR_COMPONENTS;
        String windowClassName = "com.hortonworks.streamline.streams.layout.component.rule.expression.Window";
        ObjectMapper mapper = new ObjectMapper();
        String windowJson = null;
        try {
            Set<Window> windows = new HashSet<>(Collections2.transform(rulesProcessor.getRules(), new Function<Rule, Window>() {
                @Override
                public Window apply(Rule input) {
                    return input.getWindow();
                }
            }));
            if (windows.size() != 1) {
                throw new IllegalArgumentException("All the rules in a windowed rule bolt should have the same window config.");
            }
            windowJson = mapper.writeValueAsString(windows.iterator().next());
        } catch (JsonProcessingException e) {
            log.error("Error creating json config string for RulesProcessor", e);
        }
        List constructorArgs = new ArrayList();
        constructorArgs.add(windowJson);
        this.addToComponents(this.createComponent(windowId, windowClassName, null, constructorArgs, null));
        return windowId;
    }
}
