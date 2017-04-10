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

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern.ZOOKEEPER_SERVER;
import static java.util.stream.Collectors.toList;

public class ZookeeperServiceRegistrar extends AbstractServiceRegistrar {
    public static final String COMPONENT_ZOOKEEPER_SERVER = ComponentPropertyPattern.ZOOKEEPER_SERVER.name();
    public static final String PARAM_ZOOKEEPER_SERVER_HOSTNAMES = "zkServersHostnames";

    @Override
    protected String getServiceName() {
        return Constants.Zookeeper.SERVICE_NAME;
    }

    @Override
    protected List<Component> createComponents(Config config, Map<String, String> flattenConfigMap) {
        Component zookeeperServer = createZookeeperServerComponent(config, flattenConfigMap);

        return Collections.singletonList(zookeeperServer);
    }

    @Override
    protected boolean validateComponents(List<Component> components) {
        // requirements
        // 1. ZOOKEEPER_SERVER should be available, and it should have one or more hosts and one port
        return components.stream().anyMatch(component -> {
            if (component.getName().equals(ZOOKEEPER_SERVER.name())) {
                if (component.getHosts().size() > 0 && component.getPort() != null) {
                    return true;
                }
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

    private Component createZookeeperServerComponent(Config config, Map<String, String> flattenConfigMap) {
        if (!config.contains(PARAM_ZOOKEEPER_SERVER_HOSTNAMES)) {
            throw new IllegalArgumentException("Required parameter " + PARAM_ZOOKEEPER_SERVER_HOSTNAMES + " not present.");
        }

        List<String> zookeeperServerHosts;
        try {
            zookeeperServerHosts = config.getAny(PARAM_ZOOKEEPER_SERVER_HOSTNAMES);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Required parameter " + PARAM_ZOOKEEPER_SERVER_HOSTNAMES + " should be list of string.");
        }

        Component zookeeperServer = new Component();
        zookeeperServer.setName(COMPONENT_ZOOKEEPER_SERVER);
        zookeeperServer.setHosts(zookeeperServerHosts);

        environmentService.injectProtocolAndPortToComponent(flattenConfigMap, zookeeperServer);
        return zookeeperServer;
    }
}
