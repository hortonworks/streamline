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
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.register.ServiceManualRegistrationDefinition;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public class KafkaServiceRegisterer extends AbstractServiceRegisterer {
    public static final String COMPONENT_KAFKA_BROKER = ComponentPropertyPattern.KAFKA_BROKER.name();
    public static final String CONFIG_SERVER_PROPERTIES = ServiceConfigurations.KAFKA.getConfNames()[0];
    public static final String SERVICE_NAME_KAFKA = "KAFKA";
    public static final String KAFKA_PROPERTY_ZOOKEEPER_CONNECT = "zookeeper.connect";

    @Override
    protected String getServiceName() {
        return SERVICE_NAME_KAFKA;
    }

    @Override
    protected ServiceManualRegistrationDefinition getRegistrationDefinition() {
        return ServiceManualRegistrationDefinition.KAFKA;
    }

    @Override
    protected boolean validateComponents(List<Component> components) {
        // requirements
        // 1. KAFKA_BROKER should be available, and it should have one or more hosts and one port, and protocol

        return components.stream().anyMatch(component -> {
            if (component.getName().equals(COMPONENT_KAFKA_BROKER)) {
                if (component.getHosts().size() > 0 && component.getPort() != null &&
                        !StringUtils.isEmpty(component.getProtocol())) {
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    protected boolean validateServiceConfigurations(List<ServiceConfiguration> serviceConfigurations) {
        // requirements: it should be only one: 'service'
        if (serviceConfigurations.size() != 1) {
            return false;
        }
        return serviceConfigurations.get(0).getName().equals(CONFIG_SERVER_PROPERTIES);
    }

    @Override
    protected boolean validateServiceConfiguationsViaFlattenMap(Map<String, String> configMap) {
        // requirements
        // 1. zookeeper.connect should be available in kafka-broker
        // if it exists, it should be within kafka-broker since we are allowing only one 'kafka-broker'
        return configMap.containsKey(KAFKA_PROPERTY_ZOOKEEPER_CONNECT);
    }
}
