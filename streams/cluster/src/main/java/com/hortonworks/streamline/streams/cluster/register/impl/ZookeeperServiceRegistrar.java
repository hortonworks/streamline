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
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import org.apache.commons.math3.util.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern.ZOOKEEPER_SERVER;
import static java.util.stream.Collectors.toList;

public class ZookeeperServiceRegistrar extends AbstractServiceRegistrar {
    public static final String CONFIG_ZOO_CFG = ServiceConfigurations.ZOOKEEPER.getConfNames()[0];

    public static final String COMPONENT_ZOOKEEPER_SERVER = ComponentPropertyPattern.ZOOKEEPER_SERVER.name();
    public static final String PARAM_ZOOKEEPER_SERVER_HOSTNAMES = "zkServersHostnames";
    public static final String PARAM_ZOOKEEPER_PORT = "clientPort";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected String getServiceName() {
        return Constants.Zookeeper.SERVICE_NAME;
    }

    @Override
    protected Map<Component, List<ComponentProcess>> createComponents(Config config, Map<String, String> flattenConfigMap) {
        Map<Component, List<ComponentProcess>> components = new HashMap<>();

        Pair<Component, List<ComponentProcess>> zookeeperServer = createZookeeperServerComponent(config, flattenConfigMap);
        components.put(zookeeperServer.getFirst(), zookeeperServer.getSecond());

        return components;
    }

    @Override
    protected List<ServiceConfiguration> createServiceConfigurations(Config config) {
        return Collections.singletonList(buildZooCfgServiceConfiguration(config));
    }

    @Override
    protected boolean validateComponents(Map<Component, List<ComponentProcess>> components) {
        // requirements
        // 1. ZOOKEEPER_SERVER should be available, and it should have one or more hosts and one port
        return components.entrySet().stream().anyMatch(componentEntry -> {
            Component component = componentEntry.getKey();
            List<ComponentProcess> componentProcesses = componentEntry.getValue();

            if (component.getName().equals(ZOOKEEPER_SERVER.name())) {
                return isComponentProcessesValid(componentProcesses);
            }
            return false;
        });
    }

    @Override
    protected boolean validateServiceConfigurations(List<ServiceConfiguration> serviceConfigurations) {
        // for now, every requirements will be checked from components
        return true;
    }

    @Override
    protected boolean validateServiceConfiguationsAsFlattenedMap(Map<String, String> configMap) {
        // for now, every requirements will be checked from components
        return true;
    }

    private Pair<Component, List<ComponentProcess>> createZookeeperServerComponent(Config config, Map<String, String> flattenConfigMap) {
        if (!config.contains(PARAM_ZOOKEEPER_SERVER_HOSTNAMES)) {
            throw new IllegalArgumentException("Required parameter " + PARAM_ZOOKEEPER_SERVER_HOSTNAMES + " not present.");
        }

        if (!config.contains(PARAM_ZOOKEEPER_PORT)) {
            throw new IllegalArgumentException("Required parameter " + PARAM_ZOOKEEPER_PORT + " not present.");
        }

        List<String> zookeeperServerHosts;
        try {
            zookeeperServerHosts = config.getAny(PARAM_ZOOKEEPER_SERVER_HOSTNAMES);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Required parameter " + PARAM_ZOOKEEPER_SERVER_HOSTNAMES + " should be list of string.");
        }

        Number zookeeperPort;
        try {
            zookeeperPort = config.getAny(PARAM_ZOOKEEPER_PORT);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Required parameter " + PARAM_ZOOKEEPER_PORT + " should be number.");
        }

        Component zookeeperServer = new Component();
        zookeeperServer.setName(COMPONENT_ZOOKEEPER_SERVER);

        List<ComponentProcess> componentProcesses = zookeeperServerHosts.stream().map(host -> {
            ComponentProcess cp = new ComponentProcess();
            cp.setHost(host);
            cp.setPort(zookeeperPort.intValue());
            return cp;
        }).collect(toList());

        return new Pair<>(zookeeperServer, componentProcesses);
    }

    private ServiceConfiguration buildZooCfgServiceConfiguration(Config config) {
        ServiceConfiguration zooCfg = new ServiceConfiguration();
        zooCfg.setName(CONFIG_ZOO_CFG);

        Map<String, String> confMap = new HashMap<>();

        if (config.contains(PARAM_ZOOKEEPER_PORT)) {
            Number zookeeperPort = config.getAny(PARAM_ZOOKEEPER_PORT);
            confMap.put(PARAM_ZOOKEEPER_PORT, String.valueOf(zookeeperPort));
        }

        try {
            String json = objectMapper.writeValueAsString(confMap);
            zooCfg.setConfiguration(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return zooCfg;
    }
}
