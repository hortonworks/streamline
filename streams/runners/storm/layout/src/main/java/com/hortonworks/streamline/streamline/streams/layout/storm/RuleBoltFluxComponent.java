package com.hortonworks.streamline.streams.layout.storm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.exception.ComponentConfigException;
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

    @Override
    protected void generateComponent () {
        rulesProcessor = (RulesProcessor) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);
        String boltId = "ruleBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.streamline.streams.runtime.storm.bolt.rules.RulesBolt";
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
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, null);
        addParallelismToComponent();
    }

    @Override
    public void validateConfig () throws ComponentConfigException {
        super.validateConfig();
        String fieldName = TopologyLayoutConstants.JSON_KEY_RULES_PROCESSOR_CONFIG;
        Map rulesProcessorConfig = (Map) conf.get(fieldName);
        if (rulesProcessorConfig == null) {
            throw new ComponentConfigException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
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
            throw new ComponentConfigException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
        }*/
    }
}
