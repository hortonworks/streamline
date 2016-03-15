package com.hortonworks.iotas.topology.storm;

import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.util.exception.BadTopologyLayoutException;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for HdfsBolt
 */
public class HdfsBoltFluxComponent extends AbstractFluxComponent {

    @Override
    protected void generateComponent () {
        String boltId = "hdfsBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "org.apache.storm.hdfs.bolt.HdfsBolt";

        String[] configMethodNames = {"withFsUrl", "withConfigKey"};
        String[] configKeys = {
                TopologyLayoutConstants.JSON_KEY_FS_URL,
                TopologyLayoutConstants.JSON_KEY_CONFIG_KEY
        };
        List configMethods = getConfigMethodsYaml(configMethodNames,
                configKeys);
        String[] moreConfigMethodNames = {
            "withFileNameFormat",
             "withRecordFormat",
             "withSyncPolicy",
             "withRotationPolicy"
        };
        List<String> configMethodArgRefs = new ArrayList<String>();
        configMethodArgRefs.add(addFileNameFormatComponent());
        configMethodArgRefs.add(addRecordFormatComponent());
        configMethodArgRefs.add(addSyncPolicyComponent());
        configMethodArgRefs.add(addRotationPolicyComponent());
        configMethods.addAll(getConfigMethodWithRefArg(moreConfigMethodNames,
                configMethodArgRefs.toArray(new String[configMethodArgRefs
                        .size()])));
        component = createComponent(boltId, boltClassName, null, null,
                configMethods);
        // TODO: addRotationActions
        addParallelismToComponent();
    }



    private String addFileNameFormatComponent () {
        String fileNameFormatComponentId = "fileNameFormat" +
                UUID_FOR_COMPONENTS;
        // currently only DefaultFileNameFormat is supported.
        String fileNameFormatClassName = "org.apache.storm.hdfs.bolt.format" +
                ".DefaultFileNameFormat";
        String[] configMethodNames = {"withPath", "withPrefix",
                "withExtension"};
        String[] configKeys = {
                TopologyLayoutConstants.JSON_KEY_PATH,
                TopologyLayoutConstants.JSON_KEY_PREFIX,
                TopologyLayoutConstants.JSON_KEY_EXTENSION
        };
        List configMethods = getConfigMethodsYaml(configMethodNames,
                configKeys);
        addToComponents(createComponent(fileNameFormatComponentId,
                fileNameFormatClassName, null, null, configMethods));
        return fileNameFormatComponentId;
    }

    private String addRecordFormatComponent () {
        String recordFormatComponentId = "recordFormat" +
                UUID_FOR_COMPONENTS;
        // currently only IdentityHdfsRecordFormat is supported.
        String recordFormatClassName = "com.hortonworks.iotas.hdfs" +
                ".IdentityHdfsRecordFormat";
        addToComponents(createComponent(recordFormatComponentId,
                recordFormatClassName, null, null, null));
        return recordFormatComponentId;
    }

    private String addSyncPolicyComponent () {
        String syncPolicyComponentId = "syncPolicy" +
                UUID_FOR_COMPONENTS;
        // currently only CountSyncPolicy is supported.
        String syncPolicyClassName = "org.apache.storm.hdfs.bolt.sync" +
                ".CountSyncPolicy";
        String[] constructorArgNames = {
                TopologyLayoutConstants.JSON_KEY_COUNT_POLICY_VALUE
        };
        List constructorArgs = getConstructorArgsYaml(constructorArgNames);
        addToComponents(createComponent(syncPolicyComponentId,
                syncPolicyClassName, null, constructorArgs, null));
        return syncPolicyComponentId;
    }

    private String addRotationPolicyComponent () {
        String rotationPolicyComponentId = "rotationPolicy" +
                UUID_FOR_COMPONENTS;
        // currently only TimedRotationPolicy is supported. Add check in
        // validation
        String rotationPolicyClassName = "org.apache.storm.hdfs.bolt.rotation" +
                ".TimedRotationPolicy";
        String[] constructorArgNames = {
            TopologyLayoutConstants.JSON_KEY_ROTATION_INTERVAL,
            TopologyLayoutConstants.JSON_KEY_ROTATION_INTERVAL_UNIT
        };
        List constructorArgs = getConstructorArgsYaml(constructorArgNames);
        addToComponents(createComponent(rotationPolicyComponentId,
                rotationPolicyClassName, null, constructorArgs, null));
        return rotationPolicyComponentId;
    }

    @Override
    public void validateConfig () throws BadTopologyLayoutException {
        super.validateConfig();
        validateStringFields();
        validateIntegerFields();
        validateFloatOrDoubleFields();
    }

    private void validateStringFields () throws BadTopologyLayoutException {
        String[] requiredStringFields = {
            TopologyLayoutConstants.JSON_KEY_FS_URL,
            TopologyLayoutConstants.JSON_KEY_ROTATION_INTERVAL_UNIT
        };
        validateStringFields(requiredStringFields, true);
        String[] optionalStringFields = {
            TopologyLayoutConstants.JSON_KEY_CONFIG_KEY,
            TopologyLayoutConstants.JSON_KEY_PATH,
            TopologyLayoutConstants.JSON_KEY_PREFIX,
            TopologyLayoutConstants.JSON_KEY_EXTENSION
        };
        validateStringFields(optionalStringFields, false);
    }

    private void validateIntegerFields () throws BadTopologyLayoutException {
        String[] requiredIntegerFields = {
            TopologyLayoutConstants.JSON_KEY_COUNT_POLICY_VALUE
        };
        Integer[] mins = {
            1
        };
        Integer[] maxes = {
            Integer.MAX_VALUE
        };
        validateIntegerFields(requiredIntegerFields, true, mins, maxes);
    }

    private void validateFloatOrDoubleFields () throws BadTopologyLayoutException {
        String[] requiredFields = {
            TopologyLayoutConstants.JSON_KEY_ROTATION_INTERVAL
        };
        validateFloatOrDoubleFields(requiredFields, true);
    }

}
