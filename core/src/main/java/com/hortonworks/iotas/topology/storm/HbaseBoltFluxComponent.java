package com.hortonworks.iotas.topology.storm;

import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.util.exception.BadTopologyLayoutException;

import java.util.List;
import java.util.Map;

/**
 * Implementation for HbaseBolt
 */
public class HbaseBoltFluxComponent extends AbstractFluxComponent {
    @Override
    protected void generateComponent () {
        String hbaseMapperRef = addHbaseMapperComponent();
        String boltId = "hbaseBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "org.apache.storm.hbase.bolt.HBaseBolt";
        String[] constructorArgNames = {
            TopologyLayoutConstants.JSON_KEY_TABLE
        };
        List boltConstructorArgs = getConstructorArgsYaml(constructorArgNames);
        Map ref = getRefYaml(hbaseMapperRef);
        boltConstructorArgs.add(ref);

        String[] configMethodNames = {"writeToWAL", "withConfigKey"};
        String[] configKeys = {
                TopologyLayoutConstants.JSON_KEY_WRITE_TO_WAL,
                TopologyLayoutConstants.JSON_KEY_CONFIG_KEY
        };
        List configMethods = getConfigMethodsYaml(configMethodNames,
                configKeys);
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, configMethods);
        addParallelismToComponent();
    }

    private String addHbaseMapperComponent () {
        String hbaseMapperComponentId = "hbaseMapper" + UUID_FOR_COMPONENTS;

        // currently only ParserOutputHbaseMapper is supported.
        String hbaseMapperClassName = "com.hortonworks.iotas.hbase" +
                ".ParserOutputHBaseMapper";

        //constructor args
        String[] constructorArgNames = {
            TopologyLayoutConstants.JSON_KEY_COLUMN_FAMILY
        };
        List hbaseMapperConstructorArgs = getConstructorArgsYaml
                (constructorArgNames);

        this.addToComponents(this.createComponent(hbaseMapperComponentId,
                hbaseMapperClassName, null, hbaseMapperConstructorArgs, null));
        return hbaseMapperComponentId;

    }

    @Override
    public void validateConfig () throws BadTopologyLayoutException {
        super.validateConfig();
        validateBooleanFields();
        validateStringFields();
    }

    private void validateBooleanFields () throws BadTopologyLayoutException {
        String[] optionalBooleanFields = {
            TopologyLayoutConstants.JSON_KEY_WRITE_TO_WAL
        };
        validateBooleanFields(optionalBooleanFields, false);
    }

    private void validateStringFields () throws BadTopologyLayoutException {
        String[] requiredStringFields = {
            TopologyLayoutConstants.JSON_KEY_TABLE,
            TopologyLayoutConstants.JSON_KEY_COLUMN_FAMILY
        };
        validateStringFields(requiredStringFields, true);
        String[] optionalStringFields = {
            TopologyLayoutConstants.JSON_KEY_CONFIG_KEY
        };
        validateStringFields(optionalStringFields, false);
    }

}
