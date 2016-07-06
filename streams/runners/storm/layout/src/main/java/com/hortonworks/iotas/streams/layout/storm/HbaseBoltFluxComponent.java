package com.hortonworks.iotas.streams.layout.storm;

import com.hortonworks.iotas.streams.layout.TopologyLayoutConstants;
import com.hortonworks.iotas.streams.layout.exception.BadTopologyLayoutException;

import java.util.ArrayList;
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
        List<String> configMethodNames = new ArrayList<>();
        List values = new ArrayList();
        if (conf.get(TopologyLayoutConstants.JSON_KEY_WRITE_TO_WAL) != null) {
            configMethodNames.add("writeToWAL");
            values.add(conf.get(TopologyLayoutConstants.JSON_KEY_WRITE_TO_WAL));
        }
        configMethodNames.add("withConfigKey");
        // IOT-203: We are not exposing configKey in hbase component since the value is what really matters and it is captured in topology level config which
        // translates to storm topology level config. UI uses the same key as below for the value of hbase config object in topology level config json
        values.add("hbaseConf");
        List configMethods = getConfigMethodsYaml(configMethodNames.toArray(new String[0]), values.toArray());
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
