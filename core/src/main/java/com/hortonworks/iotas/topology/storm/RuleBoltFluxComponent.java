package com.hortonworks.iotas.topology.storm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.util.exception.BadTopologyLayoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation for RuleBolt
 */
public class RuleBoltFluxComponent extends AbstractFluxComponent {
    private final Logger log = LoggerFactory.getLogger(RuleBoltFluxComponent.class);
    @Override
    protected void generateComponent () {
        String rulesBoltDependenciesFactory = addRulesBoltDependenciesFactory();
        String boltId = "ruleBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.iotas.bolt.rules.RulesBolt";
        List boltConstructorArgs = new ArrayList();
        Map ref = getRefYaml(rulesBoltDependenciesFactory);
        boltConstructorArgs.add(ref);
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs,
                null);
        addParallelismToComponent();
    }

    private String addRulesBoltDependenciesFactory () {
        String rulesProcessorBuilderRef = addRulesProcessorBuilder();
        String dependenciesFactoryId = "dependenciesFactory" + UUID_FOR_COMPONENTS;
        String dependenciesFactoryClassName = "com.hortonworks.iotas.layout.runtime.rule.RulesBoltDependenciesFactory";
        //constructor args
        List constructorArgs = new ArrayList();
        Map ref = getRefYaml(rulesProcessorBuilderRef);
        constructorArgs.add(ref);
        // hardcode script type enum for now. SQL also supported. For future this should be taken as input from UI
        constructorArgs.add("GROOVY");
        this.addToComponents(this.createComponent(dependenciesFactoryId, dependenciesFactoryClassName, null, constructorArgs, null));
        return dependenciesFactoryId;
    }

    private String addRulesProcessorBuilder () {
        String rulesProcessorBuilderComponentId = "rulesProcessorBuilder" +
                UUID_FOR_COMPONENTS;
        String rulesProcessorBuilderClassName = "com.hortonworks.iotas.layout" +
                ".design.component.RulesProcessorJsonBuilder";
        ObjectMapper mapper = new ObjectMapper();
        String rulesProcessorJson = null;
        try {
            rulesProcessorJson = mapper.writeValueAsString(conf.get
                    (TopologyLayoutConstants.JSON_KEY_RULES_PROCESSOR_CONFIG));
        } catch (JsonProcessingException e) {
            log.error("Error creating json config string for RulesProcessor",
                    e);
        }
        //constructor args
        List constructorArgs = new ArrayList();
        constructorArgs.add(rulesProcessorJson);
        this.addToComponents(this.createComponent(rulesProcessorBuilderComponentId,
                rulesProcessorBuilderClassName, null, constructorArgs, null));
        return rulesProcessorBuilderComponentId;
    }

    @Override
    public void validateConfig () throws BadTopologyLayoutException {
        super.validateConfig();
        String fieldName = TopologyLayoutConstants.JSON_KEY_RULES_PROCESSOR_CONFIG;
        Map rulesProcessorConfig = (Map) conf.get(fieldName);
        if (rulesProcessorConfig == null) {
            throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
        }
        /*
        Commenting the below code because of cyclic dependency between layout
        module and core module
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readValue(mapper.writeValueAsString(rulesProcessorConfig), RulesProcessor.class);
            //TODO: may be add further validation here for the RulesProcessor
            // object successfully created?
        } catch (IOException e) {
            throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
        }*/
    }
}
