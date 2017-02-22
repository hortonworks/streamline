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
package com.hortonworks.streamline.streams.cluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.util.ParallelStreamUtil;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.discovery.ServiceNodeDiscoverer;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

public class ClusterImporter {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterImporter.class);
    private static final int FORK_JOIN_POOL_PARALLELISM = 20;

    private final EnvironmentService environmentService;
    private final ForkJoinPool forkJoinPool;

    public ClusterImporter(EnvironmentService environmentService) {
        this.environmentService = environmentService;
        this.forkJoinPool = new ForkJoinPool(FORK_JOIN_POOL_PARALLELISM);
    }

    public Cluster importCluster(ServiceNodeDiscoverer serviceNodeDiscoverer, Cluster cluster) {
        ObjectMapper objectMapper = new ObjectMapper();

        // remove all of relevant services and associated components
        removeAllServices(cluster);

        List<String> availableServices = serviceNodeDiscoverer.getServices();

        ParallelStreamUtil.execute(
                () -> handleServices(serviceNodeDiscoverer, cluster, objectMapper, availableServices),
                forkJoinPool);

        return cluster;
    }

    private Void handleServices(ServiceNodeDiscoverer serviceNodeDiscoverer, Cluster cluster, ObjectMapper objectMapper, List<String> availableServices) {
        availableServices.parallelStream()
                .filter(ServiceConfigurations::contains)
                .forEach(serviceName -> {
                    LOG.debug("service start {}", serviceName);

                    Service service = addService(cluster, serviceName);

                    Map<String, String> flattenConfigurations = new ConcurrentHashMap<>();
                    Map<String, Map<String, String>> configurations = serviceNodeDiscoverer.getConfigurations(serviceName);
                    handleConfigurations(serviceNodeDiscoverer, objectMapper, flattenConfigurations, configurations, service);

                    List<String> components = serviceNodeDiscoverer.getComponents(serviceName);
                    handleComponents(serviceNodeDiscoverer, serviceName, flattenConfigurations, service, components);
                });

        return null;
    }

    private void handleComponents(ServiceNodeDiscoverer serviceNodeDiscoverer, String serviceName, Map<String, String> flattenConfigurations, Service service, List<String> components) {
        components.parallelStream().forEach(componentName -> {
            LOG.debug("component start {}", componentName);

            List<String> hosts = serviceNodeDiscoverer.getComponentNodes(serviceName, componentName);
            addComponent(flattenConfigurations, service, componentName, hosts);

            LOG.debug("component end {}", componentName);
        });
    }

    private void handleConfigurations(ServiceNodeDiscoverer serviceNodeDiscoverer, ObjectMapper objectMapper, Map<String, String> flattenConfigurations, Map<String, Map<String, String>> configurations, Service service) {
        configurations.entrySet().parallelStream()
                .forEach(entry -> {
                    try {
                        String confType = entry.getKey();
                        Map<String, String> configuration = entry.getValue();

                        LOG.debug("conf-type start {}", confType);

                        String actualFileName = serviceNodeDiscoverer.getOriginalFileName(confType);

                        addServiceConfiguration(objectMapper, service, confType, configuration, actualFileName);
                        flattenConfigurations.putAll(configuration);

                        LOG.debug("conf-type end {}", confType);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void addComponent(Map<String, String> flatConfigurations, Service service, String componentName, List<String> hosts) {
        Component component = environmentService.initializeComponent(service, componentName, hosts);
        environmentService.injectProtocolAndPortToComponent(flatConfigurations, component);
        environmentService.addComponent(component);
    }

    private void addServiceConfiguration(ObjectMapper objectMapper, Service service, String confType, Map<String, String> configuration, String actualFileName) throws JsonProcessingException {
        ServiceConfiguration serviceConfiguration = environmentService.initializeServiceConfiguration(objectMapper,
                service.getId(), confType, actualFileName, configuration);

        environmentService.addServiceConfiguration(serviceConfiguration);
    }

    private Service addService(Cluster cluster, String serviceName) {
        Service service = environmentService.initializeService(cluster, serviceName);
        environmentService.addService(service);
        LOG.debug("service added {}", serviceName);
        return service;
    }

    private void removeAllServices(Cluster cluster) {
        Collection<Service> services = environmentService.listServices(cluster.getId());
        for (Service service : services) {
            Collection<Component> components = environmentService.listComponents(service.getId());
            for (Component component : components) {
                environmentService.removeComponent(component.getId());
            }

            Collection<ServiceConfiguration> configurations = environmentService.listServiceConfigurations(service.getId());
            for (ServiceConfiguration configuration : configurations) {
                environmentService.removeServiceConfiguration(configuration.getId());
            }

            environmentService.removeService(service.getId());
        }
    }
}
