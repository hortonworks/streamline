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
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.catalog.configuration.ConfigFileReader;
import com.hortonworks.streamline.streams.catalog.configuration.ConfigFileType;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ConfigFilePattern;
import com.hortonworks.streamline.streams.cluster.register.ManualServiceRegistrar;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractServiceRegistrar implements ManualServiceRegistrar {
    protected EnvironmentService environmentService;

    protected abstract String getServiceName();

    protected abstract Map<Component, List<ComponentProcess>> createComponents(Config config, Map<String, String> flattenConfigMap);

    protected abstract List<ServiceConfiguration> createServiceConfigurations(Config config);

    protected abstract boolean validateComponents(Map<Component, List<ComponentProcess>> components);

    protected abstract boolean validateServiceConfigurations(List<ServiceConfiguration> serviceConfigurations);

    protected abstract boolean validateServiceConfiguationsAsFlattenedMap(Map<String, String> configMap);

    @Override
    public void init(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public Service register(Cluster cluster, Config config, List<ConfigFileInfo> configFileInfos) throws IOException {
        Service service = environmentService.initializeService(cluster, getServiceName());

        List<ServiceConfiguration> configurations = new ArrayList<>();
        Map<String, String> flattenConfigMap = new HashMap<>();

        List<ServiceConfiguration> serviceConfigurations = createServiceConfigurations(config);
        if (serviceConfigurations != null && !serviceConfigurations.isEmpty()) {
            serviceConfigurations.forEach(sc -> {
                configurations.add(sc);
                try {
                    flattenConfigMap.putAll(sc.getConfigurationMap());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        for (ConfigFileInfo configFileInfo : configFileInfos) {
            Map<String, String> configMap = readConfigFile(configFileInfo);

            String fileName = FilenameUtils.getName(configFileInfo.getFileName());
            String confType = getConfType(fileName);
            String actualFileName = ConfigFilePattern.getOriginFileName(confType);

            ServiceConfiguration configuration = environmentService.initializeServiceConfiguration(
                    service.getId(), confType, actualFileName, new HashMap<>(configMap));
            configurations.add(configuration);
            flattenConfigMap.putAll(configMap);
        }

        Map<Component, List<ComponentProcess>> components = createComponents(config, flattenConfigMap);

        if (!validateComponents(components)) {
            throw new IllegalArgumentException("Validation failed for components.");
        }

        if (!validateServiceConfigurations(configurations)) {
            throw new IllegalArgumentException("Validation failed for service configurations.");
        }

        if (!validateServiceConfiguationsAsFlattenedMap(flattenConfigMap)) {
            throw new IllegalArgumentException("Validation failed for service configurations.");
        }

        // here we are storing actual catalogs
        // before that we need to replace dummy service id to the actual one
        service = environmentService.addService(service);

        for (Map.Entry<Component, List<ComponentProcess>> entry : components.entrySet()) {
            Component component = entry.getKey();
            List<ComponentProcess> componentProcesses = entry.getValue();

            component.setServiceId(service.getId());
            component = environmentService.addComponent(component);

            for (ComponentProcess componentProcess : componentProcesses) {
                componentProcess.setComponentId(component.getId());
                environmentService.addComponentProcess(componentProcess);
            }
        }

        for (ServiceConfiguration configuration : configurations) {
            configuration.setServiceId(service.getId());
            environmentService.addServiceConfiguration(configuration);
        }

        return service;
    }

    protected boolean validateComponentProcesses(List<ComponentProcess> componentProcesses) {
        if (componentProcesses.size() <= 0) {
            return false;
        }

        return componentProcesses.stream().allMatch(componentProcess ->
                StringUtils.isNotEmpty(componentProcess.getHost()) &&
                        componentProcess.getPort() != null);
    }

    protected boolean validateComponentProcessesWithProtocolRequired(List<ComponentProcess> componentProcesses) {
        if (componentProcesses.size() <= 0) {
            return false;
        }

        return componentProcesses.stream().allMatch(componentProcess ->
                StringUtils.isNotEmpty(componentProcess.getHost()) &&
                        componentProcess.getPort() != null &&
                        StringUtils.isNotEmpty(componentProcess.getProtocol()));
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

}
