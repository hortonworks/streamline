package com.hortonworks.streamline.streams.cluster.resource;

import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.model.ServiceWithComponents;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import java.util.ArrayList;
import java.util.Collection;

public class ClusterResourceUtil {
    public static class ClusterServicesImportResult {
        private Cluster cluster;
        private Collection<ServiceWithComponents> services = new ArrayList<>();

        public ClusterServicesImportResult(Cluster cluster) {
            this.cluster = cluster;
        }

        public Cluster getCluster() {
            return cluster;
        }

        public Collection<ServiceWithComponents> getServices() {
            return services;
        }

        public void setServices(Collection<ServiceWithComponents> services) {
            this.services = services;
        }

        public void addService(ServiceWithComponents service) {
            services.add(service);
        }
    }

    public static ClusterServicesImportResult enrichCluster(Cluster cluster,
                                                     EnvironmentService environmentService) {
        ClusterServicesImportResult result = new ClusterServicesImportResult(cluster);

        for (Service service : environmentService.listServices(cluster.getId())) {
            Collection<ServiceConfiguration> configurations = environmentService.listServiceConfigurations(service.getId());
            Collection<Component> components = environmentService.listComponents(service.getId());

            ServiceWithComponents s = new ServiceWithComponents(service);
            s.setComponents(components);
            s.setConfigurations(configurations);

            result.addService(s);
        }
        return result;
    }

}
