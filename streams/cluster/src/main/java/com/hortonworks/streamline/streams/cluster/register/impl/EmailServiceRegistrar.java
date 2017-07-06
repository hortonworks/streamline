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
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern.ZOOKEEPER_SERVER;

public class EmailServiceRegistrar extends AbstractServiceRegistrar {
    public static final String PARAM_HOST = "host";
    public static final String PARAM_PORT = "port";
    public static final String PARAM_SSL = "ssl";
    public static final String PARAM_STARTTLS = "starttls";
    public static final String PARAM_PROTOCOL = "protocol";
    public static final String PARAM_AUTH = "auth";

    @Override
    protected String getServiceName() {
        return Constants.Email.SERVICE_NAME;
    }

    @Override
    protected Map<Component, List<ComponentProcess>> createComponents(Config config, Map<String, String> flattenConfigMap) {
        return Collections.emptyMap();
    }

    @Override
    protected List<ServiceConfiguration> createServiceConfigurations(Config config) {
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        serviceConfiguration.setName(Constants.Email.CONF_TYPE_PROPERTIES);

        Map<String, String> confMap = new HashMap<>();
        confMap.put(PARAM_HOST, config.get(Constants.Email.PROPERTY_KEY_HOST));
        confMap.put(PARAM_PORT, String.valueOf((Integer) config.getAny(Constants.Email.PROPERTY_KEY_PORT)));
        confMap.put(PARAM_SSL, String.valueOf((Boolean) config.getAny(Constants.Email.PROPERTY_KEY_SSL)));
        confMap.put(PARAM_STARTTLS, String.valueOf((Boolean) config.getAny(Constants.Email.PROPERTY_KEY_STARTTLS)));
        confMap.put(PARAM_PROTOCOL, config.get(Constants.Email.PROPERTY_KEY_PROTOCOL));
        confMap.put(PARAM_AUTH, String.valueOf((Boolean) config.getAny(Constants.Email.PROPERTY_KEY_AUTH)));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(confMap);
            serviceConfiguration.setConfiguration(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return Collections.singletonList(serviceConfiguration);
    }

    @Override
    protected boolean validateComponents(Map<Component, List<ComponentProcess>> components) {
        // no component required, we will just use properties
        return true;
    }

    @Override
    protected boolean validateServiceConfigurations(List<ServiceConfiguration> serviceConfigurations) {
        return serviceConfigurations.stream()
                .anyMatch(config -> config.getName().equals(Constants.Email.CONF_TYPE_PROPERTIES));
    }

    @Override
    protected boolean validateServiceConfiguationsAsFlattenedMap(Map<String, String> configMap) {
        // all fields are optional for hint provider
        return true;
    }

}
