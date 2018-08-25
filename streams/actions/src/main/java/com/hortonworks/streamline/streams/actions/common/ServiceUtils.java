package com.hortonworks.streamline.streams.actions.common;

import com.hortonworks.streamline.streams.cluster.catalog.*;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class ServiceUtils {

    public static Service getFirstOccurenceServiceForNamespace(Namespace namespace, String serviceName,
                                                           EnvironmentService environmentService) {
        Collection<Service> services = getServiceForNamespace(namespace, serviceName, environmentService);
        if (services.isEmpty()) {
            return null;
        }

        return services.iterator().next();
    }

    public static Collection<Service> getServiceForNamespace(Namespace namespace, String serviceName,
                                                         EnvironmentService environmentService) {
        Collection<NamespaceServiceClusterMap> serviceClusterMappings =
                environmentService.listServiceClusterMapping(namespace.getId(), serviceName);
        if (serviceClusterMappings == null) {
            throw new RuntimeException("Service name " + serviceName + " is not set in namespace " +
                    namespace.getName() + "(" + namespace.getId() + ")");
        }

        Collection<Service> services = new ArrayList<>(serviceClusterMappings.size());
        for (NamespaceServiceClusterMap mapping : serviceClusterMappings) {
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

    public static Optional<Component> getComponent(Service service, String componentName,
                                               EnvironmentService environmentService) {
        Collection<Component> allComponents = environmentService.listComponents(service.getId());

        List<Component> components = allComponents.stream().filter(x -> x.getName().equals(componentName)).collect(toList());
        if (components == null || components.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(components.get(0));
    }

    public static Optional<ServiceConfiguration> getServiceConfiguration(Service service, String serviceConfigurationName,
                                                                     EnvironmentService environmentService) {
        Collection<ServiceConfiguration> allServiceConfigurations = environmentService.listServiceConfigurations(service.getId());

        List<ServiceConfiguration> configurations = allServiceConfigurations.stream()
                .filter(x -> x.getName().equals(serviceConfigurationName)).collect(toList());
        if (configurations == null || configurations.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(configurations.get(0));
    }

    public static void assertHostAndPort(String componentName, String host, Integer port) {
        if (host == null || host.isEmpty() || port == null) {
            throw new RuntimeException(componentName + " component doesn't have enough information - host: " + host +
                    " / port: " + port);
        }
    }

    public static void assertHostsAndPort(String componentName, List<String> hosts, Integer port) {
        if (hosts == null || hosts.isEmpty() || port == null) {
            throw new RuntimeException(componentName + " component doesn't have enough information - hosts: " + hosts +
                    " / port: " + port);
        }
    }
}
