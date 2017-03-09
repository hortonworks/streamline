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

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DruidServiceRegistrar extends AbstractServiceRegistrar {
    @Override
    protected String getServiceName() {
        return Constants.Druid.SERVICE_NAME;
    }

    @Override
    protected List<Component> createComponents(Config config, Map<String, String> flattenConfigMap) {
        // no component to register
        return Collections.emptyList();
    }

    @Override
    protected boolean validateComponents(List<Component> components) {
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
}
