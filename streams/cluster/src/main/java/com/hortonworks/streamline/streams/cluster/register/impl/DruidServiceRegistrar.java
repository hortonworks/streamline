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
package com.hortonworks.streamline.streams.cluster.register.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DruidServiceRegistrar extends AbstractServiceRegistrar {

    public static final String PARAM_ZOOKEEPER_CONNECTION_STRING = "druid.zk.service.host";
    public static final String PARAM_INDEXING_SERVICE_NAME = "druid.selectors.indexing.serviceName";
    public static final String PARAM_DISCOVERY_CURATOR_PATH = "druid.discovery.curator.path";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected String getServiceName() {
        return Constants.Druid.SERVICE_NAME;
    }

    @Override
    protected Map<Component, List<ComponentProcess>> createComponents(Config config, Map<String, String> flattenConfigMap) {
        // no component to register
        return Collections.emptyMap();
    }

    @Override
    protected List<ServiceConfiguration> createServiceConfigurations(Config config) {
        return Collections.singletonList(buildCommonRuntimeServiceConfiguration(config));
    }

    @Override
    protected boolean validateComponents(Map<Component, List<ComponentProcess>> components) {
        // no need to check components
        return true;
    }

    @Override
    protected boolean validateServiceConfigurations(List<ServiceConfiguration> serviceConfigurations) {
        // requirements
        // 1. common.runtime.properties should be provided

        return serviceConfigurations.stream()
                .anyMatch(configuration -> configuration.getName().equals(Constants.Druid.CONF_TYPE_COMMON_RUNTIME));
    }

    @Override
    protected boolean validateServiceConfiguationsAsFlattenedMap(Map<String, String> configMap) {
        // only druid.zk.service.host is mandatory
        return configMap.containsKey(Constants.Druid.PROPERTY_KEY_ZK_SERVICE_HOSTS);
    }

    private ServiceConfiguration buildCommonRuntimeServiceConfiguration(Config config) {
        ServiceConfiguration commonRuntime = new ServiceConfiguration();
        commonRuntime.setName(Constants.Druid.CONF_TYPE_COMMON_RUNTIME);

        Map<String, String> confMap = new HashMap<>();

        if (config.contains(PARAM_ZOOKEEPER_CONNECTION_STRING)) {
            confMap.put(PARAM_ZOOKEEPER_CONNECTION_STRING, config.getString(PARAM_ZOOKEEPER_CONNECTION_STRING));
        }

        if (config.contains(PARAM_INDEXING_SERVICE_NAME)) {
            confMap.put(PARAM_INDEXING_SERVICE_NAME, config.getString(PARAM_INDEXING_SERVICE_NAME));
        }

        if (config.contains(PARAM_DISCOVERY_CURATOR_PATH)) {
            confMap.put(PARAM_DISCOVERY_CURATOR_PATH, config.getString(PARAM_DISCOVERY_CURATOR_PATH));
        }

        try {
            String json = objectMapper.writeValueAsString(confMap);
            commonRuntime.setConfiguration(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return commonRuntime;
    }
}
