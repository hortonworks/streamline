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
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class KafkaServiceRegistrar extends AbstractServiceRegistrar {
    public static final String COMPONENT_KAFKA_BROKER = ComponentPropertyPattern.KAFKA_BROKER.name();
    public static final String CONFIG_SERVER_PROPERTIES = ServiceConfigurations.KAFKA.getConfNames()[0];
    public static final String SERVICE_NAME_KAFKA = "KAFKA";
    public static final String KAFKA_PROPERTY_ZOOKEEPER_CONNECT = "zookeeper.connect";
    public static final String PARAM_KAFKA_BROKER_HOSTNAMES = "brokersHostnames";

    @Override
    protected String getServiceName() {
        return Constants.Kafka.SERVICE_NAME;
    }

    @Override
    protected List<Component> createComponents(Config config, Map<String, String> flattenConfigMap) {
        Component kafkaBroker = createKafkaBrokerComponent(config, flattenConfigMap);
        return Collections.singletonList(kafkaBroker);
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
    protected boolean validateServiceConfiguationsAsFlattenedMap(Map<String, String> configMap) {
        // requirements
        // 1. zookeeper.connect should be available in kafka-broker
        // if it exists, it should be within kafka-broker since we are allowing only one 'kafka-broker'
        return configMap.containsKey(Constants.Kafka.PROPERTY_KEY_ZOOKEEPER_CONNECT);
    }

    private Component createKafkaBrokerComponent(Config config, Map<String, String> flattenConfigMap) {
        if (!config.contains(PARAM_KAFKA_BROKER_HOSTNAMES)) {
            throw new IllegalArgumentException("Required parameter " + PARAM_KAFKA_BROKER_HOSTNAMES + " not present.");
        }

        List<String> kafkaBrokerHosts;
        try {
            String paramVal = config.getString(PARAM_KAFKA_BROKER_HOSTNAMES);
            kafkaBrokerHosts = Arrays.stream(paramVal.split(",")).collect(toList());
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Required parameter " + PARAM_KAFKA_BROKER_HOSTNAMES + " should be list of string.");
        }

        Component kafkaBroker = new Component();
        kafkaBroker.setName(COMPONENT_KAFKA_BROKER);
        kafkaBroker.setHosts(kafkaBrokerHosts);

        environmentService.injectProtocolAndPortToComponent(flattenConfigMap, kafkaBroker);
        return kafkaBroker;
    }
}
