/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streamline.streams.layout.storm;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;

import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 *
 */
public class FluxBoltGenerator {

    public Map<String, Object> generateYaml(FluxComponent fluxComponent) {
        List<Map.Entry<String, Map<String, Object>>> keysAndComponents = new ArrayList<>();

        for (Map<String, Object> referencedComponent : fluxComponent.getReferencedComponents()) {
            keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_COMPONENTS, referencedComponent));
        }

        Map<String, Object> yamlComponent = fluxComponent.getComponent();
        yamlComponent.put(StormTopologyLayoutConstants.YAML_KEY_ID, "bolt-" + UUID.randomUUID());

        keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_BOLTS, yamlComponent));

        Map<String, Object> yamlMap = new LinkedHashMap<>();
        yamlMap.put(StormTopologyLayoutConstants.YAML_KEY_NAME, "topology");

        for (Map.Entry<String, Map<String, Object>> entry : keysAndComponents) {
            addComponentToCollection(yamlMap, entry.getValue(), entry.getKey());
        }

        return yamlMap;
    }

    public String toYamlString(Map<String, Object> yamlMap) {
        StringWriter yamlStr = new StringWriter();
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setSplitLines(true);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        yaml.dump(yamlMap, yamlStr);

        return yamlStr.toString();
    }

    private static void addComponentToCollection(Map<String, Object> yamlMap, Map<String, Object> yamlComponent, String collectionKey) {
        if (yamlComponent == null) {
            return;
        }

        List<Map<String, Object>> components = (List<Map<String, Object>>) yamlMap.get(collectionKey);
        if (components == null) {
            components = new ArrayList<>();
            yamlMap.put(collectionKey, components);
        }
        components.add(yamlComponent);
    }

    private static Map.Entry<String, Map<String, Object>> makeEntry(String key, Map<String, Object> component) {
        return new AbstractMap.SimpleImmutableEntry<>(key, component);
    }
}
