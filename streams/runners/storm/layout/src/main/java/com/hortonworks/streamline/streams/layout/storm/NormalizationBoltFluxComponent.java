package com.hortonworks.streamline.streams.layout.storm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.component.impl.normalization.NormalizationProcessor;
import com.hortonworks.streamline.streams.layout.exception.ComponentConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Flux yaml generation implementation for normalization processor.
 */
public class NormalizationBoltFluxComponent extends AbstractFluxComponent {
    private final Logger log = LoggerFactory.getLogger(NormalizationBoltFluxComponent.class);
    private NormalizationProcessor normalizationProcessor;

    public NormalizationBoltFluxComponent () {}

    @Override
    protected void generateComponent() {
        normalizationProcessor = (NormalizationProcessor) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);
        String boltId = "normalizationBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.streamline.streams.runtime.storm.bolt.normalization.NormalizationBolt";
        List boltConstructorArgs = new ArrayList();
        ObjectMapper mapper = new ObjectMapper();
        String normalizationProcessorJson = null;
        try {
            normalizationProcessorJson = mapper.writeValueAsString(normalizationProcessor);
        } catch (JsonProcessingException e) {
            log.error("Error creating json config string for NormalizationProcessor", e);
            throw new RuntimeException(e);
        }
        boltConstructorArgs.add(normalizationProcessorJson);
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, null);
        addParallelismToComponent();
    }

    @Override
    public void validateConfig() throws ComponentConfigException {
        super.validateConfig();
        String fieldName = TopologyLayoutConstants.JSON_KEY_NORMALIZATION_PROCESSOR_CONFIG;
        Map normalizationProcessorConfig = (Map) conf.get(fieldName);
        if (normalizationProcessorConfig == null) {
            throw new ComponentConfigException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
        }
    }
}
