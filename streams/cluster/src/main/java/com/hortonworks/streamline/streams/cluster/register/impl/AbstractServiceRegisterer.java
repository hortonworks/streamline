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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.catalog.configuration.ConfigFileReader;
import com.hortonworks.streamline.streams.catalog.configuration.ConfigFileType;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ConfigFilePattern;
import com.hortonworks.streamline.streams.cluster.register.ManualServiceRegisterer;
import com.hortonworks.streamline.streams.cluster.register.ServiceManualRegistrationDefinition;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractServiceRegisterer implements ManualServiceRegisterer {
    private EnvironmentService environmentService;

    protected abstract String getServiceName();

    protected abstract ServiceManualRegistrationDefinition getRegistrationDefinition();

    protected abstract boolean validateComponents(List<Component> components);

    protected abstract boolean validateServiceConfigurations(List<ServiceConfiguration> serviceConfigurations);

    protected abstract boolean validateServiceConfiguationsViaFlattenMap(Map<String, String> configMap);

    @Override
    public void init(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public Service register(Cluster cluster, List<ComponentInfo> componentInfos, List<ConfigFileInfo> configFileInfos) throws IOException {
        assertInputByDefinition(componentInfos, configFileInfos);

        Service service = environmentService.initializeService(cluster, getServiceName());

        List<Component> components = new ArrayList<>();
        List<ServiceConfiguration> configurations = new ArrayList<>();
        Map<String, String> flattenConfigMap = new HashMap<>();

        ObjectMapper objectMapper = new ObjectMapper();
        for (ConfigFileInfo configFileInfo : configFileInfos) {
            Map<String, String> configMap = readConfigFile(configFileInfo);

            String fileName = FilenameUtils.getName(configFileInfo.getFileName());
            String confType = getConfType(fileName);
            String actualFileName = ConfigFilePattern.getOriginFileName(confType);

            ServiceConfiguration configuration = environmentService.initializeServiceConfiguration(objectMapper,
                    service.getId(), confType, actualFileName, new HashMap<>(configMap));
            configurations.add(configuration);
            flattenConfigMap.putAll(configMap);
        }

        for (ComponentInfo componentInfo : componentInfos) {
            try {
                // below 'valueOf' call is for checking existence. don't remove them
                ComponentPropertyPattern.valueOf(componentInfo.getName());
            } catch (IllegalArgumentException e) {
                // no need to handle
                continue;
            }

            Component component = environmentService.initializeComponent(service, componentInfo.getName(),
                    componentInfo.getHosts());
            environmentService.injectProtocolAndPortToComponent(flattenConfigMap, component);

            components.add(component);
        }

        if (!validateComponents(components)) {
            throw new IllegalArgumentException("Validation failed for components.");
        }

        if (!validateServiceConfigurations(configurations)) {
            throw new IllegalArgumentException("Validation failed for service configurations.");
        }

        if (!validateServiceConfiguationsViaFlattenMap(flattenConfigMap)) {
            throw new IllegalArgumentException("Validation failed for service configurations.");
        }

        // here we are storing actual catalogs
        // before that we need to replace dummy service id to the actual one
        service = environmentService.addService(service);

        for (Component component : components) {
            component.setServiceId(service.getId());
            environmentService.addComponent(component);
        }

        for (ServiceConfiguration configuration : configurations) {
            configuration.setServiceId(service.getId());
            environmentService.addServiceConfiguration(configuration);
        }

        return service;
    }

    private String getConfType(String fileName) {
        // treat confType as the file name without extension
        return FilenameUtils.getBaseName(fileName);
    }

    private Map<String, String> readConfigFile(ConfigFileInfo configFileInfo) throws IOException {
        String fileName = configFileInfo.getFileName();
        ConfigFileType fileType = ConfigFileType.getFileTypeFromFileName(fileName);

        if (fileType == null) {
            throw new IllegalArgumentException("Unsupported configuration file type - file name: " + fileName);
        }

        ConfigFileReader reader = new ConfigFileReader();
        return reader.readConfig(fileType, configFileInfo.getFileInputStream());
    }

    private void assertInputByDefinition(List<ComponentInfo> componentInfos, List<ConfigFileInfo> configFileInfos) {
        ServiceManualRegistrationDefinition definition = getRegistrationDefinition();

        List<String> requiredComponents = definition.getRequiredComponents();
        List<String> requiredConfigFiles = definition.getRequiredConfigFiles();

        for (String requiredComponent : requiredComponents) {
            if(!findComponent(componentInfos, requiredComponent).isPresent()) {
                throw new IllegalArgumentException("Required component " + requiredComponent + " not presented");
            }
        }

        for (String requiredConfigFile : requiredConfigFiles) {
            if (!findConfiguration(configFileInfos, requiredConfigFile).isPresent()) {
                throw new IllegalArgumentException("Required config file " + requiredConfigFile + " not presented");
            }
        }
    }

    private Optional<ComponentInfo> findComponent(List<ComponentInfo> componentInfos, String requiredComponent) {
        return componentInfos.stream().filter(c -> c.getName().equals(requiredComponent)).findFirst();
    }

    private Optional<ConfigFileInfo> findConfiguration(List<ConfigFileInfo> configFileInfos, String requiredConfigFile) {
        return configFileInfos.stream().filter(c -> c.getFileName().equals(requiredConfigFile)).findFirst();
    }
}
