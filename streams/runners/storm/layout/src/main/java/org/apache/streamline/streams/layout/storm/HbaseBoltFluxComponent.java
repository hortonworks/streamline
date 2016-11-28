package org.apache.streamline.streams.layout.storm;

import org.apache.streamline.streams.layout.TopologyLayoutConstants;
import org.apache.streamline.streams.layout.exception.ComponentConfigException;

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
        List<Object> values = new ArrayList<>();
        if (conf.get(TopologyLayoutConstants.JSON_KEY_WRITE_TO_WAL) != null) {
            configMethodNames.add("writeToWAL");
            values.add(conf.get(TopologyLayoutConstants.JSON_KEY_WRITE_TO_WAL));
        }
        /*
         * configKey is mandatory for hbase bolt. The topology config is expected to contain
         * "hbaseConf" with the required hbase config.
         */
        configMethodNames.add("withConfigKey");
        values.add("hbaseConf");
        List configMethods = getConfigMethodsYaml(configMethodNames.toArray(new String[0]), values.toArray());
        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, configMethods);
        addParallelismToComponent();
    }

    private String addHbaseMapperComponent () {
        String hbaseMapperComponentId = "hbaseMapper" + UUID_FOR_COMPONENTS;

        // currently only ParserOutputHbaseMapper is supported.
        String hbaseMapperClassName = "org.apache.streamline.streams.runtime.storm.hbase.StreamlineEventHBaseMapper";

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
    public void validateConfig () throws ComponentConfigException {
        super.validateConfig();
        validateBooleanFields();
        validateStringFields();
    }

    private void validateBooleanFields () throws ComponentConfigException {
        String[] optionalBooleanFields = {
            TopologyLayoutConstants.JSON_KEY_WRITE_TO_WAL
        };
        validateBooleanFields(optionalBooleanFields, false);
    }

    private void validateStringFields () throws ComponentConfigException {
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
