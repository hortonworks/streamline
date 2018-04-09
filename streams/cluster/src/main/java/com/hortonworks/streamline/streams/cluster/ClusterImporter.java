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

import com.hortonworks.streamline.common.util.ParallelStreamUtil;
import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.discovery.ServiceNodeDiscoverer;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.SerivceConfigurationFilter;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurationFilters;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaBrokerListeners;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import static java.util.stream.Collectors.toList;

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
        // remove all of relevant services and associated components
        removeAllServices(cluster);

        // fetching information of services from discoverer concurrently
        List<ServiceInformation> serviceInformations = fetchServices(serviceNodeDiscoverer, cluster);

        // storing services and corresponding components, component processes, configurations
        // this is done in the thread which handles cleanup, so that it can be grouped to same transaction
        serviceInformations.forEach(this::storeService);

        return cluster;
    }

    private List<ServiceInformation> fetchServices(ServiceNodeDiscoverer serviceNodeDiscoverer, Cluster cluster) {
        List<String> availableServices = serviceNodeDiscoverer.getServices();
        return availableServices.parallelStream()
                .filter(ServiceConfigurations::contains)
                .map(serviceName -> ParallelStreamUtil.execute(
                        () -> fetchService(serviceNodeDiscoverer, cluster, serviceName),
                        forkJoinPool))
                .collect(toList());
    }

    private ServiceInformation fetchService(ServiceNodeDiscoverer serviceNodeDiscoverer, Cluster cluster, String serviceName) {
        LOG.debug("service start {}", serviceName);

        ServiceInformation serviceInformation = createServiceInformation(cluster, serviceName);

        ServiceConfigurationInformation serviceConfigurationInformation = fetchServiceConfigurations(cluster,
                serviceNodeDiscoverer, serviceInformation);

        List<ComponentInformation> components = fetchComponents(serviceNodeDiscoverer, serviceName,
                serviceConfigurationInformation.getFlattenConfiguration(), serviceInformation);

        serviceInformation.setComponents(components);
        serviceInformation.setServiceConfigurationInfo(serviceConfigurationInformation);

        return serviceInformation;
    }

    private List<ComponentInformation> fetchComponents(ServiceNodeDiscoverer serviceNodeDiscoverer, String serviceName,
                                                       Map<String, String> flatConfiguration, ServiceInformation serviceInformation) {
        List<String> ambariComponents = serviceNodeDiscoverer.getComponents(serviceName);

        return ambariComponents.stream().map(componentName -> {
            LOG.debug("component start {}", componentName);

            Component component = environmentService.createComponent(serviceInformation.getService(), componentName);

            List<ComponentProcess> componentProcesses = fetchComponentProcesses(serviceNodeDiscoverer, serviceName, componentName);

            environmentService.injectProtocolAndPortToComponent(flatConfiguration, component, componentProcesses);
            ComponentInformation componentInformation;
            // workaround for Kafka protocol
            if (componentName.equals(ComponentPropertyPattern.KAFKA_BROKER.name())) {
                List<KafkaBrokerListeners.ListenersPropEntry> parsedProps =
                        new KafkaBrokerListeners.ListenersPropParsed(flatConfiguration).getParsedProps();
                List<ComponentProcess> effectiveComponentProcesses =
                        new ArrayList<>(componentProcesses.size() * parsedProps.size());

                for (ComponentProcess componentProcess : componentProcesses) {
                    for (KafkaBrokerListeners.ListenersPropEntry listenersPropEntry : parsedProps) {
                        effectiveComponentProcesses.add(newComponentProcess(componentProcess, listenersPropEntry));
                    }
                }
                LOG.debug("components added for kafka [{}]", effectiveComponentProcesses);
                componentInformation = new ComponentInformation(component, effectiveComponentProcesses);
            } else {
                LOG.debug("components added for non-kafka [{}]", componentProcesses);
                componentInformation = new ComponentInformation(component, componentProcesses);
            }

            LOG.debug("component end {}", componentName);
            return componentInformation;
        }).collect(toList());
    }

    private ComponentProcess newComponentProcess(ComponentProcess componentProcess,
                                                 KafkaBrokerListeners.ListenersPropEntry listenersPropEntry) {
        ComponentProcess newComponentProcess = new ComponentProcess();
        newComponentProcess.setComponentId(componentProcess.getComponentId());
        newComponentProcess.setHost(componentProcess.getHost());
        newComponentProcess.setTimestamp(componentProcess.getTimestamp());
        newComponentProcess.setProtocol(listenersPropEntry.getProtocol().name());
        newComponentProcess.setPort(listenersPropEntry.getPort());

        return newComponentProcess;
    }

    private List<ComponentProcess> fetchComponentProcesses(ServiceNodeDiscoverer serviceNodeDiscoverer, String serviceName, String componentName) {
        List<String> hosts = serviceNodeDiscoverer.getComponentNodes(serviceName, componentName);
        return hosts.stream().map(host -> {
            ComponentProcess cp = new ComponentProcess();
            cp.setHost(host);
            return cp;
        }).collect(toList());
    }

    private ServiceConfigurationInformation fetchServiceConfigurations(Cluster cluster,
                                                                       ServiceNodeDiscoverer serviceNodeDiscoverer,
                                                                       ServiceInformation serviceInformation) {
        Map<String, Map<String, String>> ambariServiceConfigurations = serviceNodeDiscoverer.getConfigurations(
                serviceInformation.getService().getName());
        List<ServiceConfiguration> serviceConfigurations = ambariServiceConfigurations.entrySet().stream()
                .map(entry -> {
                    try {
                        String confType = entry.getKey();
                        SerivceConfigurationFilter filter = ServiceConfigurationFilters.get(cluster, confType);
                        Map<String, String> configuration = filter.filter(entry.getValue());

                        LOG.debug("conf-type start {}", confType);

                        String actualFileName = serviceNodeDiscoverer.getOriginalFileName(confType);

                        ServiceConfiguration serviceConfiguration = environmentService.createServiceConfiguration(
                                null, confType, actualFileName, configuration);

                        LOG.debug("conf-type end {}", confType);

                        return serviceConfiguration;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(toList());

        return new ServiceConfigurationInformation(serviceConfigurations);
    }

    private ServiceInformation createServiceInformation(Cluster cluster, String serviceName) {
        Service service = environmentService.createService(cluster, serviceName);
        return new ServiceInformation(service);
    }


    private void storeService(ServiceInformation serviceInformation) {
        Service service = serviceInformation.getService();

        Long serviceId = environmentService.addService(service).getId();

        serviceInformation.getComponents().forEach(componentInformation -> storeComponent(serviceId, componentInformation));

        ServiceConfigurationInformation scInfo = serviceInformation.getServiceConfigurationInfo();
        scInfo.getServiceConfigurations().forEach(sc -> storeServiceConfiguration(serviceId, sc));
    }

    private void storeComponent(Long serviceId, ComponentInformation componentInformation) {
        Component component = componentInformation.getComponent();
        component.setServiceId(serviceId);

        Long componentId = environmentService.addComponent(component).getId();

        componentInformation.getComponentProcesses().forEach(componentProcess -> {
            componentProcess.setComponentId(componentId);
            environmentService.addComponentProcess(componentProcess);
        });
    }

    private void storeServiceConfiguration(Long serviceId, ServiceConfiguration sc) {
        sc.setServiceId(serviceId);
        environmentService.addServiceConfiguration(sc);
    }

    private void removeAllServices(Cluster cluster) {
        Collection<Service> services = environmentService.listServices(cluster.getId());
        for (Service service : services) {
            Collection<Component> components = environmentService.listComponents(service.getId());
            for (Component component : components) {
                environmentService.listComponentProcesses(component.getId())
                        .forEach(componentProcess -> environmentService.removeComponentProcess(componentProcess.getId()));

                environmentService.removeComponent(component.getId());
            }

            Collection<ServiceConfiguration> configurations = environmentService.listServiceConfigurations(service.getId());
            for (ServiceConfiguration configuration : configurations) {
                environmentService.removeServiceConfiguration(configuration.getId());
            }

            environmentService.removeService(service.getId());
        }
    }

    private static class ServiceInformation {
        private Service service;
        private List<ComponentInformation> components;
        private ServiceConfigurationInformation serviceConfigurationInfo;

        ServiceInformation(Service service) {
            this.service = service;
            this.components = new ArrayList<>();
            this.serviceConfigurationInfo = null;
        }

        void setService(Service service) {
            this.service = service;
        }

        void setComponents(List<ComponentInformation> components) {
            this.components = components;
        }

        void setServiceConfigurationInfo(ServiceConfigurationInformation serviceConfigurationInfo) {
            this.serviceConfigurationInfo = serviceConfigurationInfo;
        }

        Service getService() {
            return service;
        }

        List<ComponentInformation> getComponents() {
            return components;
        }

        ServiceConfigurationInformation getServiceConfigurationInfo() {
            return serviceConfigurationInfo;
        }
    }

    private static class ServiceConfigurationInformation {
        private List<ServiceConfiguration> serviceConfigurations;
        private Map<String, String> flattenConfiguration;

        ServiceConfigurationInformation(List<ServiceConfiguration> serviceConfigurations) {
            this.serviceConfigurations = serviceConfigurations;

            this.flattenConfiguration = new HashMap<>();
            serviceConfigurations.forEach(Unchecked.consumer(sc -> flattenConfiguration.putAll(sc.getConfigurationMap())));
        }

        List<ServiceConfiguration> getServiceConfigurations() {
            return serviceConfigurations;
        }

        Map<String, String> getFlattenConfiguration() {
            return flattenConfiguration;
        }
    }

    private static class ComponentInformation {
        private Component component;
        private List<ComponentProcess> componentProcesses;

        ComponentInformation(Component component, List<ComponentProcess> componentProcesses) {
            this.component = component;
            this.componentProcesses = componentProcesses;
        }

        Component getComponent() {
            return component;
        }

        List<ComponentProcess> getComponentProcesses() {
            return componentProcesses;
        }
    }

}
