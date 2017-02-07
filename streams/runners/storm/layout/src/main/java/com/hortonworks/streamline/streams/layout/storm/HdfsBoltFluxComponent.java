/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/
package com.hortonworks.streamline.streams.layout.storm;

import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.exception.ComponentConfigException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        List<String> configMethodArgRefs = new ArrayList<>();
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
        // currently only HdfsTextOutputFormat is supported.
        String recordFormatClassName
                = "com.hortonworks.streamline.streams.runtime.storm.hdfs.HdfsTextOutputFormat";

        List<Map<String, Object>> configMethods = new ArrayList<>();

        // setup the call to 'withFields()' config method
        Map<String, Object> withFieldsMethod = new LinkedHashMap<>();
        withFieldsMethod.put(StormTopologyLayoutConstants.YAML_KEY_NAME, "withFields");
        String outputFields = getCommaSepList( (List<String>) conf.get("outputFields") ) ;
        withFieldsMethod.put(StormTopologyLayoutConstants.YAML_KEY_ARGS, Arrays.asList(outputFields));
        configMethods.add(withFieldsMethod);

        addToComponents(createComponent(recordFormatComponentId,
                recordFormatClassName, null, null, configMethods));
        return recordFormatComponentId;
    }

    // returns a String with comma separated fields
    private static String getCommaSepList(List<String> fields) {
        return fields.stream().reduce( (a, b) -> a + "," + b ).get();
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
        String rotationPolicyClassName = null;
        Object[] constructorArgs = new Object[2];
        if (conf.get(TopologyLayoutConstants.JSON_KEY_ROTATION_POLICY) != null) {
            Map<String, Object> rotationPolicy = (Map<String, Object>) conf.get(TopologyLayoutConstants.JSON_KEY_ROTATION_POLICY);
            if (rotationPolicy.containsKey(TopologyLayoutConstants.JSON_KEY_TIME_BASED_ROTATION)) {
                rotationPolicyClassName = "org.apache.storm.hdfs.bolt.rotation.TimedRotationPolicy";
                rotationPolicy = (Map<String, Object>) rotationPolicy.get(TopologyLayoutConstants.JSON_KEY_TIME_BASED_ROTATION);
                constructorArgs[0] = rotationPolicy.get(TopologyLayoutConstants.JSON_KEY_ROTATION_INTERVAL);
                constructorArgs[1] = rotationPolicy.get(TopologyLayoutConstants.JSON_KEY_ROTATION_INTERVAL_UNIT);
            } else if (rotationPolicy.containsKey(TopologyLayoutConstants.JSON_KEY_SIZE_BASED_ROTATION)) {
                rotationPolicyClassName = "org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy";
                rotationPolicy = (Map<String, Object>) rotationPolicy.get(TopologyLayoutConstants.JSON_KEY_SIZE_BASED_ROTATION);
                constructorArgs[0] = rotationPolicy.get(TopologyLayoutConstants.JSON_KEY_ROTATION_SIZE);
                constructorArgs[1] = rotationPolicy.get(TopologyLayoutConstants.JSON_KEY_ROTATION_SIZE_UNIT);
            } else {
                throw new RuntimeException("Rotation policy not supported for hdfs bolt");
            }
        }
        addToComponents(createComponent(rotationPolicyComponentId,
                rotationPolicyClassName, null, getConstructorArgsYaml(constructorArgs), null));
        return rotationPolicyComponentId;
    }

    @Override
    public void validateConfig () throws ComponentConfigException {
        super.validateConfig();
        validateStringFields();
        validateIntegerFields();
        validateFloatOrDoubleFields();
    }

    private void validateStringFields () throws ComponentConfigException {
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

    private void validateIntegerFields () throws ComponentConfigException {
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

    private void validateFloatOrDoubleFields () throws ComponentConfigException {
        String[] requiredFields = {
            TopologyLayoutConstants.JSON_KEY_ROTATION_INTERVAL
        };
        validateFloatOrDoubleFields(requiredFields, true);
    }

}
