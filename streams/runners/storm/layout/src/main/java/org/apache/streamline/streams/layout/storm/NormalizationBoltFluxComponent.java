package org.apache.streamline.streams.layout.storm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.streams.layout.TopologyLayoutConstants;
import org.apache.streamline.streams.layout.component.impl.normalization.NormalizationProcessor;
import org.apache.streamline.streams.layout.exception.ComponentConfigException;
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
        String normalizationProcessorId = addNormalizationProcessorBuilder();

        String boltId = "normalizationBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "org.apache.streamline.streams.runtime.storm.bolt.normalization.NormalizationBolt";
        List boltConstructorArgs = new ArrayList();
        Map ref = getRefYaml(normalizationProcessorId);
        boltConstructorArgs.add(ref);
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, null);
        addParallelismToComponent();
    }

    private String addNormalizationProcessorBuilder() {
        String normalizationProcessorBuilderComponentId = "normalizationProcessorBuilder" + UUID_FOR_COMPONENTS;
        String normalizationProcessorBuilderClassName = "org.apache.streamline.streams.layout.component.impl.normalization.NormalizationProcessorJsonBuilder";
        ObjectMapper mapper = new ObjectMapper();
        String normalizationProcessorJson = null;
        try {
            normalizationProcessorJson = mapper.writeValueAsString(normalizationProcessor);
        } catch (JsonProcessingException e) {
            log.error("Error creating json config string for NormalizationProcessor", e);
            throw new RuntimeException(e);
        }

        //constructor args
        List constructorArgs = new ArrayList();
        constructorArgs.add(normalizationProcessorJson);
        this.addToComponents(this.createComponent(normalizationProcessorBuilderComponentId, normalizationProcessorBuilderClassName, null, constructorArgs, null));
        return normalizationProcessorBuilderComponentId;
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
