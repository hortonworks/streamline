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
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.hortonworks.streamline.streams.cluster.discovery.ambari.ConfigFilePattern.CORE_SITE;
import static com.hortonworks.streamline.streams.cluster.discovery.ambari.ConfigFilePattern.HDFS_SITE;

public class HDFSServiceRegistrar extends AbstractServiceRegistrar {

    @Override
    protected String getServiceName() {
        return Constants.HDFS.SERVICE_NAME;
    }

    @Override
    protected List<Component> createComponents(Config config, Map<String, String> flattenConfigMap) {
        // no component to register
        return Collections.emptyList();
    }

    @Override
    protected List<ServiceConfiguration> createServiceConfigurations(Config config) {
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
        // 1. core-site.xml should be provided
        // 2. hdfs-site.xml should be provided

        long validConfigFileCount = serviceConfigurations.stream().filter(configuration -> {
            if (configuration.getName().equals(CORE_SITE.getConfType()) || configuration.getName().equals(HDFS_SITE.getConfType())) {
                if (!StringUtils.isEmpty(configuration.getFilename())) {
                    return true;
                }
            }
            return false;
        }).count();

        return validConfigFileCount == 2;
    }

    @Override
    protected boolean validateServiceConfiguationsAsFlattenedMap(Map<String, String> configMap) {
        return configMap.containsKey(Constants.HDFS.PROPERTY_KEY_DEFAULT_FS);
    }
}
