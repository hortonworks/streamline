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

import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.register.ServiceManualRegistrationDefinition;

import java.util.List;
import java.util.Map;

import static com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern.ZOOKEEPER_SERVER;

public class ZookeeperServiceRegisterer extends AbstractServiceRegisterer {

    public static final String SERVICE_NAME_ZOOKEEPER = "ZOOKEEPER";

    @Override
    protected String getServiceName() {
        return SERVICE_NAME_ZOOKEEPER;
    }

    @Override
    protected ServiceManualRegistrationDefinition getRegistrationDefinition() {
        return ServiceManualRegistrationDefinition.ZOOKEEPER;
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
    protected boolean validateServiceConfiguationsViaFlattenMap(Map<String, String> configMap) {
        // for now, every requirements will be checked from components
        return true;
    }
}
