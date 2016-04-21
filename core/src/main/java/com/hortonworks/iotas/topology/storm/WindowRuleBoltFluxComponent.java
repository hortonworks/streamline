package com.hortonworks.iotas.topology.storm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handle rules with windowing
 */
public class WindowRuleBoltFluxComponent extends RuleBoltFluxComponent {
    private final Logger log = LoggerFactory.getLogger(WindowRuleBoltFluxComponent.class);

    @Override
    protected void generateComponent() {
        String rulesBoltDependenciesFactory = addRulesBoltDependenciesFactory();
        String boltId = "windowruleBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.iotas.bolt.rules.WindowRulesBolt";
        List boltConstructorArgs = new ArrayList();
        Map ref = getRefYaml(rulesBoltDependenciesFactory);
        boltConstructorArgs.add(ref);

        String[] configMethodNames = {"withWindowConfig"};
        Object[] configKeys = {getRefYaml(addWindowConfig())};
        List configMethods = getConfigMethodsYaml(configMethodNames, configKeys);
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, configMethods);
        addParallelismToComponent();
    }

    private String addWindowConfig() {
        String windowId = "window" + UUID_FOR_COMPONENTS;
        String windowClassName = "com.hortonworks.iotas.layout.design.rule.condition.Window";
        ObjectMapper mapper = new ObjectMapper();
        String windowJson = null;
        try {
            windowJson = mapper.writeValueAsString(conf.get(TopologyLayoutConstants.JSON_KEY_RULE_WINDOW_CONFIG));
        } catch (JsonProcessingException e) {
            log.error("Error creating json config string for RulesProcessor", e);
        }
        List constructorArgs = new ArrayList();
        constructorArgs.add(windowJson);
        this.addToComponents(this.createComponent(windowId, windowClassName, null, constructorArgs, null));
        return windowId;
    }
}
