package com.hortonworks.iotas.topology.storm;

import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.util.exception.BadTopologyLayoutException;

import java.util.List;

/**
 * Implementation for ParserBolt
 */
public class ParserBoltFluxComponent extends AbstractFluxComponent {

    @Override
    protected void generateComponent () {
        String boltId = "parserBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.iotas.bolt.ParserBolt";
        String[] configMethodNames = {"withParsedTuplesStreamId",
                "withUnparsedTuplesStreamId", "withParserId",
                "withDataSourceId"};
        String[] configKeys = {
                TopologyLayoutConstants.JSON_KEY_PARSED_TUPLES_STREAM,
                TopologyLayoutConstants.JSON_KEY_FAILED_TUPLES_STREAM,
                TopologyLayoutConstants.JSON_KEY_PARSER_ID,
                TopologyLayoutConstants.JSON_KEY_DATA_SOURCE_ID
        };
        List configMethods = getConfigMethodsYaml(configMethodNames,
                configKeys);
        component = createComponent(boltId, boltClassName, null, null, configMethods);
        addParallelismToComponent();
    }

    @Override
    public void validateConfig () throws BadTopologyLayoutException {
        super.validateConfig();
        validateStringFields();
        validateLongFields();
    }

    private void validateStringFields () throws BadTopologyLayoutException {
        String[] requiredStringFields = {
            TopologyLayoutConstants.JSON_KEY_PARSED_TUPLES_STREAM
        };
        validateStringFields(requiredStringFields, true);
        String[] optionalStringFields = {
            TopologyLayoutConstants.JSON_KEY_FAILED_TUPLES_STREAM
        };
        validateStringFields(optionalStringFields, false);
    }

    private void validateLongFields () throws BadTopologyLayoutException {
        String[] optionalLongFields = {
            TopologyLayoutConstants.JSON_KEY_DATA_SOURCE_ID,
            TopologyLayoutConstants.JSON_KEY_PARSER_ID
        };
        Long[] mins = {
            1l, 1l
        };
        Long[] maxes = {
            Long.MAX_VALUE, Long.MAX_VALUE
        };
        validateLongFields(optionalLongFields, false, mins, maxes);
    }
}
