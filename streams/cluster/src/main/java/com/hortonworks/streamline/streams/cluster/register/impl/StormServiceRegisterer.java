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

import com.google.common.collect.Lists;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class StormServiceRegisterer extends AbstractServiceRegisterer {
    public static final String COMPONENT_STORM_UI_SERVER = ComponentPropertyPattern.STORM_UI_SERVER.name();
    public static final String COMPONENT_NIMBUS = ComponentPropertyPattern.NIMBUS.name();
    public static final String SERVICE_NAME_STORM = "STORM";
    public static final String PARAM_STORM_UI_SERVER_HOSTNAME = "uiServerHostname";
    public static final String PARAM_NIMBUS_HOSTNAMES = "nimbusesHostnames";

    @Override
    protected String getServiceName() {
        return SERVICE_NAME_STORM;
    }

    @Override
    protected List<Component> createComponents(Config config, Map<String, String> flattenConfigMap) {
        Component stormUIServer = createStormUIServerComponent(config, flattenConfigMap);
        Component nimbus = createNimbusComponent(config, flattenConfigMap);

        return Lists.newArrayList(stormUIServer, nimbus);
    }

    @Override
    protected boolean validateComponents(List<Component> components) {
        // requirements
        // 1. STORM_UI_SERVER should be available, and it should have one host and one port
        // 2. NIMBUS should be available, and it should have one or multiple hosts and one port

        // filter out components which don't ensure requirements
        long filteredComponentCount = components.stream().filter(component -> {
            boolean validComponent = true;
            if (component.getName().equals(COMPONENT_STORM_UI_SERVER)) {
                if (component.getHosts().size() <= 0 || component.getHosts().size() > 1 || component.getPort() == null) {
                    validComponent = false;
                }
            } else if (component.getName().equals(COMPONENT_NIMBUS)) {
                // check hosts and port
                if (component.getHosts().size() <= 0 || component.getPort() == null) {
                    validComponent = false;
                }
            } else {
                validComponent = false;
            }
            return validComponent;
        }).count();

        return filteredComponentCount == 2;
    }

    @Override
    protected boolean validateServiceConfigurations(List<ServiceConfiguration> serviceConfigurations) {
        // for now, every requirements will be checked from components
        return true;
    }

    @Override
    protected boolean validateServiceConfiguationsViaFlattenMap(Map<String, String> configMap) {
        // for now, every requirements will be checked from components
        return true;
    }

    private Component createStormUIServerComponent(Config config, Map<String, String> flattenConfigMap) {
        if (!config.contains(PARAM_STORM_UI_SERVER_HOSTNAME)) {
            throw new IllegalArgumentException("Required parameter " + PARAM_STORM_UI_SERVER_HOSTNAME + " not presented.");
        }

        String stormUiServerHost;
        try {
            stormUiServerHost = config.getString(PARAM_STORM_UI_SERVER_HOSTNAME);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Required parameter " + PARAM_STORM_UI_SERVER_HOSTNAME + " should be string.");
        }

        Component stormUiServer = new Component();
        stormUiServer.setName(COMPONENT_STORM_UI_SERVER);
        stormUiServer.setHosts(Collections.singletonList(stormUiServerHost));
        environmentService.injectProtocolAndPortToComponent(flattenConfigMap, stormUiServer);
        return stormUiServer;
    }

    private Component createNimbusComponent(Config config, Map<String, String> flattenConfigMap) {
        if (!config.contains(PARAM_NIMBUS_HOSTNAMES)) {
            throw new IllegalArgumentException("Required parameter " + PARAM_NIMBUS_HOSTNAMES + " not presented.");
        }

        List<String> stormNimbusServerHosts;
        try {
            String paramVal = config.getString(PARAM_NIMBUS_HOSTNAMES);
            stormNimbusServerHosts = Arrays.stream(paramVal.split(",")).collect(toList());
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Required parameter " + PARAM_NIMBUS_HOSTNAMES + " should be list of string.");
        }

        Component nimbus = new Component();
        nimbus.setName(COMPONENT_NIMBUS);
        nimbus.setHosts(stormNimbusServerHosts);
        environmentService.injectProtocolAndPortToComponent(flattenConfigMap, nimbus);
        return nimbus;
    }

}
