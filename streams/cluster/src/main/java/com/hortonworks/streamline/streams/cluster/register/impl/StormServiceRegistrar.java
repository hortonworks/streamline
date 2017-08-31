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
import com.google.common.collect.Lists;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import org.apache.commons.math3.util.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class StormServiceRegistrar extends AbstractServiceRegistrar {
    public static final String COMPONENT_STORM_UI_SERVER = ComponentPropertyPattern.STORM_UI_SERVER.name();
    public static final String COMPONENT_NIMBUS = ComponentPropertyPattern.NIMBUS.name();

    public static final String PARAM_NIMBUS_SEEDS = "nimbus.seeds";
    public static final String PARAM_NIMBUS_THRIFT_PORT = "nimbus.thrift.port";
    public static final String PARAM_UI_HOST = "ui.host";
    public static final String PARAM_UI_PORT = "ui.port";
    public static final String PARAM_NIMBUS_THRIFT_MAX_BUFFER_SIZE = "nimbus.thrift.max.buffer.size";
    public static final String PARAM_THRIFT_TRANSPORT = "storm.thrift.transport";
    public static final String PARAM_PRINCIPAL_TO_LOCAL = "storm.principal.tolocal";
    public static final String PARAM_NIMBUS_PRINCIPAL_NAME = "nimbus_principal_name";

    public static final String CONF_STORM = ServiceConfigurations.STORM.getConfNames()[0];
    public static final String CONF_STORM_ENV = ServiceConfigurations.STORM.getConfNames()[1];

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected String getServiceName() {
        return Constants.Storm.SERVICE_NAME;
    }

    @Override
    protected Map<Component, List<ComponentProcess>> createComponents(Config config, Map<String, String> flattenConfigMap) {
        Map<Component, List<ComponentProcess>> components = new HashMap<>();

        Pair<Component, List<ComponentProcess>> stormUIServer = createStormUIServerComponent(config, flattenConfigMap);
        Pair<Component, List<ComponentProcess>> nimbus = createNimbusComponent(config, flattenConfigMap);

        components.put(stormUIServer.getFirst(), stormUIServer.getSecond());
        components.put(nimbus.getFirst(), nimbus.getSecond());

        return components;
    }

    @Override
    protected List<ServiceConfiguration> createServiceConfigurations(Config config) {
        ServiceConfiguration storm = buildStormServiceConfiguration(config);
        ServiceConfiguration stormEnv = buildStormEnvServiceConfiguration(config);

        return Lists.newArrayList(storm, stormEnv);
    }

    @Override
    protected boolean validateComponents(Map<Component, List<ComponentProcess>> components) {
        // requirements
        // 1. STORM_UI_SERVER should be available, and it should have one host and one port
        // 2. NIMBUS should be available, and it should have one or multiple hosts and one port

        // filter out components which don't ensure requirements
        long filteredComponentCount = components.entrySet().stream().filter(componentEntry -> {
            Component component = componentEntry.getKey();
            List<ComponentProcess> componentProcesses = componentEntry.getValue();

            if (component.getName().equals(COMPONENT_STORM_UI_SERVER) || component.getName().equals(COMPONENT_NIMBUS)) {
                return isComponentProcessesValid(componentProcesses);
            } else {
                return false;
            }
        }).count();

        return filteredComponentCount == 2;
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

    private ServiceConfiguration buildStormServiceConfiguration(Config config) {
        ServiceConfiguration storm = new ServiceConfiguration();
        storm.setName(CONF_STORM);

        Map<String, String> confMap = new HashMap<>();

        if (config.contains(PARAM_NIMBUS_THRIFT_MAX_BUFFER_SIZE)) {
            Number thriftMaxBufferSize = readNumberFromConfig(config, PARAM_NIMBUS_THRIFT_MAX_BUFFER_SIZE);
            confMap.put(PARAM_NIMBUS_THRIFT_MAX_BUFFER_SIZE, String.valueOf(thriftMaxBufferSize));
        }

        if (config.contains(PARAM_THRIFT_TRANSPORT)) {
            confMap.put(PARAM_THRIFT_TRANSPORT, config.getString(PARAM_THRIFT_TRANSPORT));
        }

        if (config.contains(PARAM_PRINCIPAL_TO_LOCAL)) {
            confMap.put(PARAM_PRINCIPAL_TO_LOCAL, config.getString(PARAM_PRINCIPAL_TO_LOCAL));
        }

        try {
            String json = objectMapper.writeValueAsString(confMap);
            storm.setConfiguration(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return storm;
    }

    private ServiceConfiguration buildStormEnvServiceConfiguration(Config config) {
        Map<String, String> confMap;
        ServiceConfiguration stormEnv = new ServiceConfiguration();
        stormEnv.setName(CONF_STORM_ENV);

        confMap = new HashMap<>();

        if (config.contains(PARAM_NIMBUS_PRINCIPAL_NAME)) {
            confMap.put(PARAM_NIMBUS_PRINCIPAL_NAME, config.get(PARAM_NIMBUS_PRINCIPAL_NAME));
        }

        try {
            String json = objectMapper.writeValueAsString(confMap);
            stormEnv.setConfiguration(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return stormEnv;
    }

    private Pair<Component, List<ComponentProcess>> createNimbusComponent(Config config, Map<String, String> flatConfigMap) {
        if (!config.contains(PARAM_NIMBUS_SEEDS)) {
            throw new IllegalArgumentException("Required parameter " + PARAM_NIMBUS_SEEDS + " not present.");
        }

        if (!config.contains(PARAM_NIMBUS_THRIFT_PORT)) {
            throw new IllegalArgumentException("Required parameter " + PARAM_NIMBUS_THRIFT_PORT + " not present.");
        }

        String nimbusSeeds;
        try {
            nimbusSeeds = config.getString(PARAM_NIMBUS_SEEDS);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Required parameter " + PARAM_NIMBUS_SEEDS + " should be a string.");
        }

        Number nimbusThriftPort = readNumberFromConfig(config, PARAM_NIMBUS_THRIFT_PORT);

        Component nimbus = new Component();
        nimbus.setName(COMPONENT_NIMBUS);

        List<ComponentProcess> componentProcesses = Arrays.stream(nimbusSeeds.split(",")).map(nimbusHost -> {
            ComponentProcess cp = new ComponentProcess();
            cp.setHost(nimbusHost);
            cp.setPort(nimbusThriftPort.intValue());
            return cp;
        }).collect(toList());

        return new Pair<>(nimbus, componentProcesses);
    }

    private Pair<Component, List<ComponentProcess>> createStormUIServerComponent(Config config, Map<String, String> flattenConfigMap) {
        if (!config.contains(PARAM_UI_HOST)) {
            throw new IllegalArgumentException("Required parameter " + PARAM_UI_HOST + " not present.");
        }

        if (!config.contains(PARAM_UI_PORT)) {
            throw new IllegalArgumentException("Required parameter " + PARAM_UI_PORT + " not present.");
        }

        String stormUiServerHost;
        try {
            stormUiServerHost = config.getString(PARAM_UI_HOST);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Required parameter " + PARAM_UI_HOST + " should be a string.");
        }

        Number stormUiServerPort = readNumberFromConfig(config, PARAM_UI_PORT);

        Component stormUiServer = new Component();
        stormUiServer.setName(COMPONENT_STORM_UI_SERVER);

        ComponentProcess uiProcess = new ComponentProcess();
        uiProcess.setHost(stormUiServerHost);
        uiProcess.setPort(stormUiServerPort.intValue());

        return new Pair<>(stormUiServer, Collections.singletonList(uiProcess));
    }

    private Number readNumberFromConfig(Config config, String parameterName) {
        try {
            return config.getAny(parameterName);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Required parameter " + parameterName + " should be a number.");
        }
    }

}
