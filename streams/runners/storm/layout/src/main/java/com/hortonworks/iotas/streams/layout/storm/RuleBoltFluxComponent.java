package com.hortonworks.iotas.streams.layout.storm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.streams.layout.TopologyLayoutConstants;
import com.hortonworks.iotas.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.iotas.streams.layout.exception.BadTopologyLayoutException;
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
    protected RulesProcessor rulesProcessor;

    public RuleBoltFluxComponent() {
    }

    public RuleBoltFluxComponent(RulesProcessor rulesProcessor) {
        this.rulesProcessor = rulesProcessor;
    }

    @Override
    protected void generateComponent () {
        String rulesBoltDependenciesFactory = addRulesBoltDependenciesFactory();
        String boltId = "ruleBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.iotas.streams.runtime.storm.bolt.rules.RulesBolt";
        List boltConstructorArgs = new ArrayList();
        Map ref = getRefYaml(rulesBoltDependenciesFactory);
        boltConstructorArgs.add(ref);
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, null);
        addParallelismToComponent();
    }

    protected String addRulesBoltDependenciesFactory () {
        String rulesProcessorBuilderRef = addRulesProcessorBuilder();
        String dependenciesFactoryId = "dependenciesFactory" + UUID_FOR_COMPONENTS;
        String dependenciesFactoryClassName = "com.hortonworks.iotas.streams.runtime.rule.RulesDependenciesFactory";
        //constructor args
        List constructorArgs = new ArrayList();
        Map ref = getRefYaml(rulesProcessorBuilderRef);
        constructorArgs.add(ref);
        // hardcode script type enum for now. SQL also supported. For future this should be taken as input from UI
        constructorArgs.add("SQL");
        this.addToComponents(this.createComponent(dependenciesFactoryId, dependenciesFactoryClassName, null, constructorArgs, null));
        return dependenciesFactoryId;
    }

    private String addRulesProcessorBuilder () {
        String rulesProcessorBuilderComponentId = "rulesProcessorBuilder" +
                UUID_FOR_COMPONENTS;
        String rulesProcessorBuilderClassName = "com.hortonworks.iotas.streams" +
                ".layout.component.RulesProcessorJsonBuilder";
        ObjectMapper mapper = new ObjectMapper();
        String rulesProcessorJson = null;
        try {
            rulesProcessorJson = mapper.writeValueAsString(rulesProcessor);
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
