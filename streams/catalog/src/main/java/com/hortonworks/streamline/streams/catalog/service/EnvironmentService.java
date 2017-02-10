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
package com.hortonworks.streamline.streams.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.StorageManager;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.NamespaceServiceClusterMapping;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.catalog.cluster.ClusterImporter;
import com.hortonworks.streamline.streams.catalog.container.ContainingNamespaceAwareContainer;
import com.hortonworks.streamline.streams.cluster.discovery.ServiceNodeDiscoverer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EnvironmentService {
    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentService.class);

    private static final String CLUSTER_NAMESPACE = new Cluster().getNameSpace();
    private static final String SERVICE_NAMESPACE = new Service().getNameSpace();
    private static final String COMPONENT_NAMESPACE = new Component().getNameSpace();
    private static final String SERVICE_CONFIGURATION_NAMESPACE = new ServiceConfiguration().getNameSpace();
    private static final String NAMESPACE_NAMESPACE = new Namespace().getNameSpace();
    private static final String NAMESPACE_SERVICE_CLUSTER_MAPPING_NAMESPACE = new NamespaceServiceClusterMapping().getNameSpace();

    private final StorageManager dao;
    private final ClusterImporter clusterImporter;
    private final List<ContainingNamespaceAwareContainer> containers;

    public EnvironmentService(StorageManager dao) {
        this.dao = dao;
        this.clusterImporter = new ClusterImporter(this);
        this.containers = new ArrayList<>();
    }

    public void addNamespaceAwareContainer(ContainingNamespaceAwareContainer container) {
        this.containers.add(container);
    }

    public Cluster importClusterServices(ServiceNodeDiscoverer serviceNodeDiscoverer, Cluster cluster) throws Exception {
        return clusterImporter.importCluster(serviceNodeDiscoverer, cluster);
    }

    public Service initializeService(Cluster cluster, String serviceName) {
        Service service = new Service();
        service.setId(this.dao.nextId(SERVICE_NAMESPACE));
        service.setName(serviceName);
        service.setClusterId(cluster.getId());
        service.setTimestamp(System.currentTimeMillis());
        return service;
    }

    public Component initializeComponent(Service service, String componentName, List<String> hosts) {
        Component component = new Component();
        component.setId(this.dao.nextId(COMPONENT_NAMESPACE));
        component.setName(componentName);
        component.setServiceId(service.getId());
        component.setTimestamp(System.currentTimeMillis());
        component.setHosts(hosts);
        return component;
    }

    public ServiceConfiguration initializeServiceConfiguration(ObjectMapper objectMapper, Long serviceId,
                                                               String confType, String actualFileName, Map<String, Object> configuration) throws JsonProcessingException {
        ServiceConfiguration conf = new ServiceConfiguration();
        conf.setId(this.dao.nextId(SERVICE_CONFIGURATION_NAMESPACE));
        conf.setName(confType);
        conf.setServiceId(serviceId);
        conf.setFilename(actualFileName);
        conf.setConfiguration(objectMapper.writeValueAsString(configuration));
        conf.setTimestamp(System.currentTimeMillis());
        return conf;
    }


    public Cluster addCluster(Cluster cluster) {
        if (cluster.getId() == null) {
            cluster.setId(this.dao.nextId(CLUSTER_NAMESPACE));
        }
        if (cluster.getTimestamp() == null) {
            cluster.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(cluster);
        return cluster;
    }

    public Collection<Cluster> listClusters() {
        return this.dao.list(CLUSTER_NAMESPACE);
    }


    public Collection<Cluster> listClusters(List<QueryParam> params) {
        return dao.find(CLUSTER_NAMESPACE, params);
    }

    public Cluster getCluster(Long clusterId) {
        Cluster cluster = new Cluster();
        cluster.setId(clusterId);
        return this.dao.get(new StorableKey(CLUSTER_NAMESPACE, cluster.getPrimaryKey()));
    }

    public Cluster getClusterByNameAndImportUrl(String clusterName, String ambariImportUrl) {
        Collection<Cluster> clusters = listClusters(
                Lists.newArrayList(new QueryParam("name", clusterName), new QueryParam("ambariImportUrl", ambariImportUrl)));
        if (clusters.size() > 1) {
            LOG.warn("Multiple Clusters have same name {} and import url {} : returning first match.", clusterName, ambariImportUrl);
            return clusters.iterator().next();
        } else if (clusters.size() == 1) {
            return clusters.iterator().next();
        }
        return null;
    }

    public Cluster removeCluster(Long clusterId) {
        Cluster cluster = new Cluster();
        cluster.setId(clusterId);
        return dao.remove(new StorableKey(CLUSTER_NAMESPACE, cluster.getPrimaryKey()));
    }

    public Cluster addOrUpdateCluster(Long clusterId, Cluster cluster) {
        if (cluster.getId() == null) {
            cluster.setId(clusterId);
        }
        if (cluster.getTimestamp() == null) {
            cluster.setTimestamp(System.currentTimeMillis());
        }
        this.dao.addOrUpdate(cluster);
        return cluster;
    }

    public Service addService(Service service) {
        if (service.getId() == null) {
            service.setId(this.dao.nextId(SERVICE_NAMESPACE));
        }
        if (service.getTimestamp() == null) {
            service.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(service);
        return service;
    }

    public Collection<Service> listServices() {
        return this.dao.list(SERVICE_NAMESPACE);
    }

    public Collection<Service> listServices(final Long clusterId) {
        return this.dao.find(SERVICE_NAMESPACE, Collections.singletonList(new QueryParam("clusterId", clusterId.toString())));
    }

    public Collection<Service> listServices(List<QueryParam> params) {
        return dao.find(SERVICE_NAMESPACE, params);
    }

    public Service getService(Long serviceId) {
        Service service = new Service();
        service.setId(serviceId);
        return this.dao.get(new StorableKey(SERVICE_NAMESPACE, service.getPrimaryKey()));
    }

    public Long getServiceIdByName(Long clusterId, String serviceName) {
        final Service service = getServiceByName(clusterId, serviceName);
        return service == null ? null : service.getId();
    }

    public Service getServiceByName(Long clusterId, String serviceName) {
        Collection<Service> services = listServices(
                Lists.newArrayList(new QueryParam("clusterId", String.valueOf(clusterId)), new QueryParam("name", serviceName)));
        if (services.size() > 1) {
            LOG.warn("Multiple Services have same name {} in cluster {}. returning first match.", serviceName, clusterId);
            return services.iterator().next();
        } else if (services.size() == 1) {
            return services.iterator().next();
        }
        return null;
    }

    public Service removeService(Long serviceId) {
        Service service = new Service();
        service.setId(serviceId);
        return dao.remove(new StorableKey(SERVICE_NAMESPACE, service.getPrimaryKey()));
    }

    public Service addOrUpdateService(Long clusterId, Service service) {
        return addOrUpdateService(clusterId, service.getId(), service);
    }

    public Service addOrUpdateService(Long clusterId, Long componentId, Service service) {
        service.setClusterId(clusterId);
        service.setId(componentId);
        if (service.getTimestamp() == null) {
            service.setTimestamp(System.currentTimeMillis());
        }
        this.dao.addOrUpdate(service);
        return service;
    }

    public Component addComponent(Component component) {
        if (component.getId() == null) {
            component.setId(this.dao.nextId(COMPONENT_NAMESPACE));
        }
        if (component.getTimestamp() == null) {
            component.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(component);
        return component;
    }

    public Collection<Component> listComponents() {
        return this.dao.list(COMPONENT_NAMESPACE);
    }

    public Collection<Component> listComponents(final Long serviceId) {
        return dao.find(COMPONENT_NAMESPACE, Collections.singletonList(new QueryParam("serviceId", serviceId.toString())));
    }

    public Collection<Component> listComponents(List<QueryParam> queryParams) {
        return dao.find(COMPONENT_NAMESPACE, queryParams);
    }

    public Component getComponent(Long componentId) {
        Component component = new Component();
        component.setId(componentId);
        return this.dao.get(new StorableKey(COMPONENT_NAMESPACE, component.getPrimaryKey()));
    }

    public Component getComponentByName(Long serviceId, String componentName) {
        Collection<Component> components = listComponents(Lists.newArrayList(
                new QueryParam("serviceId", String.valueOf(serviceId)), new QueryParam("name", componentName)));
        if (components.size() > 1) {
            LOG.warn("Multiple Components have same name {} in service {}. returning first match.",
                    componentName, serviceId);
            return components.iterator().next();
        } else if (components.size() == 1) {
            return components.iterator().next();
        }
        return null;
    }

    public Component removeComponent(Long componentId) {
        Component component = new Component();
        component.setId(componentId);
        return dao.remove(new StorableKey(COMPONENT_NAMESPACE, component.getPrimaryKey()));
    }

    public Component addOrUpdateComponent(Long serviceId, Component component) {
        return addOrUpdateComponent(serviceId, component.getId(), component);
    }

    public Component addOrUpdateComponent(Long serviceId, Long componentId, Component component) {
        component.setServiceId(serviceId);
        component.setId(componentId);
        if (component.getTimestamp() == null) {
            component.setTimestamp(System.currentTimeMillis());
        }
        this.dao.addOrUpdate(component);
        return component;
    }

    public Collection<ServiceConfiguration> listServiceConfigurations() {
        return this.dao.list(SERVICE_CONFIGURATION_NAMESPACE);
    }

    public Collection<ServiceConfiguration> listServiceConfigurations(final Long serviceId) {
        return dao.find(SERVICE_CONFIGURATION_NAMESPACE, Collections.singletonList(new QueryParam("serviceId", serviceId.toString())));
    }

    public Collection<ServiceConfiguration> listServiceConfigurations(
            List<QueryParam> queryParams) {
        return dao.find(SERVICE_CONFIGURATION_NAMESPACE, queryParams);
    }

    public ServiceConfiguration getServiceConfiguration(Long configurationId) {
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        serviceConfiguration.setId(configurationId);
        return this.dao.get(new StorableKey(SERVICE_CONFIGURATION_NAMESPACE, serviceConfiguration.getPrimaryKey()));
    }

    public ServiceConfiguration getServiceConfigurationByName(Long serviceId, String configurationName) {
        Collection<ServiceConfiguration> configurations = listServiceConfigurations(Lists.newArrayList(
                new QueryParam("serviceId", String.valueOf(serviceId)), new QueryParam("name", configurationName)));
        if (configurations.size() > 1) {
            LOG.warn("Multiple ServiceConfigurations have same name {} in service {}. returning first match.",
                    configurationName, serviceId);
            return configurations.iterator().next();
        } else if (configurations.size() == 1) {
            return configurations.iterator().next();
        }
        return null;
    }

    public ServiceConfiguration removeServiceConfiguration(Long configurationId) {
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        serviceConfiguration.setId(configurationId);
        return this.dao.remove(new StorableKey(SERVICE_CONFIGURATION_NAMESPACE, serviceConfiguration.getPrimaryKey()));
    }

    public ServiceConfiguration addServiceConfiguration(ServiceConfiguration serviceConfiguration) {
        if (serviceConfiguration.getId() == null) {
            serviceConfiguration.setId(this.dao.nextId(SERVICE_CONFIGURATION_NAMESPACE));
        }
        if (serviceConfiguration.getTimestamp() == null) {
            serviceConfiguration.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(serviceConfiguration);
        return serviceConfiguration;
    }

    public ServiceConfiguration addOrUpdateServiceConfiguration(Long serviceId,
                                                                ServiceConfiguration serviceConfiguration) {
        return addOrUpdateServiceConfiguration(serviceId, serviceConfiguration.getId(), serviceConfiguration);
    }

    public ServiceConfiguration addOrUpdateServiceConfiguration(Long serviceId, Long serviceConfigurationId,
                                                                ServiceConfiguration serviceConfiguration) {
        serviceConfiguration.setServiceId(serviceId);
        serviceConfiguration.setId(serviceConfigurationId);
        if (serviceConfiguration.getTimestamp() == null) {
            serviceConfiguration.setTimestamp(System.currentTimeMillis());
        }
        this.dao.addOrUpdate(serviceConfiguration);
        return serviceConfiguration;
    }

    public Collection<Namespace> listNamespaces() {
        return this.dao.list(NAMESPACE_NAMESPACE);
    }

    public Collection<Namespace> listNamespaces(List<QueryParam> params) {
        return dao.find(NAMESPACE_NAMESPACE, params);
    }

    public Namespace getNamespace(Long namespaceId) {
        Namespace namespace = new Namespace();
        namespace.setId(namespaceId);
        return this.dao.get(new StorableKey(NAMESPACE_NAMESPACE, namespace.getPrimaryKey()));
    }

    public Namespace getNamespaceByName(String namespaceName) {
        Collection<Namespace> namespaces = listNamespaces(Lists.newArrayList(new QueryParam("name", namespaceName)));
        if (namespaces.size() > 1) {
            LOG.warn("Multiple Namespaces have same name: {} returning first match.", namespaceName);
            return namespaces.iterator().next();
        } else if (namespaces.size() == 1) {
            return namespaces.iterator().next();
        }
        return null;
    }

    public Namespace addNamespace(Namespace namespace) {
        if (namespace.getId() == null) {
            namespace.setId(this.dao.nextId(NAMESPACE_NAMESPACE));
        }
        if (namespace.getTimestamp() == null) {
            namespace.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(namespace);
        return namespace;
    }

    public Namespace removeNamespace(Long namespaceId) {
        Namespace namespace = new Namespace();
        namespace.setId(namespaceId);
        Namespace ret = this.dao.remove(new StorableKey(NAMESPACE_NAMESPACE, namespace.getPrimaryKey()));
        invalidateTopologyActionsMetricsInstances(namespaceId);
        return ret;
    }

    public Namespace addOrUpdateNamespace(Long namespaceId, Namespace namespace) {
        if (namespace.getId() == null) {
            namespace.setId(namespaceId);
        }
        if (namespace.getTimestamp() == null) {
            namespace.setTimestamp(System.currentTimeMillis());
        }
        this.dao.addOrUpdate(namespace);
        return namespace;
    }

    public Collection<NamespaceServiceClusterMapping> listServiceClusterMapping(Long namespaceId) {
        return this.dao.find(NAMESPACE_SERVICE_CLUSTER_MAPPING_NAMESPACE,
                Lists.newArrayList(new QueryParam("namespaceId", namespaceId.toString())));
    }

    public Collection<NamespaceServiceClusterMapping> listServiceClusterMapping(Long namespaceId, String serviceName) {
        return this.dao.find(NAMESPACE_SERVICE_CLUSTER_MAPPING_NAMESPACE,
                Lists.newArrayList(new QueryParam("namespaceId", namespaceId.toString()),
                        new QueryParam("serviceName", serviceName)));
    }

    public NamespaceServiceClusterMapping getServiceClusterMapping(Long namespaceId,
                                                                   String serviceName, Long clusterId) {
        StorableKey key = getStorableKeyForNamespaceServiceClusterMapping(namespaceId, serviceName, clusterId);
        return this.dao.get(key);
    }

    public NamespaceServiceClusterMapping removeServiceClusterMapping(Long namespaceId, String serviceName, Long clusterId) {
        StorableKey key = getStorableKeyForNamespaceServiceClusterMapping(namespaceId, serviceName, clusterId);
        NamespaceServiceClusterMapping ret = this.dao.remove(key);
        invalidateTopologyActionsMetricsInstances(namespaceId);
        return ret;
    }

    public NamespaceServiceClusterMapping addOrUpdateServiceClusterMapping(
            NamespaceServiceClusterMapping newMapping) {
        this.dao.addOrUpdate(newMapping);
        invalidateTopologyActionsMetricsInstances(newMapping.getNamespaceId());
        return newMapping;
    }

    private StorableKey getStorableKeyForNamespaceServiceClusterMapping(Long namespaceId, String serviceName,
                                                                        Long clusterId) {
        NamespaceServiceClusterMapping mapping = new NamespaceServiceClusterMapping();
        mapping.setNamespaceId(namespaceId);
        mapping.setServiceName(serviceName);
        mapping.setClusterId(clusterId);
        return new StorableKey(NAMESPACE_SERVICE_CLUSTER_MAPPING_NAMESPACE,
                mapping.getPrimaryKey());
    }

    private void invalidateTopologyActionsMetricsInstances(Long namespaceId) {
        this.containers.forEach(c -> c.invalidateInstance(namespaceId));
    }

}
