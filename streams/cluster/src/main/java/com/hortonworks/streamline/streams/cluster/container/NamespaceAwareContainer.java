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
package com.hortonworks.streamline.streams.cluster.container;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.NamespaceServiceClusterMapping;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * This class is a base container implementation which element (instance) should be created per Namespace.
 * This only provides container functionality, and implementations should provide the way to initialize element (instance).
 */
public abstract class NamespaceAwareContainer<T> {
    private Map<Long, T> namespaceToInstance;
    protected final EnvironmentService environmentService;

    public NamespaceAwareContainer(EnvironmentService environmentService) {
        this.environmentService = environmentService;
        namespaceToInstance = new HashMap<>();
    }

    public T findInstance(Namespace namespace) {
        Long namespaceId = namespace.getId();
        if (namespaceToInstance.containsKey(namespaceId)) {
            return namespaceToInstance.get(namespaceId);
        }

        T newInstance = initializeInstance(namespace);
        namespaceToInstance.put(namespaceId, newInstance);
        return newInstance;
    }

    public void invalidateInstance(Long namespaceId) {
        namespaceToInstance.remove(namespaceId);
    }

    protected abstract T initializeInstance(Namespace namespace);

    protected Service getFirstOccurenceServiceForNamespace(Namespace namespace, String serviceName) {
        Collection<Service> services = getServiceForNamespace(namespace, serviceName);
        if (services.isEmpty()) {
            return null;
        }

        return services.iterator().next();
    }

    protected Collection<Service> getServiceForNamespace(Namespace namespace, String serviceName) {
        Collection<NamespaceServiceClusterMapping> serviceClusterMappings =
                environmentService.listServiceClusterMapping(namespace.getId(), serviceName);
        if (serviceClusterMappings == null) {
            throw new RuntimeException("Service name " + serviceName + " is not set in namespace " +
                    namespace.getName() + "(" + namespace.getId() + ")");
        }

        Collection<Service> services = new ArrayList<>(serviceClusterMappings.size());
        for (NamespaceServiceClusterMapping mapping : serviceClusterMappings) {
            Long clusterId = mapping.getClusterId();
            Cluster cluster = environmentService.getCluster(clusterId);
            if (cluster == null) {
                throw new RuntimeException("Cluster " + clusterId + " is not found");
            }

            Service service = environmentService.getServiceByName(clusterId, serviceName);
            if (service == null) {
                throw new RuntimeException("Service name " + serviceName + " is not found in Cluster " + clusterId);
            }

            services.add(service);
        }

        return services;
    }

    protected Optional<Component> getComponent(Service service, String componentName) {
        Collection<Component> allComponents = environmentService.listComponents(service.getId());

        List<Component> components = allComponents.stream().filter(x -> x.getName().equals(componentName)).collect(toList());
        if (components == null || components.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(components.get(0));
    }

    protected Optional<ServiceConfiguration> getServiceConfiguration(Service service, String serviceConfigurationName) {
        Collection<ServiceConfiguration> allServiceConfigurations = environmentService.listServiceConfigurations(service.getId());

        List<ServiceConfiguration> configurations = allServiceConfigurations.stream()
                .filter(x -> x.getName().equals(serviceConfigurationName)).collect(toList());
        if (configurations == null || configurations.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(configurations.get(0));
    }

    protected T instantiate(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<T> clazz = (Class<T>) Class.forName(className);
        return clazz.newInstance();
    }

    protected void assertHostAndPort(String componentName, String host, Integer port) {
        if (host == null || host.isEmpty() || port == null) {
            throw new RuntimeException(componentName + " component doesn't have enough information - host: " + host +
                    " / port: " + port);
        }
    }

    protected void assertHostsAndPort(String componentName, List<String> hosts, Integer port) {
        if (hosts == null || hosts.isEmpty() || port == null) {
            throw new RuntimeException(componentName + " component doesn't have enough information - hosts: " + hosts +
                    " / port: " + port);
        }
    }
}
