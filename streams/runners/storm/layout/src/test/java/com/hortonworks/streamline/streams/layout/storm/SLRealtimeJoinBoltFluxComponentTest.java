/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.hortonworks.streamline.streams.layout.storm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class SLRealtimeJoinBoltFluxComponentTest {

    @Test
    public void testFluxGen_Count() throws Exception {
        SLRealtimeJoinBoltFluxComponent me = new SLRealtimeJoinBoltFluxComponent();
        String json = "{\n" +
                "\"from\" : {\"stream\": \"orders\", \"seconds\" : 10, \"unique\" : false },\n" +
                "\n" +
                "\"joins\" : [\n" +
                "    { \"type\":\"inner\",  \"stream\":\"adImpressions\",  \"seconds\":20,  \"unique\":true,\n" +
                "               \"conditions\" : [\n" +
                "                  [ \"equal\",  \"adImpressions:userID\",  \"orders:userId\" ],\n" +
                "                  [ \"ignoreCase\", \"product\", \"orders:product\"]\n" +
                "               ]\n" +
                "     }\n" +
                "  ],\n" +
                "\n" +
                "\"outputKeys\" : [ \"userID\", \"orders:product as product\" ,\"orderId\", \"impressionId\" ],\n" +
                "\"outputStream\" : \"joinedStream1\"\n" +
                "}";

        List<Map.Entry<String, Map<String, Object>>> map = getYamlComponents(json, me);
        String yamlStr = makeYaml(map);
        System.out.println(yamlStr);

    }

    public static List<Map.Entry<String, Map<String, Object>>> getYamlComponents(String json, FluxComponent fluxComponent) throws IOException {
        Map<String, Object> props = new ObjectMapper().readValue(json, new TypeReference<HashMap<String, Object>>(){});

        fluxComponent.withConfig(props);

        List<Map.Entry<String, Map<String, Object>>> keysAndComponents = new ArrayList<>();

        for (Map<String, Object> referencedComponent : fluxComponent.getReferencedComponents()) {
            keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_COMPONENTS, referencedComponent));
        }

        Map<String, Object> yamlComponent = fluxComponent.getComponent();
        yamlComponent.put(StormTopologyLayoutConstants.YAML_KEY_ID, "roshan");


        keysAndComponents.add( makeEntry(StormTopologyLayoutConstants.YAML_KEY_BOLTS,  yamlComponent) );

        return keysAndComponents;
    }


    private static Map.Entry<String, Map<String, Object>> makeEntry(String key, Map<String, Object> component) {
        return new AbstractMap.SimpleImmutableEntry<>(key, component);
    }

    private static String makeYaml(List<Map.Entry<String, Map<String, Object>>> keysAndComponents) {
        Map<String, Object> yamlMap;
        StringWriter yamlStr = new StringWriter();
        yamlMap = new LinkedHashMap<>();
        yamlMap.put(StormTopologyLayoutConstants.YAML_KEY_NAME, "topo1");

        for (Map.Entry<String, Map<String, Object>> entry: keysAndComponents) {
            addComponentToCollection(yamlMap, entry.getValue(), entry.getKey());
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setSplitLines(false);
        Yaml yaml = new Yaml (options);
        yaml.dump(yamlMap, yamlStr);
        return yamlStr.toString();
    }

    private static void addComponentToCollection (Map<String, Object> yamlMap, Map<String, Object> yamlComponent, String collectionKey) {
        if (yamlComponent == null ) {
            return;
        }

        List<Map<String, Object>> components = (List<Map<String, Object>>) yamlMap.get(collectionKey);
        if (components == null) {
            components = new ArrayList<>();
            yamlMap.put(collectionKey, components);
        }
        components.add(yamlComponent);
    }
}
