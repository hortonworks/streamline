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
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaBrokerListeners;
import org.apache.commons.math3.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class KafkaServiceRegistrar extends AbstractServiceRegistrar {
    public static final String COMPONENT_KAFKA_BROKER = ComponentPropertyPattern.KAFKA_BROKER.name();
    public static final String CONFIG_SERVER_PROPERTIES = ServiceConfigurations.KAFKA.getConfNames()[0];
    public static final String CONFIG_KAFKA_ENV = ServiceConfigurations.KAFKA.getConfNames()[1];

    public static final String PARAM_ZOOKEEPER_CONNECT = "zookeeper.connect";
    // Manual Kafka registrar determines brokers via parsing listeners
    public static final String PARAM_LISTENERS = "listeners";
    public static final String PARAM_SECURITY_INTER_BROKER_PROTOCOL = "security.inter.broker.protocol";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected String getServiceName() {
        return Constants.Kafka.SERVICE_NAME;
    }

    @Override
    protected Map<Component, List<ComponentProcess>> createComponents(Config config, Map<String, String> flattenConfigMap) {
        Map<Component, List<ComponentProcess>> components = new HashMap<>();

        Pair<Component, List<ComponentProcess>> kafkaBroker = createKafkaBrokerComponent(config, flattenConfigMap);
        components.put(kafkaBroker.getFirst(), kafkaBroker.getSecond());

        return components;
    }

    @Override
    protected List<ServiceConfiguration> createServiceConfigurations(Config config) {
        ServiceConfiguration serverProperties = buildServerPropertiesServiceConfiguration(config);
        ServiceConfiguration kafkaEnvProperties = buildKafkaEnvServiceConfiguration(config);
        return Lists.newArrayList(serverProperties, kafkaEnvProperties);
    }

    @Override
    protected boolean validateComponents(Map<Component, List<ComponentProcess>> components) {
        // requirements
        // 1. KAFKA_BROKER should be available, and all broker process should have host and port, and protocol

        return components.entrySet().stream().anyMatch(componentEntry -> {
            Component component = componentEntry.getKey();
            List<ComponentProcess> componentProcesses = componentEntry.getValue();

            if (component.getName().equals(COMPONENT_KAFKA_BROKER)) {
                return isComponentProcessesWithProtocolRequiredValid(componentProcesses);
            }

            return false;
        });
    }

    @Override
    protected boolean validateServiceConfigurations(List<ServiceConfiguration> serviceConfigurations) {
        // requirements: it should be only one: 'service'
        long validConfigFileCount = serviceConfigurations.stream().filter(configuration -> {
            if (configuration.getName().equals(CONFIG_SERVER_PROPERTIES) || configuration.getName().equals(CONFIG_KAFKA_ENV)) {
                return true;
            }
            return false;
        }).count();

        return validConfigFileCount == 2;
    }

    @Override
    protected boolean validateServiceConfiguationsAsFlattenedMap(Map<String, String> configMap) {
        // requirements
        // 1. zookeeper.connect should be available in kafka-broker
        // if it exists, it should be within kafka-broker since we are allowing only one 'kafka-broker'
        return configMap.containsKey(Constants.Kafka.PROPERTY_KEY_ZOOKEEPER_CONNECT);
    }

    private ServiceConfiguration buildServerPropertiesServiceConfiguration(Config config) {
        ServiceConfiguration serverProperties = new ServiceConfiguration();
        serverProperties.setName(CONFIG_SERVER_PROPERTIES);

        Map<String, String> confMap = new HashMap<>();

        if (config.contains(PARAM_ZOOKEEPER_CONNECT)) {
            confMap.put(PARAM_ZOOKEEPER_CONNECT, config.getString(PARAM_ZOOKEEPER_CONNECT));
        }

        if (config.contains(PARAM_SECURITY_INTER_BROKER_PROTOCOL)) {
            confMap.put(PARAM_SECURITY_INTER_BROKER_PROTOCOL, config.getString(PARAM_SECURITY_INTER_BROKER_PROTOCOL));
        }

        try {
            String json = objectMapper.writeValueAsString(confMap);
            serverProperties.setConfiguration(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return serverProperties;
    }

    private ServiceConfiguration buildKafkaEnvServiceConfiguration(Config config) {
        ServiceConfiguration serverProperties = new ServiceConfiguration();
        serverProperties.setName(CONFIG_KAFKA_ENV);

        Map<String, String> confMap = new HashMap<>();

        try {
            String json = objectMapper.writeValueAsString(confMap);
            serverProperties.setConfiguration(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return serverProperties;
    }

    private Pair<Component, List<ComponentProcess>> createKafkaBrokerComponent(Config config, Map<String, String> flattenConfigMap) {
        if (!config.contains(PARAM_LISTENERS)) {
            throw new IllegalArgumentException("Required parameter " + PARAM_LISTENERS + " not present.");
        }

        Map<String, String> confMap = new HashMap<>();
        confMap.put(PARAM_LISTENERS, config.getString(PARAM_LISTENERS));

        Component kafkaBroker = new Component();
        kafkaBroker.setName(COMPONENT_KAFKA_BROKER);

        List<KafkaBrokerListeners.ListenersPropEntry> parsedProps = new KafkaBrokerListeners
                .ListenersPropParsed(confMap).getParsedProps();

        List<ComponentProcess> componentProcesses = parsedProps.stream().map(propEntry -> {
            ComponentProcess cp = new ComponentProcess();
            cp.setHost(propEntry.getHost());
            cp.setPort(propEntry.getPort());
            cp.setProtocol(propEntry.getProtocol().name());
            return cp;
        }).collect(toList());

        return new Pair<>(kafkaBroker, componentProcesses);
    }
}
