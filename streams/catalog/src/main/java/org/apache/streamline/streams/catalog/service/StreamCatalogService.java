/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.streamline.streams.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Joiner;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.streamline.common.ComponentTypes;
import org.apache.streamline.streams.layout.component.StreamlineComponent;
import org.apache.streamline.streams.layout.component.TopologyDagVisitor;
import org.apache.streamline.streams.layout.component.impl.RulesProcessor;
import org.apache.streamline.streams.catalog.Projection;
import org.apache.streamline.streams.layout.component.StreamlineComponent;
import org.apache.streamline.streams.layout.component.TopologyDagVisitor;
import org.apache.streamline.streams.layout.storm.FluxComponent;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.Schema;
import org.apache.streamline.common.util.FileStorage;
import org.apache.streamline.common.util.JsonSchemaValidator;
import org.apache.streamline.common.util.ProxyUtil;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.storage.Storable;
import org.apache.streamline.storage.StorableKey;
import org.apache.streamline.storage.StorageManager;
import org.apache.streamline.storage.exception.StorageException;
import org.apache.streamline.storage.util.StorageUtils;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.catalog.BranchRuleInfo;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.Component;
import org.apache.streamline.streams.catalog.Namespace;
import org.apache.streamline.streams.catalog.NamespaceServiceClusterMapping;
import org.apache.streamline.streams.catalog.FileInfo;
import org.apache.streamline.streams.catalog.NotifierInfo;
import org.apache.streamline.streams.catalog.RuleInfo;
import org.apache.streamline.streams.catalog.Service;
import org.apache.streamline.streams.catalog.ServiceConfiguration;
import org.apache.streamline.streams.catalog.StreamInfo;
import org.apache.streamline.streams.catalog.Topology;
import org.apache.streamline.streams.catalog.TopologyComponent;
import org.apache.streamline.streams.catalog.TopologyEdge;
import org.apache.streamline.streams.catalog.TopologyEditorMetadata;
import org.apache.streamline.streams.catalog.TopologyOutputComponent;
import org.apache.streamline.streams.catalog.TopologyProcessor;
import org.apache.streamline.streams.catalog.TopologyProcessorStreamMapping;
import org.apache.streamline.streams.catalog.TopologySink;
import org.apache.streamline.streams.catalog.TopologySource;
import org.apache.streamline.streams.catalog.TopologySourceStreamMapping;
import org.apache.streamline.streams.catalog.TopologyVersionInfo;
import org.apache.streamline.streams.catalog.UDFInfo;
import org.apache.streamline.streams.catalog.WindowInfo;
import org.apache.streamline.streams.catalog.configuration.ConfigFileType;
import org.apache.streamline.streams.catalog.configuration.ConfigFileWriter;
import org.apache.streamline.streams.catalog.container.TopologyActionsContainer;
import org.apache.streamline.streams.catalog.container.TopologyMetricsContainer;
import org.apache.streamline.streams.catalog.processor.CustomProcessorInfo;
import org.apache.streamline.streams.catalog.rule.RuleParser;
import org.apache.streamline.streams.catalog.topology.TopologyComponentBundle;
import org.apache.streamline.streams.catalog.topology.TopologyComponentUISpecification;
import org.apache.streamline.streams.catalog.topology.TopologyLayoutValidator;
import org.apache.streamline.streams.catalog.topology.component.TopologyDagBuilder;
import org.apache.streamline.streams.cluster.discovery.ServiceNodeDiscoverer;
import org.apache.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import org.apache.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import org.apache.streamline.streams.layout.TopologyLayoutConstants;
import org.apache.streamline.streams.layout.component.OutputComponent;
import org.apache.streamline.streams.layout.component.Stream;
import org.apache.streamline.streams.layout.component.StreamlineProcessor;
import org.apache.streamline.streams.layout.component.StreamlineSource;
import org.apache.streamline.streams.layout.component.TopologyActions;
import org.apache.streamline.streams.layout.component.TopologyDag;
import org.apache.streamline.streams.catalog.topology.TopologyData;
import org.apache.streamline.streams.catalog.topology.component.TopologyExportVisitor;
import org.apache.streamline.streams.layout.component.TopologyLayout;
import org.apache.streamline.streams.layout.component.rule.Rule;
import org.apache.streamline.streams.layout.exception.ComponentConfigException;
import org.apache.streamline.streams.metrics.topology.TopologyMetrics;
import org.apache.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import org.apache.streamline.streams.rule.UDAF;
import org.apache.streamline.streams.rule.UDAF2;
import org.apache.streamline.streams.rule.UDF;
import org.apache.streamline.streams.rule.UDF2;
import org.apache.streamline.streams.rule.UDF3;
import org.apache.streamline.streams.rule.UDF4;
import org.apache.streamline.streams.rule.UDF5;
import org.apache.streamline.streams.rule.UDF6;
import org.apache.streamline.streams.rule.UDF7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.streamline.streams.catalog.TopologyEditorMetadata.TopologyUIData;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;

import static java.util.stream.Collectors.toList;
import static org.apache.streamline.common.util.WSUtils.CURRENT_VERSION;
import static org.apache.streamline.common.util.WSUtils.currentVersionQueryParam;
import static org.apache.streamline.common.util.WSUtils.versionIdQueryParam;
import static org.apache.streamline.streams.catalog.TopologyEdge.StreamGrouping;

/**
 * A service layer where we could put our business logic.
 * Right now this exists as a very thin layer between the DAO and
 * the REST controllers.
 */
public class StreamCatalogService {

    private static final Logger LOG = LoggerFactory.getLogger(StreamCatalogService.class);

    // TODO: the namespace and Id generation logic should be moved inside DAO
    private static final String CLUSTER_NAMESPACE = new Cluster().getNameSpace();
    private static final String SERVICE_NAMESPACE = new Service().getNameSpace();
    private static final String COMPONENT_NAMESPACE = new Component().getNameSpace();
    private static final String SERVICE_CONFIGURATION_NAMESPACE = new ServiceConfiguration().getNameSpace();
    private static final String NOTIFIER_INFO_NAMESPACE = new NotifierInfo().getNameSpace();
    private static final String TOPOLOGY_NAMESPACE = new Topology().getNameSpace();
    private static final String TOPOLOGY_VERSIONINFO_NAMESPACE = new TopologyVersionInfo().getNameSpace();
    private static final String STREAMINFO_NAMESPACE = new StreamInfo().getNameSpace();
    private static final String TOPOLOGY_COMPONENT_NAMESPACE = new TopologyComponent().getNameSpace();
    private static final String TOPOLOGY_SOURCE_NAMESPACE = new TopologySource().getNameSpace();
    private static final String TOPOLOGY_SOURCE_STREAM_MAPPING_NAMESPACE = new TopologySourceStreamMapping().getNameSpace();
    private static final String TOPOLOGY_SINK_NAMESPACE = new TopologySink().getNameSpace();
    private static final String TOPOLOGY_PROCESSOR_NAMESPACE = new TopologyProcessor().getNameSpace();
    private static final String TOPOLOGY_PROCESSOR_STREAM_MAPPING_NAMESPACE = new TopologyProcessorStreamMapping().getNameSpace();
    private static final String TOPOLOGY_EDGE_NAMESPACE = new TopologyEdge().getNameSpace();
    private static final String TOPOLOGY_RULEINFO_NAMESPACE = new RuleInfo().getNameSpace();
    private static final String TOPOLOGY_BRANCHRULEINFO_NAMESPACE = new BranchRuleInfo().getNameSpace();
    private static final String TOPOLOGY_WINDOWINFO_NAMESPACE = new WindowInfo().getNameSpace();
    private static final String UDF_NAMESPACE = new UDFInfo().getNameSpace();
    private static final String NAMESPACE_NAMESPACE = new Namespace().getNameSpace();
    private static final String NAMESPACE_SERVICE_CLUSTER_MAPPING_NAMESPACE = new NamespaceServiceClusterMapping().getNameSpace();

    private static final ArrayList<Class<?>> UDF_CLASSES = Lists.newArrayList(UDAF.class, UDAF2.class, UDF.class, UDF2.class,
                                                                              UDF3.class, UDF4.class, UDF5.class, UDF6.class, UDF7.class);

    
    private final StorageManager dao;
    private final TopologyActionsContainer topologyActionsContainer;
    private final TopologyMetricsContainer topologyMetricsContainer;
    private final FileStorage fileStorage;
    private final TopologyDagBuilder topologyDagBuilder;
    private final ConfigFileWriter configFileWriter;

    public StreamCatalogService(StorageManager dao, FileStorage fileStorage, Map<String, Object> configuration) {
        this.dao = dao;
        dao.registerStorables(getStorableClasses());
        this.fileStorage = fileStorage;
        this.topologyDagBuilder = new TopologyDagBuilder(this);
        this.configFileWriter = new ConfigFileWriter();

        Map<String, String> conf = new HashMap<>();
        for (Map.Entry<String, Object> confEntry : configuration.entrySet()) {
            Object value = confEntry.getValue();
            conf.put(confEntry.getKey(), value == null ? null : value.toString());
        }
        this.topologyActionsContainer = new TopologyActionsContainer(this, conf);
        this.topologyMetricsContainer = new TopologyMetricsContainer(this);
    }

    public static Collection<Class<? extends Storable>> getStorableClasses() {
        InputStream resourceAsStream = StreamCatalogService.class.getClassLoader().getResourceAsStream("streamcatalogstorables.props");
        HashSet<Class<? extends Storable>> classes = new HashSet<>();
        try {
            List<String> classNames = IOUtils.readLines(resourceAsStream);
            for (String className : classNames) {
                classes.add((Class<? extends Storable>) Class.forName(className));
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return classes;
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

    public Cluster getClusterByName(String clusterName) {
        Collection<Cluster> clusters = listClusters(Lists.newArrayList(new QueryParam("name", clusterName)));
        if (clusters.size() > 1) {
            LOG.warn("Multiple Clusters have same name: {} returning first match.", clusterName);
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

    public Map<String, Object> getDeserializedConfiguration(String clusterName, String serviceName,
                                                            String serviceConfiguraionName) throws IOException {
        Cluster cluster = getClusterByName(clusterName);
        if (cluster == null) {
            return null;
        }

        Service service = getServiceByName(cluster.getId(), serviceName);
        if (service == null) {
            return null;
        }

        ServiceConfiguration serviceConfiguration = getServiceConfigurationByName(service.getId(), serviceConfiguraionName);
        if (serviceConfiguration == null) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(serviceConfiguration.getConfiguration(), Map.class);
    }

    public Object lookupConfiguration(String clusterName, String serviceName,
                                      String serviceConfiguraionName, String configKey) throws IOException {
        Cluster cluster = getClusterByName(clusterName);
        if (cluster == null) {
            return null;
        }

        Service service = getServiceByName(cluster.getId(), serviceName);
        if (service == null) {
            return null;
        }

        ServiceConfiguration serviceConfiguration = getServiceConfigurationByName(service.getId(), serviceConfiguraionName);
        if (serviceConfiguration == null) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> conf = objectMapper.readValue(serviceConfiguration.getConfiguration(), Map.class);
        return conf.get(configKey);
    }

    public NotifierInfo addNotifierInfo(NotifierInfo notifierInfo) {
        if (notifierInfo.getId() == null) {
            notifierInfo.setId(this.dao.nextId(NOTIFIER_INFO_NAMESPACE));
        }
        if (notifierInfo.getTimestamp() == null) {
            notifierInfo.setTimestamp(System.currentTimeMillis());
        }
        if (StringUtils.isEmpty(notifierInfo.getName())) {
            throw new StorageException("Notifier name empty");
        }
        this.dao.add(notifierInfo);
        return notifierInfo;
    }

    public NotifierInfo getNotifierInfo(Long id) {
        NotifierInfo notifierInfo = new NotifierInfo();
        notifierInfo.setId(id);
        return this.dao.get(new StorableKey(NOTIFIER_INFO_NAMESPACE, notifierInfo.getPrimaryKey()));
    }

    public Collection<NotifierInfo> listNotifierInfos() {
        return this.dao.list(NOTIFIER_INFO_NAMESPACE);
    }

    public Collection<NotifierInfo> listNotifierInfos(List<QueryParam> params) throws Exception {
        return dao.find(NOTIFIER_INFO_NAMESPACE, params);
    }


    public NotifierInfo removeNotifierInfo(Long notifierId) {
        NotifierInfo notifierInfo = new NotifierInfo();
        notifierInfo.setId(notifierId);
        return dao.remove(new StorableKey(NOTIFIER_INFO_NAMESPACE, notifierInfo.getPrimaryKey()));
    }


    public NotifierInfo addOrUpdateNotifierInfo(Long id, NotifierInfo notifierInfo) {
        notifierInfo.setId(id);
        notifierInfo.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(notifierInfo);
        return notifierInfo;
    }

    public Collection<TopologyVersionInfo> listCurrentTopologyVersionInfos() {
        return listTopologyVersionInfos(currentVersionQueryParam());
    }

    public Collection<TopologyVersionInfo> listTopologyVersionInfos(List<QueryParam> queryParams) {
        return dao.find(TOPOLOGY_VERSIONINFO_NAMESPACE, queryParams);
    }


    public Optional<TopologyVersionInfo> getCurrentTopologyVersionInfo(Long topologyId) {
        Collection<TopologyVersionInfo> versions = listTopologyVersionInfos(
                WSUtils.currentTopologyVersionQueryParam(topologyId, null));
        if (versions.isEmpty()) {
            LOG.warn("No current version for topology " + topologyId);
            return Optional.empty();
        } else if (versions.size() > 1) {
            throw new IllegalStateException("More than one 'CURRENT' version for topology id: " + topologyId);
        }
        return Optional.of(versions.iterator().next());
    }

    // latest version before the CURRENT version
    public Optional<TopologyVersionInfo> getLatestVersionInfo(Long topologyId) {
        Collection<TopologyVersionInfo> versions =
                listTopologyVersionInfos(WSUtils.buildTopologyIdAwareQueryParams(topologyId, null));
        return  versions.stream()
                .filter(v -> !v.getName().equals(CURRENT_VERSION))
                .max((versionInfo1, versionInfo2) -> {
                    // compares the number part from version strings like V1, V2 ...
                    return versionInfo1.getVersionNumber() - versionInfo2.getVersionNumber();
                });
    }
    public TopologyVersionInfo getTopologyVersionInfo(Long versionId) {
        TopologyVersionInfo topologyVersionInfo = new TopologyVersionInfo();
        topologyVersionInfo.setId(versionId);
        return dao.get(topologyVersionInfo.getStorableKey());
    }

    public TopologyVersionInfo addTopologyVersionInfo(TopologyVersionInfo topologyVersionInfo) {
        if (topologyVersionInfo.getId() == null) {
            topologyVersionInfo.setId(this.dao.nextId(TOPOLOGY_VERSIONINFO_NAMESPACE));
        }
        if (topologyVersionInfo.getTimestamp() == null) {
            topologyVersionInfo.setTimestamp(System.currentTimeMillis());
        }
        dao.add(topologyVersionInfo);
        return topologyVersionInfo;
    }

    public TopologyVersionInfo addOrUpdateTopologyVersionInfo(Long versionId, TopologyVersionInfo topologyVersionInfo) {
        topologyVersionInfo.setId(versionId);
        topologyVersionInfo.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(topologyVersionInfo);
        return topologyVersionInfo;
    }

    public Long getVersionTimestamp(Long versionId) {
        TopologyVersionInfo versionInfo = getTopologyVersionInfo(versionId);
        if (versionInfo == null) {
            throw new IllegalArgumentException("No version with versionId " + versionId);
        }
        return versionInfo.getTimestamp();
    }

    public TopologyVersionInfo updateVersionTimestamp(Long versionId) {
        return updateVersionTimestamp(versionId, System.currentTimeMillis());
    }

    public TopologyVersionInfo updateVersionTimestamp(Long versionId, Long timestamp) {
        TopologyVersionInfo topologyVersionInfo = getTopologyVersionInfo(versionId);
        if (topologyVersionInfo == null) {
            throw new IllegalStateException("No version with version Id " + versionId);
        }
        topologyVersionInfo.setTimestamp(timestamp);
        dao.addOrUpdate(topologyVersionInfo);
        return topologyVersionInfo;
    }

    public TopologyVersionInfo removeTopologyVersionInfo(Long versionId) {
        TopologyVersionInfo topologyVersionInfo = new TopologyVersionInfo();
        topologyVersionInfo.setId(versionId);
        return dao.remove(new StorableKey(TOPOLOGY_VERSIONINFO_NAMESPACE, topologyVersionInfo.getPrimaryKey()));
    }

    /**
     * Lists the 'CURRENT' version of topologies
     */
    public Collection<Topology> listTopologies() {
        List<Topology> topologies = new ArrayList<>();
        for (TopologyVersionInfo version: listCurrentTopologyVersionInfos()) {
            topologies.addAll(listTopologies(version.getId()));
        }
        return topologies;
    }

    private Collection<Topology> listTopologies(Long versionId) {
        Collection<Topology> topologies = this.dao.find(TOPOLOGY_NAMESPACE, versionIdQueryParam(versionId));
        Long versionTimestamp = getVersionTimestamp(versionId);
        topologies.forEach(x -> x.setVersionTimestamp(versionTimestamp));
        return topologies;
    }

    public Collection<Topology> listTopologies(List<QueryParam> queryParams) {
        Collection<Topology> topologies = this.dao.find(TOPOLOGY_NAMESPACE, queryParams);
        topologies.forEach(t -> t.setVersionTimestamp(getVersionTimestamp(t.getVersionId())));
        return topologies;
    }

    public Long getCurrentVersionId(Long topologyId) {
        Optional<TopologyVersionInfo> versionInfo = getCurrentTopologyVersionInfo(topologyId);
        return versionInfo.isPresent() ? versionInfo.get().getId() : -1L;
    }

    /**
     * returns the 'CURRENT' version of the topology with given topologyId
     */
    public Topology getTopology(Long topologyId) {
        return getTopology(topologyId, getCurrentVersionId(topologyId));
    }

    public Topology getTopology(Long topologyId, Long versionId) {
        Topology topology = new Topology();
        topology.setId(topologyId);
        topology.setVersionId(versionId);
        Topology result = this.dao.get(topology.getStorableKey());
        if (result != null) {
            result.setVersionTimestamp(getVersionTimestamp(versionId));
        }
        return result;
    }

    public Topology addTopology(Topology topology) {
        if (topology.getId() == null) {
            topology.setId(this.dao.nextId(TOPOLOGY_NAMESPACE));
        }
        long timestamp = System.currentTimeMillis();
        topology.setVersionTimestamp(timestamp);
        TopologyVersionInfo versionInfo = addCurrentTopologyVersionInfo(topology.getId(), timestamp);
        LOG.debug("Added version info {}", versionInfo);
        topology.setVersionId(versionInfo.getId());
        validateTopology(topology);
        this.dao.add(topology);
        LOG.debug("Added topology {}", topology);
        return topology;
    }

    // create a 'CURRENT' version for given topology id
    private TopologyVersionInfo addCurrentTopologyVersionInfo(Long topologyId, Long timestamp) {
        TopologyVersionInfo versionInfo = new TopologyVersionInfo();
        versionInfo.setName(CURRENT_VERSION);
        versionInfo.setDescription("");
        versionInfo.setTimestamp(timestamp);
        versionInfo.setTopologyId(topologyId);
        return addTopologyVersionInfo(versionInfo);
    }

    /**
     * removes the 'CURRENT' version of the topology with the given id.
     */
    public Topology removeTopology(Long topologyId, boolean recurse) {
        return removeTopology(topologyId, getCurrentVersionId(topologyId), recurse);
    }

    public Topology removeTopology(Long topologyId, Long versionId, boolean recurse) {
        Topology topology = new Topology();
        topology.setId(topologyId);
        topology.setVersionId(versionId);
        if (recurse) {
            try {
                removeTopologyDependencies(topology.getId(), topology.getVersionId());
            } catch (Exception ex) {
                LOG.error("Got exception while removing topology dependencies", ex);
                throw new RuntimeException(ex);
            }
        }
        Topology removedTopology = dao.remove(topology.getStorableKey());
        removeTopologyVersionInfo(versionId);
        return removedTopology;
    }

    private void removeTopologyDependencies(Long topologyId, Long versionId) throws Exception {
        List<QueryParam> topologyIdVersionIdQueryParams = WSUtils.buildTopologyIdAndVersionIdAwareQueryParams(
                topologyId, versionId, null);
        // remove edges
        Collection<TopologyEdge> edges = listTopologyEdges(topologyIdVersionIdQueryParams);
        for (TopologyEdge edge: edges) {
            removeTopologyEdge(topologyId, edge.getId(), versionId);
        }

        // remove rules
        Collection<RuleInfo> ruleInfos = listRules(topologyIdVersionIdQueryParams);
        for (RuleInfo ruleInfo: ruleInfos) {
            removeRule(topologyId, ruleInfo.getId(), versionId);
        }

        // remove windowed rules
        Collection<WindowInfo> windowInfos = listWindows(topologyIdVersionIdQueryParams);
        for (WindowInfo windowInfo: windowInfos) {
            removeWindow(topologyId, windowInfo.getId(), versionId);
        }

        // remove branch rules
        Collection<BranchRuleInfo> branchRuleInfos = listBranchRules(topologyIdVersionIdQueryParams);
        for (BranchRuleInfo branchRuleInfo: branchRuleInfos) {
            removeBranchRule(topologyId, branchRuleInfo.getId(), versionId);
        }

        // remove sinks
        Collection<TopologySink> sinks = listTopologySinks(topologyIdVersionIdQueryParams);
        for (TopologySink sink : sinks) {
            removeTopologySink(topologyId, sink.getId(), versionId);
        }

        // remove processors
        Collection<TopologyProcessor> processors = listTopologyProcessors(topologyIdVersionIdQueryParams);
        for (TopologyProcessor processor: processors) {
            removeTopologyProcessor(topologyId, processor.getId(), versionId);
        }

        // remove sources
        Collection<TopologySource> sources = listTopologySources(topologyIdVersionIdQueryParams);
        for (TopologySource source : sources) {
            removeTopologySource(topologyId, source.getId(), versionId);
        }

        // remove output streams
        Collection<StreamInfo> streamInfos = listStreamInfos(topologyIdVersionIdQueryParams);
        for (StreamInfo streamInfo: streamInfos) {
            removeStreamInfo(topologyId, streamInfo.getId(), versionId);
        }

        // remove topology editor metadata
        removeTopologyEditorMetadata(topologyId, versionId);
    }

    /**
     * Clones the given version of the topology and all its dependencies to a new 'CURRENT' version.
     * The ids of the topology and its dependencies are retained.
     */
    public Topology cloneTopologyVersion(Long topologyId, Long versionId) {
        Topology topology = getTopology(topologyId, versionId);
        if (topology != null) {
            try {
                topology = addTopology(new Topology(topology));
                copyTopologyDependencies(topologyId, versionId, topology.getVersionId());
            } catch (Exception ex) {
                LOG.error("Got exception while copying topology dependencies", ex);
                if (topology != null) {
                    removeTopology(topology.getId(), topology.getVersionId(), true);
                }
                throw new RuntimeException(ex);
            }
        }
        return topology;
    }

    private void copyTopologyDependencies(Long topologyId, Long oldVersionId, Long newVersionId) throws Exception {
        List<QueryParam> topologyIdVersionIdQueryParams = WSUtils.buildTopologyIdAndVersionIdAwareQueryParams(
                topologyId, oldVersionId, null);

        // topology editor metadata
        TopologyEditorMetadata metadata = getTopologyEditorMetadata(topologyId, oldVersionId);
        if (metadata != null) {
            addTopologyEditorMetadata(topologyId, newVersionId, new TopologyEditorMetadata(metadata));
        }

        // sources, output streams
        Collection<TopologySource> sources = listTopologySources(topologyIdVersionIdQueryParams);
        for (TopologySource source : sources) {
            addTopologySource(topologyId, newVersionId, new TopologySource(source));
        }

        // processors, output streams
        Collection<TopologyProcessor> processors = listTopologyProcessors(topologyIdVersionIdQueryParams);
        for (TopologyProcessor processor: processors) {
            addTopologyProcessor(topologyId, newVersionId, new TopologyProcessor(processor));
        }

        // add sinks
        Collection<TopologySink> sinks = listTopologySinks(topologyIdVersionIdQueryParams);
        for (TopologySink sink : sinks) {
            addTopologySink(topologyId, newVersionId, new TopologySink(sink));
        }

        // branch rules
        Collection<BranchRuleInfo> branchRuleInfos = listBranchRules(topologyIdVersionIdQueryParams);
        for (BranchRuleInfo branchRuleInfo: branchRuleInfos) {
            addBranchRule(topologyId, newVersionId, new BranchRuleInfo(branchRuleInfo));
        }

        // windowed rules
        Collection<WindowInfo> windowInfos = listWindows(topologyIdVersionIdQueryParams);
        for (WindowInfo windowInfo: windowInfos) {
            addWindow(topologyId, newVersionId, new WindowInfo(windowInfo));
        }

        // rules
        Collection<RuleInfo> ruleInfos = listRules(topologyIdVersionIdQueryParams);
        for (RuleInfo ruleInfo: ruleInfos) {
            addRule(topologyId, newVersionId, new RuleInfo(ruleInfo));
        }

        // add edges
        Collection<TopologyEdge> edges = listTopologyEdges(topologyIdVersionIdQueryParams);
        for (TopologyEdge edge: edges) {
            addTopologyEdge(topologyId, newVersionId, new TopologyEdge(edge));
        }
    }

    public Topology addOrUpdateTopology(Long topologyId, Topology topology) {
        return addOrUpdateTopology(topologyId, getCurrentVersionId(topologyId), topology);
    }

    private Topology addOrUpdateTopology(Long topologyId, Long versionId, Topology topology) {
        topology.setId(topologyId);
        topology.setVersionId(versionId);
        long timestamp = System.currentTimeMillis();
        topology.setVersionTimestamp(timestamp);
        validateTopology(topology);
        this.dao.addOrUpdate(topology);
        updateVersionTimestamp(versionId, timestamp);
        return topology;
    }

    public TopologyComponent getTopologyComponent(Long topologyId, Long topologyComponentId) {
        TopologyComponent topologyComponent = getTopologySource(topologyId, topologyComponentId);
        if (topologyComponent == null) {
            topologyComponent = getTopologyProcessor(topologyId, topologyComponentId);
            if (topologyComponent == null) {
                topologyComponent = getTopologySink(topologyId, topologyComponentId);
            }
        }
        return topologyComponent;
    }

    public Topology validateTopology(URL schema, Long topologyId)
            throws Exception {
        Topology ds = new Topology();
        ds.setId(topologyId);
        Topology result = this.dao.get(ds.getStorableKey());
        boolean isValidAsPerSchema;
        if (result != null) {
            String json = result.getConfig();
            // first step is to validate against json schema provided
            isValidAsPerSchema = JsonSchemaValidator
                    .isValidJsonAsPerSchema(schema, json);

            if (!isValidAsPerSchema) {
                throw new ComponentConfigException("Topology with id "
                        + topologyId + " failed to validate against json "
                        + "schema");
            }
            // if first step succeeds, proceed to other validations that
            // cannot be covered using json schema
            TopologyLayoutValidator validator = new TopologyLayoutValidator(json);
            validator.validate();

            TopologyActions topologyActions = getTopologyActionsInstance(ds);

            // finally pass it on for streaming engine based config validations
            topologyActions.validate(getTopologyLayout(result));
        }
        return result;
    }

    public void deployTopology(Topology topology) throws Exception {
        TopologyDag dag = topologyDagBuilder.getDag(topology);
        topology.setTopologyDag(dag);
        ensureValid(dag);
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        LOG.debug("Deploying topology {}", topology);
        setUpClusterArtifacts(topology, topologyActions);
        setUpExtraJars(topology, topologyActions);
        topologyActions.deploy(getTopologyLayout(topology));
    }

    /* Should have
     * at-least 1 processor
     * OR
     * at-least 1 source with an edge.
     */
    private void ensureValid(TopologyDag dag) {
        if (dag.getComponents().isEmpty()) {
            throw new IllegalStateException("Empty topology");
        }
        java.util.Optional<OutputComponent> processor = dag.getOutputComponents().stream()
                .filter(x -> x instanceof StreamlineProcessor)
                .findFirst();
        if (!processor.isPresent()) {
            java.util.Optional<OutputComponent> sourceWithOutgoingEdge = dag.getOutputComponents().stream()
                    .filter(x -> x instanceof StreamlineSource && !dag.getEdgesFrom(x).isEmpty())
                    .findFirst();
            if (!sourceWithOutgoingEdge.isPresent()) {
                throw new IllegalStateException("Topology does not contain a processor or a source with an outgoing edge");
            }
        }
    }

    public String exportTopology(Topology topology) throws Exception {
        Preconditions.checkNotNull(topology);
        TopologyData topologyData = doExportTopology(topology);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(topologyData);
    }

    private TopologyData doExportTopology(Topology topology) throws Exception {
        TopologyDag dag = topologyDagBuilder.getDag(topology);
        topology.setTopologyDag(dag);
        TopologyData topologyData = new TopologyData();
        TopologyExportVisitor exportVisitor = new TopologyExportVisitor(topology.getId(), topologyData, this);
        topologyData.setTopologyName(topology.getName());
        topologyData.setConfig(topology.getConfig());
        TopologyDag topologyDag = topology.getTopologyDag();
        if (topologyDag != null) {
            topologyDag.traverse(exportVisitor);
        }
        topologyData.setMetadata(getTopologyEditorMetadata(topology.getId()));
        return topologyData;
    }

    private List<Long> importOutputStreams(Long newTopologyId, Map<Long, Long> oldToNewStreamIds, List<StreamInfo> streams) {
        List<Long> importedOutputStreamIds = new ArrayList<>();
        for (StreamInfo stream : streams) {
            Long oldId = stream.getId();
            Long newId = oldToNewStreamIds.get(oldId);
            if (newId == null) {
                stream.setId(null);
                StreamInfo addedStreamInfo = addStreamInfo(newTopologyId, stream);
                newId = addedStreamInfo.getId();
                oldToNewStreamIds.put(oldId, newId);
            }
            importedOutputStreamIds.add(newId);
        }
        return importedOutputStreamIds;
    }

    private TopologyComponentBundle getCurrentTopologyComponentBundle(TopologyComponentBundle.TopologyComponentType type, String subType) {
        Collection<TopologyComponentBundle> bundles = listTopologyComponentBundlesForTypeWithFilter(type, Collections.singletonList(
                new QueryParam(TopologyComponentBundle.SUB_TYPE, subType)
        ));
        if (bundles.size() != 1) {
            throw new IllegalStateException("Not able to find topology component bundle for type " + type
            + " sub type " + subType);
        }
        return bundles.iterator().next();
    }

    private Topology doImportTopology(Topology newTopology, TopologyData topologyData) throws Exception {
        List<TopologySource> topologySources = topologyData.getSources();
        Map<Long, Long> oldToNewComponentIds = new HashMap<>();
        Map<Long, Long> oldToNewRuleIds = new HashMap<>();
        Map<Long, Long> oldToNewWindowIds = new HashMap<>();
        Map<Long, Long> oldToNewBranchRuleIds = new HashMap<>();
        Map<Long, Long> oldToNewStreamIds = new HashMap<>();

        // import source streams
        for (TopologySource topologySource : topologySources) {
            topologySource.setOutputStreamIds(importOutputStreams(newTopology.getId(),
                    oldToNewStreamIds, topologySource.getOutputStreams()));
            topologySource.setOutputStreams(null);
        }

        // import processor streams
        for (TopologyProcessor topologyProcessor : topologyData.getProcessors()) {
            topologyProcessor.setOutputStreamIds(importOutputStreams(newTopology.getId(),
                    oldToNewStreamIds, topologyProcessor.getOutputStreams()));
            topologyProcessor.setOutputStreams(null);
        }

        // import rules
        for (RuleInfo rule : topologyData.getRules()) {
            Long currentId = rule.getId();
            rule.setId(null);
            RuleInfo addedRule = addRule(newTopology.getId(), rule);
            oldToNewRuleIds.put(currentId, addedRule.getId());
        }

        // import windowed rules
        for (WindowInfo window : topologyData.getWindows()) {
            Long currentId = window.getId();
            window.setId(null);
            WindowInfo addedWindow = addWindow(newTopology.getId(), window);
            oldToNewWindowIds.put(currentId, addedWindow.getId());
        }

        // import branch rules
        for (BranchRuleInfo branchRule : topologyData.getBranchRules()) {
            Long currentId = branchRule.getId();
            branchRule.setId(null);
            BranchRuleInfo addedBranchRule = addBranchRule(newTopology.getId(), branchRule);
            oldToNewBranchRuleIds.put(currentId, addedBranchRule.getId());
        }

        // import sources
        for (TopologySource topologySource : topologySources) {
            Long oldComponentId = topologySource.getId();
            topologySource.setId(null);
            topologySource.setTopologyId(newTopology.getId());
            TopologyComponentBundle bundle = getCurrentTopologyComponentBundle(
                    TopologyComponentBundle.TopologyComponentType.SOURCE,
                    topologyData.getBundleIdToType().get(topologySource.getTopologyComponentBundleId().toString()));
            topologySource.setTopologyComponentBundleId(bundle.getId());
            addTopologySource(newTopology.getId(), topologySource);
            oldToNewComponentIds.put(oldComponentId, topologySource.getId());
        }

        // import processors
        for (TopologyProcessor topologyProcessor : topologyData.getProcessors()) {
            Long oldComponentId = topologyProcessor.getId();
            topologyProcessor.setId(null);
            topologyProcessor.setTopologyId(newTopology.getId());
            TopologyComponentBundle bundle = getCurrentTopologyComponentBundle(
                    TopologyComponentBundle.TopologyComponentType.PROCESSOR,
                    topologyData.getBundleIdToType().get(topologyProcessor.getTopologyComponentBundleId().toString()));
            topologyProcessor.setTopologyComponentBundleId(bundle.getId());
            Optional<Object> ruleListObj = topologyProcessor.getConfig().getAnyOptional(RulesProcessor.CONFIG_KEY_RULES);
            ruleListObj.ifPresent(ruleList -> {
                List<Long> ruleIds = new ObjectMapper().convertValue(ruleList, new TypeReference<List<Long>>() {});
                List<Long> updatedRuleIds = new ArrayList<>();
                if (bundle.getSubType().equals(ComponentTypes.RULE)) {
                    ruleIds.forEach(ruleId -> updatedRuleIds.add(oldToNewRuleIds.get(ruleId)));
                } else if (bundle.getSubType().equals(ComponentTypes.BRANCH)) {
                    ruleIds.forEach(ruleId -> updatedRuleIds.add(oldToNewBranchRuleIds.get(ruleId)));
                } else if (bundle.getSubType().equals(ComponentTypes.WINDOW)) {
                    ruleIds.forEach(ruleId -> updatedRuleIds.add(oldToNewWindowIds.get(ruleId)));
                }
                topologyProcessor.getConfig().setAny(RulesProcessor.CONFIG_KEY_RULES, updatedRuleIds);
            });
            addTopologyProcessor(newTopology.getId(), topologyProcessor);
            oldToNewComponentIds.put(oldComponentId, topologyProcessor.getId());
        }

        // import sinks
        for (TopologySink topologySink : topologyData.getSinks()) {
            topologySink.setTopologyId(newTopology.getId());
            Long currentId = topologySink.getId();
            topologySink.setId(null);
            TopologyComponentBundle bundle = getCurrentTopologyComponentBundle(
                    TopologyComponentBundle.TopologyComponentType.SINK,
                    topologyData.getBundleIdToType().get(topologySink.getTopologyComponentBundleId().toString()));
            topologySink.setTopologyComponentBundleId(bundle.getId());
            addTopologySink(newTopology.getId(), topologySink);
            oldToNewComponentIds.put(currentId, topologySink.getId());
        }

        // import edges
        for (TopologyEdge topologyEdge : topologyData.getEdges()) {
            List<StreamGrouping> streamGroupings = topologyEdge.getStreamGroupings();
            for (StreamGrouping streamGrouping : streamGroupings) {
                Long newStreamId = oldToNewStreamIds.get(streamGrouping.getStreamId());
                streamGrouping.setStreamId(newStreamId);
            }
            topologyEdge.setId(null);
            topologyEdge.setTopologyId(newTopology.getId());
            topologyEdge.setFromId(oldToNewComponentIds.get(topologyEdge.getFromId()));
            topologyEdge.setToId(oldToNewComponentIds.get(topologyEdge.getToId()));
            addTopologyEdge(newTopology.getId(), topologyEdge);
        }

        // import topology editor metadata
        TopologyEditorMetadata topologyEditorMetadata = topologyData.getTopologyEditorMetadata();
        topologyEditorMetadata.setTopologyId(newTopology.getId());
        TopologyUIData topologyUIData = new ObjectMapper().readValue(topologyEditorMetadata.getData(), TopologyUIData.class);
        topologyUIData.getSources().forEach(c -> c.setId(oldToNewComponentIds.get(c.getId())));
        topologyUIData.getProcessors().forEach(c -> c.setId(oldToNewComponentIds.get(c.getId())));
        topologyUIData.getSinks().forEach(c -> c.setId(oldToNewComponentIds.get(c.getId())));
        topologyEditorMetadata.setData(new ObjectMapper().writeValueAsString(topologyUIData));
        addTopologyEditorMetadata(newTopology.getId(), topologyData.getTopologyEditorMetadata());
        return newTopology;
    }

    public Topology importTopology(TopologyData topologyData) throws Exception {
        Preconditions.checkNotNull(topologyData);
        Topology newTopology = new Topology();
        try {
            newTopology.setName(topologyData.getTopologyName());
            newTopology.setConfig(topologyData.getConfig());
            addTopology(newTopology);
            doImportTopology(newTopology, topologyData);
        } catch (Exception ex) {
            LOG.error("Got exception while importing the topology", ex);
            removeTopology(newTopology.getId(), true);
            throw ex;
        }
        return newTopology;
    }

    public Topology cloneTopology(Topology topology) throws Exception {
        Preconditions.checkNotNull(topology, "Topology does not exist");
        TopologyData exported = new TopologyData(doExportTopology(topology));
        exported.setTopologyName(exported.getTopologyName() + "-clone");
        return importTopology(exported);
    }

    private void setUpExtraJars(Topology topology, TopologyActions topologyActions) throws IOException {
        StormTopologyExtraJarsHandler extraJarsHandler = new StormTopologyExtraJarsHandler(this);
        topology.getTopologyDag().traverse(extraJarsHandler);
        Path extraJarsLocation = topologyActions.getExtraJarsLocation(getTopologyLayout(topology));
        makeEmptyDir(extraJarsLocation);
        Set<String> extraJars = new HashSet<>();
        extraJars.addAll(extraJarsHandler.getExtraJars());
        extraJars.addAll(getBundleJars(getTopologyLayout(topology)));
        downloadAndCopyJars(extraJars, extraJarsLocation);
    }

    private Set<String> getBundleJars (TopologyLayout topologyLayout) throws IOException {
        TopologyComponentBundleJarHandler topologyComponentBundleJarHandler = new TopologyComponentBundleJarHandler(this);
        topologyLayout.getTopologyDag().traverse(topologyComponentBundleJarHandler);
        Set<TopologyComponentBundle> bundlesToDeploy = topologyComponentBundleJarHandler.getTopologyComponentBundleSet();
        Set<String> bundleJars = new HashSet<>();
        for (TopologyComponentBundle topologyComponentBundle: bundlesToDeploy) {
            bundleJars.add(topologyComponentBundle.getBundleJar());
        }
        return bundleJars;
    }

    private void downloadAndCopyJars (Set<String> jarsToDownload, Path destinationPath) throws IOException {
        Set<String> copiedJars = new HashSet<>();
        for (String jar: jarsToDownload) {
            if (!copiedJars.contains(jar)) {
                File destPath = Paths.get(destinationPath.toString(), Paths.get(jar).getFileName().toString()).toFile();
                try (InputStream src = fileStorage.downloadFile(jar);
                     FileOutputStream dest = new FileOutputStream(destPath)
                ) {
                    IOUtils.copy(src, dest);
                    copiedJars.add(jar);
                    LOG.debug("Jar {} copied to {}", jar, destPath);
                }
            }
        }
    }

    private void setUpClusterArtifacts(Topology topology, TopologyActions topologyActions) throws IOException {
        Namespace namespace = getNamespace(topology.getNamespaceId());
        if (namespace == null) {
            throw new RuntimeException("Corresponding namespace not found: " + topology.getNamespaceId());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Path artifactsDir = topologyActions.getArtifactsLocation(getTopologyLayout(topology));
        makeEmptyDir(artifactsDir);

        Collection<NamespaceServiceClusterMapping> serviceClusterMappings = listServiceClusterMapping(namespace.getId());
        for (NamespaceServiceClusterMapping serviceClusterMapping : serviceClusterMappings) {
            Service service = getServiceByName(serviceClusterMapping.getClusterId(),
                    serviceClusterMapping.getServiceName());
            if (service != null) {
                Collection<ServiceConfiguration> serviceConfigurations = listServiceConfigurations(service.getId());
                if (serviceConfigurations != null) {
                    for (ServiceConfiguration serviceConfiguration : serviceConfigurations) {
                        writeConfigurationFile(objectMapper, artifactsDir, serviceConfiguration);
                    }
                }
            }
        }
    }

    private void makeEmptyDir(Path path) throws IOException {
        if (path.toFile().exists()) {
            if (path.toFile().isDirectory()) {
                FileUtils.cleanDirectory(path.toFile());
            } else {
                final String errorMessage = String.format("Location '%s' must be a directory.", path);
                LOG.error(errorMessage);
                throw new IOException(errorMessage);
            }
        } else if (!path.toFile().mkdirs()) {
            LOG.error("Could not create dir {}", path);
            throw new IOException("Could not create dir: " + path);
        }
    }

    // Only known configuration files will be saved to local
    private void writeConfigurationFile(ObjectMapper objectMapper, Path artifactsDir,
                                        ServiceConfiguration configuration) throws IOException {
        String filename = configuration.getFilename();
        if (filename != null && !filename.isEmpty()) {
            // Configuration itself is aware of file name
            ConfigFileType fileType = ConfigFileType.getFileTypeFromFileName(filename);

            if (fileType != null) {
                File destPath = Paths.get(artifactsDir.toString(), filename).toFile();

                Map<String, Object> conf = objectMapper.readValue(configuration.getConfiguration(), Map.class);

                try {
                    configFileWriter.writeConfigToFile(fileType, conf, destPath);
                    LOG.debug("Resource {} written to {}", filename, destPath);
                } catch (IllegalArgumentException e) {
                    LOG.warn("Don't know how to write resource {} skipping...", filename);
                }
            }
        }
    }

    public void killTopology(Topology topology) throws Exception {
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        topologyActions.kill(getTopologyLayout(topology));
    }

    public void suspendTopology(Topology topology) throws Exception {
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        topologyActions.suspend(getTopologyLayout(topology));
    }

    public void resumeTopology(Topology topology) throws Exception {
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        topologyActions.resume(getTopologyLayout(topology));
    }

    public TopologyActions.Status topologyStatus(Topology topology) throws Exception {
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        return topologyActions.status(getTopologyLayout(topology));
    }

    public String getRuntimeTopologyId(Topology topology) throws IOException {
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        return topologyActions.getRuntimeTopologyId(getTopologyLayout(topology));
    }

    public Map<String, TopologyMetrics.ComponentMetric> getTopologyMetrics(Topology topology) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getMetricsForTopology(getTopologyLayout(topology));
    }

    public Map<Long, Double> getCompleteLatency(Topology topology, TopologyComponent component, long from, long to) throws Exception {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getCompleteLatency(getTopologyLayout(topology), getComponentLayout(component), from, to);
    }

    public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getTopologyStats(Topology topology, Long from, Long to) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getTopologyStats(getTopologyLayout(topology), from, to);
    }

    public TopologyTimeSeriesMetrics.TimeSeriesComponentMetric getComponentStats(Topology topology, TopologyComponent component, Long from, Long to) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getComponentStats(getTopologyLayout(topology), getComponentLayout(component), from, to);
    }

    public Map<String, Map<Long, Double>> getKafkaTopicOffsets(Topology topology, TopologyComponent component, Long from, Long to) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getkafkaTopicOffsets(getTopologyLayout(topology), getComponentLayout(component), from, to);
    }

    public TopologyMetrics.TopologyMetric getTopologyMetric(Topology topology) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        return topologyMetrics.getTopologyMetric(getTopologyLayout(topology));
    }

    public List<Pair<String, Double>> getTopNAndOtherComponentsLatency(Topology topology, int nOfTopN) throws IOException {
        TopologyMetrics topologyMetrics = getTopologyMetricsInstance(topology);
        Map<String, TopologyMetrics.ComponentMetric> metricsForTopology = topologyMetrics
            .getMetricsForTopology(getTopologyLayout(topology));

        List<Pair<String, Double>> topNAndOther = new ArrayList<>();

        List<ImmutablePair<String, Double>> latencyOrderedComponents = metricsForTopology.entrySet().stream()
                .map((x) -> new ImmutablePair<>(x.getKey(), x.getValue().getProcessedTime()))
                // reversed sort
                .sorted((c1, c2) -> {
                    if (c2.getValue() == null) {
                        // assuming c1 is bigger
                        return -1;
                    } else {
                        return c2.getValue().compareTo(c1.getValue());
                    }
                })
                .collect(toList());

        latencyOrderedComponents.stream().limit(nOfTopN).forEachOrdered(topNAndOther::add);
        double sumLatencyOthers = latencyOrderedComponents.stream()
                .skip(nOfTopN).filter((x) -> x.getValue() != null)
                .mapToDouble(Pair::getValue).sum();

        topNAndOther.add(new ImmutablePair<>("Others", sumLatencyOthers));

        return topNAndOther;
    }

    public Collection<TopologyComponentBundle.TopologyComponentType> listTopologyComponentBundleTypes() {
        return Arrays.asList(TopologyComponentBundle.TopologyComponentType.values());
    }

    public Collection<TopologyComponentBundle> listTopologyComponentBundlesForTypeWithFilter(TopologyComponentBundle.TopologyComponentType componentType,
                                                                                  List<QueryParam> params) {
        List<TopologyComponentBundle> topologyComponentBundles = new ArrayList<>();
        String ns = TopologyComponentBundle.NAME_SPACE;
        Collection<TopologyComponentBundle> filtered = dao.find(ns, params);
        for (TopologyComponentBundle tcb : filtered) {
            if (tcb.getType().equals(componentType)) {
                topologyComponentBundles.add(tcb);
            }
        }
        return topologyComponentBundles;
    }

    public TopologyComponentBundle getTopologyComponentBundle(Long topologyComponentBundleId) {
        TopologyComponentBundle topologyComponentBundle = new TopologyComponentBundle();
        topologyComponentBundle.setId(topologyComponentBundleId);
        return this.dao.get(topologyComponentBundle.getStorableKey());
    }

    public TopologyComponentBundle addTopologyComponentBundle (TopologyComponentBundle topologyComponentBundle, InputStream bundleJar) throws
            ComponentConfigException, IOException {
        topologyComponentBundle.getTopologyComponentUISpecification().validate();
        loadTransformationClassForBundle(topologyComponentBundle, bundleJar);
        if (!topologyComponentBundle.getBuiltin()) {
            topologyComponentBundle.setBundleJar(getTopologyComponentBundleJarName(topologyComponentBundle));
            uploadFileToStorage(bundleJar, topologyComponentBundle.getBundleJar());
        }
        try {
            if (topologyComponentBundle.getId() == null) {
                topologyComponentBundle.setId(this.dao.nextId(TopologyComponentBundle.NAME_SPACE));
            }
            if (topologyComponentBundle.getTimestamp() == null) {
                topologyComponentBundle.setTimestamp(System.currentTimeMillis());
            }
            this.dao.add(topologyComponentBundle);
        } catch (StorageException e) {
            if (!topologyComponentBundle.getBuiltin()) {
                LOG.debug("StorageException while adding the bundle. Deleting the bundle jar.");
                deleteFileFromStorage(topologyComponentBundle.getBundleJar());
            }
            throw e;
        }
        return topologyComponentBundle;
    }

    public TopologyComponentBundle addOrUpdateTopologyComponentBundle (Long id, TopologyComponentBundle topologyComponentBundle, InputStream bundleJar) throws
            ComponentConfigException, IOException {
        topologyComponentBundle.getTopologyComponentUISpecification().validate();
        loadTransformationClassForBundle(topologyComponentBundle, bundleJar);
        if (!topologyComponentBundle.getBuiltin()) {
            topologyComponentBundle.setBundleJar(getTopologyComponentBundleJarName(topologyComponentBundle));
            uploadFileToStorage(bundleJar, topologyComponentBundle.getBundleJar());
        }
        TopologyComponentBundle existing = new TopologyComponentBundle();
        existing.setId(id);
        existing = this.dao.get(existing.getStorableKey());
        if (!existing.getBuiltin()) {
            try {
                deleteFileFromStorage(existing.getBundleJar());
            } catch (IOException e) {
                if (!topologyComponentBundle.getBuiltin()) {
                    deleteFileFromStorage(topologyComponentBundle.getBundleJar());
                }
                throw e;
            }
        }
        try {
            topologyComponentBundle.setId(id);
            topologyComponentBundle.setTimestamp(System.currentTimeMillis());
            this.dao.addOrUpdate(topologyComponentBundle);
        } catch (StorageException e) {
            if (!topologyComponentBundle.getBuiltin()) {
                deleteFileFromStorage(topologyComponentBundle.getBundleJar());
            }
            throw e;
        }
        return topologyComponentBundle;
    }

    public TopologyComponentBundle removeTopologyComponentBundle (Long id) throws IOException {
        TopologyComponentBundle topologyComponentBundle = new TopologyComponentBundle();
        topologyComponentBundle.setId(id);
        TopologyComponentBundle existing = this.dao.get(topologyComponentBundle.getStorableKey());
        if (!existing.getBuiltin()) {
            deleteFileFromStorage(existing.getBundleJar());
        }
        return dao.remove(existing.getStorableKey());
    }

    public InputStream getFileFromJarStorage(String fileName) throws IOException {
        return this.fileStorage.downloadFile(fileName);
    }

    public Collection<CustomProcessorInfo> listCustomProcessorsFromBundleWithFilter(List<QueryParam> params) throws IOException {
        Collection<TopologyComponentBundle> customProcessors = this.listCustomProcessorBundlesWithFilter(params);
        Collection<CustomProcessorInfo> result = new ArrayList<>();
        for (TopologyComponentBundle cp : customProcessors) {
            CustomProcessorInfo customProcessorInfo = new CustomProcessorInfo();
            result.add(customProcessorInfo.fromTopologyComponentBundle(cp));
        }
        return result;
    }

    private Collection<TopologyComponentBundle> listCustomProcessorBundlesWithFilter(List<QueryParam> params) throws IOException {
        List<QueryParam> queryParamsForTopologyComponent = new ArrayList<>();
        queryParamsForTopologyComponent.add(new QueryParam(TopologyComponentBundle.SUB_TYPE, TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_SUB_TYPE));
        for (QueryParam qp : params) {
            if (qp.getName().equals(TopologyComponentBundle.STREAMING_ENGINE)) {
                queryParamsForTopologyComponent.add(qp);
            }
        }
        Collection<TopologyComponentBundle> customProcessors = this.listTopologyComponentBundlesForTypeWithFilter(TopologyComponentBundle.TopologyComponentType
                        .PROCESSOR, queryParamsForTopologyComponent);
        Collection<TopologyComponentBundle> result = new ArrayList<>();
        for (TopologyComponentBundle cp : customProcessors) {
            Map<String, Object> config = new HashMap<>();
            for (TopologyComponentUISpecification.UIField uiField: cp.getTopologyComponentUISpecification().getFields()) {
                config.put(uiField.getFieldName(), uiField.getDefaultValue());
            }
            boolean matches = true;
            for (QueryParam qp : params) {
                if (!qp.getName().equals(TopologyComponentBundle.STREAMING_ENGINE) && !qp.getValue().equals(config.get(qp.getName()))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                result.add(cp);
            }
        }
        return result;
    }

    public CustomProcessorInfo addCustomProcessorInfoAsBundle(CustomProcessorInfo customProcessorInfo, InputStream jarFile) throws IOException,
            ComponentConfigException {
        try {
            uploadFileToStorage(jarFile, customProcessorInfo.getJarFileName());
            TopologyComponentBundle topologyComponentBundle = customProcessorInfo.toTopologyComponentBundle();
            this.addTopologyComponentBundle(topologyComponentBundle, null);
        } catch (IOException e) {
            LOG.error("IOException thrown while trying to upload jar for %s", customProcessorInfo.getName());
            throw e;
        } catch (StorageException se) {
            LOG.error("StorageException thrown while adding custom processor info %s", customProcessorInfo.getName());
            try {
                deleteFileFromStorage(customProcessorInfo.getJarFileName());
            } catch (IOException e) {
                LOG.error("Unexpected exception thrown while cleaning up custom processor info %s", customProcessorInfo.getName());
                throw e;
            }
            throw se;
        }
        return customProcessorInfo;
    }

    public CustomProcessorInfo updateCustomProcessorInfoAsBundle(CustomProcessorInfo customProcessorInfo, InputStream jarFile) throws
            IOException, ComponentConfigException {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(CustomProcessorInfo.NAME, customProcessorInfo.getName()));
        Collection<TopologyComponentBundle> result = this.listCustomProcessorBundlesWithFilter(queryParams);
        if (result.isEmpty() || result.size() != 1) {
            throw new IOException("Failed to update custom processor with name:" + customProcessorInfo.getName());
        }
        TopologyComponentBundle customProcessorBundle = result.iterator().next();
        deleteFileFromStorage(new CustomProcessorInfo().fromTopologyComponentBundle(customProcessorBundle).getJarFileName());
        uploadFileToStorage(jarFile, customProcessorInfo.getJarFileName());
        TopologyComponentBundle newCustomProcessorBundle = customProcessorInfo.toTopologyComponentBundle();
        this.addOrUpdateTopologyComponentBundle(customProcessorBundle.getId(), newCustomProcessorBundle, null);
        return customProcessorInfo;
    }

    public String uploadFileToStorage(InputStream inputStream, String jarFileName) throws IOException {
        return fileStorage.uploadFile(inputStream, jarFileName);
    }

    public InputStream downloadFileFromStorage(String jarName) throws IOException {
        return fileStorage.downloadFile(jarName);
    }

    public boolean deleteFileFromStorage(String jarName) throws IOException {
        return fileStorage.deleteFile(jarName);
    }

    public CustomProcessorInfo removeCustomProcessorInfoAsBundle(String name) throws IOException {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(CustomProcessorInfo.NAME, name));
        Collection<TopologyComponentBundle> result = this.listCustomProcessorBundlesWithFilter(queryParams);
        if (result.isEmpty() || result.size() != 1) {
            throw new IOException("Failed to delete custom processor with name:" + name);
        }
        TopologyComponentBundle customProcessorBundle = result.iterator().next();
        CustomProcessorInfo customProcessorInfo = new CustomProcessorInfo();
        deleteFileFromStorage(customProcessorInfo.fromTopologyComponentBundle(customProcessorBundle).getJarFileName());
        this.removeTopologyComponentBundle(customProcessorBundle.getId());
        return customProcessorInfo;
    }

    public Collection<TopologyEditorMetadata> listTopologyEditorMetadata() {
        List<TopologyEditorMetadata> metadatas = new ArrayList<>();
        Collection<TopologyVersionInfo> currentVersions = listCurrentTopologyVersionInfos();
        for (TopologyVersionInfo version : currentVersions) {
            List<QueryParam> queryParams = WSUtils.buildTopologyIdAndVersionIdAwareQueryParams(
                    version.getTopologyId(), version.getId(), null);
            metadatas.addAll(listTopologyEditorMetadata(queryParams));
        }
        return metadatas;
    }

    public Collection<TopologyEditorMetadata> listTopologyEditorMetadata(List<QueryParam> queryParams) {
        return this.dao.find(TopologyEditorMetadata.NAME_SPACE, queryParams);
    }


    public TopologyEditorMetadata getTopologyEditorMetadata(Long topologyId) {
        return getTopologyEditorMetadata(topologyId, getCurrentVersionId(topologyId));
    }

    public TopologyEditorMetadata getTopologyEditorMetadata(Long topologyId, Long versionId) {
        TopologyEditorMetadata topologyEditorMetadata = new TopologyEditorMetadata();
        topologyEditorMetadata.setTopologyId(topologyId);
        topologyEditorMetadata.setVersionId(versionId);
        return this.dao.get(topologyEditorMetadata.getStorableKey());
    }

    public TopologyEditorMetadata addTopologyEditorMetadata(Long topologyId,
                                                            TopologyEditorMetadata topologyEditorMetadata) {
        return addTopologyEditorMetadata(topologyId, getCurrentVersionId(topologyId), topologyEditorMetadata);
    }

    public TopologyEditorMetadata addTopologyEditorMetadata(Long topologyId,
                                                            Long versionId,
                                                            TopologyEditorMetadata topologyEditorMetadata) {

        long timestamp = System.currentTimeMillis();
        topologyEditorMetadata.setTimestamp(timestamp);
        topologyEditorMetadata.setVersionId(versionId);
        this.dao.add(topologyEditorMetadata);
        updateVersionTimestamp(versionId, timestamp);
        return topologyEditorMetadata;
    }

    public TopologyEditorMetadata addOrUpdateTopologyEditorMetadata(Long topologyId, TopologyEditorMetadata topologyEditorMetadata) {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologyEditorMetadata.setTopologyId(topologyId);
        topologyEditorMetadata.setVersionId(currentTopologyVersionId);
        long timestamp = System.currentTimeMillis();
        topologyEditorMetadata.setTimestamp(timestamp);
        this.dao.addOrUpdate(topologyEditorMetadata);
        updateVersionTimestamp(currentTopologyVersionId, timestamp);
        return topologyEditorMetadata;
    }

    public TopologyEditorMetadata removeTopologyEditorMetadata(Long topologyId) {
        return removeTopologyEditorMetadata(topologyId, getCurrentVersionId(topologyId));
    }

    public TopologyEditorMetadata removeTopologyEditorMetadata(Long topologyId, Long versionId) {
        TopologyEditorMetadata topologyEditorMetadata = getTopologyEditorMetadata(topologyId, versionId);
        if (topologyEditorMetadata != null) {
            topologyEditorMetadata = dao.remove(topologyEditorMetadata.getStorableKey());
            topologyEditorMetadata.setTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologyEditorMetadata;
    }

    /**
     * returns the 'CURRENT' version of the source with given source Id
     */
    public TopologySource getTopologySource(Long topologyId, Long sourceId) {
        return getTopologySource(topologyId, sourceId, getCurrentVersionId(topologyId));
    }

    public TopologySource getTopologySource(Long topologyId, Long sourceId, Long versionId) {
        TopologySource topologySource = new TopologySource();
        topologySource.setId(sourceId);
        topologySource.setVersionId(versionId);
        TopologySource source = dao.get(new StorableKey(TOPOLOGY_SOURCE_NAMESPACE, topologySource.getPrimaryKey()));
        if (source == null || !source.getTopologyId().equals(topologyId)) {
            return null;
        }
        fillSourceStreams(source);
        return source;
    }

    /**
     * Generate id from the {@link TopologyComponent} namespace
     * so that its unique across source, sink and processors.
     * Similar to Table per concrete class hibernate strategy.
     */
    private Long getNextTopologyComponentId() {
        TopologyComponent component = new TopologyComponent();
        Long id = dao.nextId(TOPOLOGY_COMPONENT_NAMESPACE);
        component.setId(id);
        dao.add(component);
        dao.remove(component.getStorableKey());
        return id;
    }

    /*
     * Handle this check at application layer since in-memory storage
     * does not contain unique key constraint.
     *
     * Other checks can be added later.
     */
    private void validateTopology(Topology topology) {
        StorageUtils.ensureUniqueName(topology, this::listTopologies, topology.getName());
    }

    private void validateTopologySource(TopologySource topologySource) {
        StorageUtils.ensureUniqueName(topologySource, this::listTopologySources, topologySource.getName());
    }

    private void validateTopologySink(TopologySink topologySink) {
        StorageUtils.ensureUniqueName(topologySink, this::listTopologySinks, topologySink.getName());
    }

    private void validateTopologyProcessor(TopologyProcessor topologyProcessor) {
        StorageUtils.ensureUniqueName(topologyProcessor, this::listTopologyProcessors, topologyProcessor.getName());
    }

    public TopologySource addTopologySource(Long topologyId, TopologySource topologySource) {
        return addTopologySource(topologyId, getCurrentVersionId(topologyId), topologySource);
    }

    public TopologySource addTopologySource(Long topologyId,
                                            Long versionId,
                                            TopologySource topologySource) {
        if (topologySource.getId() == null) {
            topologySource.setId(getNextTopologyComponentId());
        }
        topologySource.setVersionId(versionId);
        topologySource.setTopologyId(topologyId);
        validateTopologySource(topologySource);
        List<StreamInfo> streamInfos = addTopologyOutputComponent(topologySource);
        addSourceStreamMapping(topologySource, topologySource.getOutputStreamIds());
        topologySource.setOutputStreams(streamInfos);
        topologySource.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return topologySource;
    }

    private List<StreamInfo> addTopologyOutputComponent(TopologyOutputComponent outputComponent) {
        List<StreamInfo> streamInfos;
        if (outputComponent.getOutputStreams() != null) {
            streamInfos = addOutputStreams(outputComponent.getTopologyId(), outputComponent.getVersionId(),
                    outputComponent.getOutputStreams());
            outputComponent.setOutputStreamIds(new ArrayList<>(Collections2.transform(streamInfos, new Function<StreamInfo, Long>() {
                @Override
                public Long apply(StreamInfo input) {
                    return input.getId();
                }
            })));
        } else if (outputComponent.getOutputStreamIds() != null) {
            streamInfos = getOutputStreams(outputComponent.getTopologyId(), outputComponent.getVersionId(),
                    outputComponent.getOutputStreamIds());
        } else {
            streamInfos = Collections.emptyList();
            outputComponent.setOutputStreamIds(Collections.<Long>emptyList());
        }
        dao.add(outputComponent);
        return streamInfos;
    }

    private List<StreamInfo> getOutputStreams(Long topologyId, Long versionId, List<Long> outputStreamIds) {
        List<StreamInfo> streamInfos = new ArrayList<>();
        for (Long outputStreamId : outputStreamIds) {
            StreamInfo streamInfo;
            if ((streamInfo = getStreamInfo(topologyId, outputStreamId, versionId)) == null) {
                throw new IllegalArgumentException("Output stream with id '" + outputStreamId + "' does not exist.");
            }
            streamInfos.add(streamInfo);
        }
        return streamInfos;
    }

    public TopologySource addOrUpdateTopologySource(Long topologyId, Long sourceId, TopologySource topologySource) {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologySource.setId(sourceId);
        topologySource.setVersionId(currentTopologyVersionId);
        topologySource.setTopologyId(topologyId);
        validateTopologySource(topologySource);
        dao.addOrUpdate(topologySource);
        List<Long> newList = Collections.emptyList();
        if (topologySource.getOutputStreamIds() != null) {
            newList = topologySource.getOutputStreamIds();
        } else if (topologySource.getOutputStreams() != null) {
            newList = updateOutputStreams(topologySource);
        }
        List<Long> existing = getOutputStreamIds(topologySource);
        Sets.SetView<Long> streamIdsToRemove = Sets.difference(ImmutableSet.copyOf(existing), ImmutableSet.copyOf(newList));
        Sets.SetView<Long> streamIdsToAdd = Sets.difference(ImmutableSet.copyOf(newList), ImmutableSet.copyOf(existing));
        removeSourceStreamMapping(topologySource, Lists.newArrayList(streamIdsToRemove));
        addSourceStreamMapping(topologySource, Lists.newArrayList(streamIdsToAdd));
        TopologySource updatedSource = getTopologySource(topologyId, sourceId, currentTopologyVersionId);
        updatedSource.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return updatedSource;
    }

    private List<Long> updateOutputStreams(TopologyOutputComponent outputComponent) {
        List<Long> newStreamIds = new ArrayList<>();
        for (StreamInfo streamInfo : outputComponent.getOutputStreams()) {
            if (streamInfo.getId() != null && getStreamInfo(outputComponent.getTopologyId(), streamInfo.getId()) != null) {
                addOrUpdateStreamInfo(outputComponent.getTopologyId(), streamInfo.getId(), streamInfo);
                newStreamIds.add(streamInfo.getId());
            } else {
                newStreamIds.add(addStreamInfo(outputComponent.getTopologyId(), streamInfo).getId());
            }
        }
        return newStreamIds;
    }

    public TopologySource removeTopologySource(Long topologyId, Long sourceId) {
        return removeTopologySource(topologyId, sourceId, getCurrentVersionId(topologyId));
    }

    public TopologySource removeTopologySource(Long topologyId, Long sourceId, Long versionId) {
        TopologySource topologySource = getTopologySource(topologyId, sourceId, versionId);
        if (topologySource != null) {
            topologySource = dao.<TopologySource>remove(new StorableKey(TOPOLOGY_SOURCE_NAMESPACE, topologySource.getPrimaryKey()));
            removeSourceStreamMapping(topologySource);
            topologySource.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologySource;
    }

    public Collection<TopologySource> listTopologySources() {
        return fillSourceStreams(dao.<TopologySource>list(TOPOLOGY_SOURCE_NAMESPACE));
    }

    public Collection<TopologySource> listTopologySources(List<QueryParam> params) {
        return fillSourceStreams(dao.<TopologySource>find(TOPOLOGY_SOURCE_NAMESPACE, params));
    }

    private List<StreamInfo> addOutputStreams(Long topologyId, Long versionId, List<StreamInfo> streams) {
        List<StreamInfo> streamInfos = new ArrayList<>();
        for (StreamInfo outputStream : streams) {
            streamInfos.add(addStreamInfo(topologyId, versionId, outputStream));
        }
        return streamInfos;
    }

    private void addSourceStreamMapping(TopologySource topologySource, List<Long> streamIds) {
        for (Long outputStreamId : streamIds) {
            dao.<TopologySourceStreamMapping>add(new TopologySourceStreamMapping(topologySource.getId(),
                    topologySource.getVersionId(), outputStreamId));
        }
    }

    private void removeSourceStreamMapping(TopologySource topologySource) {
        if (topologySource != null) {
            removeSourceStreamMapping(topologySource, topologySource.getOutputStreamIds());
        }
    }

    private void removeSourceStreamMapping(TopologySource topologySource, List<Long> streamIds) {
        if (topologySource != null) {
            for (Long outputStreamId : streamIds) {
                TopologySourceStreamMapping mapping = new TopologySourceStreamMapping(
                        topologySource.getId(),
                        topologySource.getVersionId(),
                        outputStreamId);
                dao.<TopologySourceStreamMapping>remove(mapping.getStorableKey());
            }
        }
    }

    private List<Long> getOutputStreamIds(TopologySource topologySource) {
        List<Long> streamIds = new ArrayList<>();
        if (topologySource != null) {
            QueryParam qp1 = new QueryParam(TopologySourceStreamMapping.FIELD_SOURCE_ID,
                    String.valueOf(topologySource.getId()));
            QueryParam qp2 = new QueryParam(TopologySourceStreamMapping.FIELD_VERSION_ID,
                    String.valueOf(topologySource.getVersionId()));
            for (TopologySourceStreamMapping mapping : listTopologySourceStreamMapping(ImmutableList.of(qp1, qp2))) {
                streamIds.add(mapping.getStreamId());
            }
        }
        return streamIds;
    }

    private Collection<TopologySourceStreamMapping> listTopologySourceStreamMapping(List<QueryParam> params) {
        try {
            return dao.find(TOPOLOGY_SOURCE_STREAM_MAPPING_NAMESPACE, params);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void fillProcessorStreams(TopologyProcessor processor) {
        if (processor != null) {
            fillProcessorStreams(Collections.singletonList(processor));
        }
    }

    private Collection<TopologyProcessor> fillProcessorStreams(Collection<TopologyProcessor> processors) {
        if (processors != null) {
            for (TopologyProcessor processor : processors) {
                List<StreamInfo> streamInfos = getOutputStreams(processor);
                processor.setOutputStreams(streamInfos);
                processor.setOutputStreamIds(new ArrayList<>(Collections2.transform(streamInfos, new Function<StreamInfo, Long>() {
                    @Nullable
                    @Override
                    public Long apply(@Nullable StreamInfo input) {
                        return input.getId();
                    }
                })));
            }
        }
        return processors;
    }

    private List<StreamInfo> getOutputStreams(TopologyProcessor topologyProcessor) {
        List<StreamInfo> streams = new ArrayList<>();
        if (topologyProcessor != null) {
            QueryParam qp1 = new QueryParam(TopologyProcessorStreamMapping.FIELD_PROCESSOR_ID,
                    String.valueOf(topologyProcessor.getId()));
            QueryParam qp2 = new QueryParam(TopologyProcessorStreamMapping.FIELD_VERSION_ID,
                    String.valueOf(topologyProcessor.getVersionId()));
            for (TopologyProcessorStreamMapping mapping : listTopologyProcessorStreamMapping(ImmutableList.of(qp1, qp2))) {
                StreamInfo streamInfo = getStreamInfo(topologyProcessor.getTopologyId(), mapping.getStreamId(), topologyProcessor.getVersionId());
                if (streamInfo != null) {
                    streams.add(streamInfo);
                }
            }
        }
        return streams;
    }

    private void fillSourceStreams(TopologySource source) {
        if (source != null) {
            fillSourceStreams(Collections.singletonList(source));
        }
    }

    private Collection<TopologySource> fillSourceStreams(Collection<TopologySource> sources) {
        if (sources != null) {
            for (TopologySource source : sources) {
                List<StreamInfo> streamInfos = getOutputStreams(source);
                source.setOutputStreams(streamInfos);
                source.setOutputStreamIds(new ArrayList<>(Collections2.transform(streamInfos, new Function<StreamInfo, Long>() {
                    @Nullable
                    @Override
                    public Long apply(@Nullable StreamInfo input) {
                        return input.getId();
                    }
                })));
            }
        }
        return sources;
    }

    private List<StreamInfo> getOutputStreams(TopologySource topologySource) {
        List<StreamInfo> streams = new ArrayList<>();
        if (topologySource != null) {
            QueryParam qp1 = new QueryParam(TopologySourceStreamMapping.FIELD_SOURCE_ID,
                    String.valueOf(topologySource.getId()));
            QueryParam qp2 = new QueryParam(TopologySourceStreamMapping.FIELD_VERSION_ID,
                    String.valueOf(topologySource.getVersionId()));
            for (TopologySourceStreamMapping mapping : listTopologySourceStreamMapping(ImmutableList.of(qp1, qp2))) {
                StreamInfo streamInfo = getStreamInfo(topologySource.getTopologyId(), mapping.getStreamId(), topologySource.getVersionId());
                if (streamInfo != null) {
                    streams.add(streamInfo);
                }
            }
        }
        return streams;
    }

    public TopologySink getTopologySink(Long topologyId, Long sinkId) {
        return getTopologySink(topologyId, sinkId, getCurrentVersionId(topologyId));
    }

    public TopologySink getTopologySink(Long topologyId, Long sinkId, Long versionId) {
        TopologySink topologySink = new TopologySink();
        topologySink.setId(sinkId);
        topologySink.setVersionId(versionId);
        TopologySink sink = dao.get(new StorableKey(TOPOLOGY_SINK_NAMESPACE, topologySink.getPrimaryKey()));
        if (sink == null || !sink.getTopologyId().equals(topologyId)) {
            return null;
        }
        sink.setVersionTimestamp(getVersionTimestamp(versionId));
        return sink;
    }

    public TopologySink addTopologySink(Long topologyId, TopologySink topologySink) {
        return addTopologySink(topologyId, getCurrentVersionId(topologyId), topologySink);
    }

    public TopologySink addTopologySink(Long topologyId,
                                        Long versionId,
                                        TopologySink topologySink) {
        if (topologySink.getId() == null) {
            topologySink.setId(getNextTopologyComponentId());
        }
        topologySink.setVersionId(versionId);
        topologySink.setTopologyId(topologyId);
        validateTopologySink(topologySink);
        dao.add(topologySink);
        topologySink.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return topologySink;
    }

    public TopologySink addOrUpdateTopologySink(Long topologyId, Long id, TopologySink topologySink) {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologySink.setId(id);
        topologySink.setVersionId(currentTopologyVersionId);
        topologySink.setTopologyId(topologyId);
        validateTopologySink(topologySink);
        dao.addOrUpdate(topologySink);
        topologySink.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return topologySink;
    }

    public TopologySink removeTopologySink(Long id) {
        TopologySink topologySink = new TopologySink();
        topologySink.setId(id);
        return dao.remove(new StorableKey(TOPOLOGY_SINK_NAMESPACE, topologySink.getPrimaryKey()));
    }

    public TopologySink removeTopologySink(Long topologyId, Long sinkId) {
        return removeTopologySink(topologyId, sinkId, getCurrentVersionId(topologyId));
    }

    public TopologySink removeTopologySink(Long topologyId, Long sinkId, Long versionId) {
        TopologySink topologySink = getTopologySink(topologyId, sinkId, versionId);
        if (topologySink != null) {
            topologySink = dao.<TopologySink>remove(new StorableKey(TOPOLOGY_SINK_NAMESPACE, topologySink.getPrimaryKey()));
            topologySink.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologySink;
    }

    public Collection<TopologySink> listTopologySinks() {
        return dao.list(TOPOLOGY_SINK_NAMESPACE);
    }

    public Collection<TopologySink> listTopologySinks(List<QueryParam> params) {
        return dao.find(TOPOLOGY_SINK_NAMESPACE, params);
    }

    public TopologyProcessor getTopologyProcessor(Long topologyId, Long processorId) {
        return getTopologyProcessor(topologyId, processorId, getCurrentVersionId(topologyId));
    }

    public TopologyProcessor getTopologyProcessor(Long topologyId, Long processorId, Long versionId) {
        TopologyProcessor topologyProcessor = new TopologyProcessor();
        topologyProcessor.setId(processorId);
        topologyProcessor.setVersionId(versionId);
        TopologyProcessor processor = dao.get(new StorableKey(TOPOLOGY_PROCESSOR_NAMESPACE, topologyProcessor.getPrimaryKey()));
        if (processor == null || !processor.getTopologyId().equals(topologyId)) {
            return null;
        }
        fillProcessorStreams(processor);
        processor.setVersionTimestamp(getVersionTimestamp(versionId));
        return processor;
    }

    public TopologyProcessor addTopologyProcessor(Long topologyId, TopologyProcessor topologyProcessor) {
        return addTopologyProcessor(topologyId, getCurrentVersionId(topologyId), topologyProcessor);
    }

    public TopologyProcessor addTopologyProcessor(Long topologyId,
                                                  Long versionId,
                                                  TopologyProcessor topologyProcessor) {
        if (topologyProcessor.getId() == null) {
            topologyProcessor.setId(getNextTopologyComponentId());
        }
        topologyProcessor.setVersionId(versionId);
        topologyProcessor.setTopologyId(topologyId);
        validateTopologyProcessor(topologyProcessor);
        List<StreamInfo> streamInfos = addTopologyOutputComponent(topologyProcessor);
        addProcessorStreamMapping(topologyProcessor, topologyProcessor.getOutputStreamIds());
        topologyProcessor.setOutputStreams(streamInfos);
        topologyProcessor.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return topologyProcessor;
    }

    public TopologyProcessor addOrUpdateTopologyProcessor(Long topologyId, Long id, TopologyProcessor topologyProcessor) {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologyProcessor.setId(id);
        topologyProcessor.setVersionId(currentTopologyVersionId);
        topologyProcessor.setTopologyId(topologyId);
        validateTopologyProcessor(topologyProcessor);
        dao.addOrUpdate(topologyProcessor);
        List<Long> newList = Collections.emptyList();
        if (topologyProcessor.getOutputStreamIds() != null) {
            newList = topologyProcessor.getOutputStreamIds();
        } else if (topologyProcessor.getOutputStreams() != null) {
            newList = updateOutputStreams(topologyProcessor);
        }
        List<Long> existing = getOutputStreamIds(topologyProcessor);
        Sets.SetView<Long> streamIdsToRemove = Sets.difference(ImmutableSet.copyOf(existing), ImmutableSet.copyOf(newList));
        Sets.SetView<Long> streamIdsToAdd = Sets.difference(ImmutableSet.copyOf(newList), ImmutableSet.copyOf(existing));
        removeProcessorStreamMapping(topologyProcessor, Lists.newArrayList(streamIdsToRemove));
        addProcessorStreamMapping(topologyProcessor, Lists.newArrayList(streamIdsToAdd));
        TopologyProcessor updatedProcessor = getTopologyProcessor(topologyId, id, currentTopologyVersionId);
        updatedProcessor.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return topologyProcessor;
    }

    public TopologyProcessor removeTopologyProcessor(Long topologyId, Long processorId) {
        return removeTopologyProcessor(topologyId, processorId, getCurrentVersionId(topologyId));
    }

    public TopologyProcessor removeTopologyProcessor(Long topologyId, Long processorId, Long versionId) {
        TopologyProcessor topologyProcessor = getTopologyProcessor(topologyId, processorId, versionId);
        if (topologyProcessor != null) {
            topologyProcessor = dao.<TopologyProcessor>remove(new StorableKey(TOPOLOGY_PROCESSOR_NAMESPACE, topologyProcessor.getPrimaryKey()));
            removeProcessorStreamMapping(topologyProcessor);
            topologyProcessor.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologyProcessor;
    }

    public Collection<TopologyProcessor> listTopologyProcessors() {
        return fillProcessorStreams(dao.<TopologyProcessor>list(TOPOLOGY_PROCESSOR_NAMESPACE));
    }

    public Collection<TopologyProcessor> listTopologyProcessors(List<QueryParam> params) {
        return fillProcessorStreams(dao.<TopologyProcessor>find(TOPOLOGY_PROCESSOR_NAMESPACE, params));
    }

    private void createProcessorStreamMapping(TopologyProcessor topologyProcessor, List<StreamInfo> streams) {
        for (StreamInfo outputStream : streams) {
            StreamInfo addedStream = addStreamInfo(topologyProcessor.getTopologyId(), outputStream);
            dao.<TopologyProcessorStreamMapping>add(new TopologyProcessorStreamMapping(topologyProcessor.getId(),
                    topologyProcessor.getVersionId(),
                    addedStream.getId()));
        }
    }

    private void addProcessorStreamMapping(TopologyProcessor topologyProcessor, List<Long> streamIds) {
        for (Long outputStreamId : streamIds) {
            dao.<TopologyProcessorStreamMapping>add(new TopologyProcessorStreamMapping(topologyProcessor.getId(),
                    topologyProcessor.getVersionId(),
                    outputStreamId));
        }
    }

    private void removeProcessorStreamMapping(TopologyProcessor topologyProcessor) {
        if (topologyProcessor != null) {
            removeProcessorStreamMapping(topologyProcessor, topologyProcessor.getOutputStreamIds());
        }
    }

    private void removeProcessorStreamMapping(TopologyProcessor topologyProcessor, List<Long> streamIds) {
        if (topologyProcessor != null) {
            for (Long outputStreamId : streamIds) {
                TopologyProcessorStreamMapping mapping = new TopologyProcessorStreamMapping(topologyProcessor.getId(),
                        topologyProcessor.getVersionId(),
                        outputStreamId);
                dao.<TopologyProcessorStreamMapping>remove(mapping.getStorableKey());
            }
        }
    }

    private List<Long> getOutputStreamIds(TopologyProcessor topologyProcessor) {
        List<Long> streamIds = new ArrayList<>();
        if (topologyProcessor != null) {
            QueryParam qp1 = new QueryParam(TopologyProcessorStreamMapping.FIELD_PROCESSOR_ID,
                    String.valueOf(topologyProcessor.getId()));
            QueryParam qp2 = new QueryParam(TopologyProcessorStreamMapping.FIELD_VERSION_ID,
                    String.valueOf(topologyProcessor.getVersionId()));
            for (TopologyProcessorStreamMapping mapping : listTopologyProcessorStreamMapping(ImmutableList.of(qp1, qp2))) {
                streamIds.add(mapping.getStreamId());
            }
        }
        return streamIds;
    }

    private Collection<TopologyProcessorStreamMapping> listTopologyProcessorStreamMapping(List<QueryParam> params) {
        try {
            return dao.find(TOPOLOGY_PROCESSOR_STREAM_MAPPING_NAMESPACE, params);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public TopologyEdge getTopologyEdge(Long topologyId, Long edgeId) {
        return getTopologyEdge(topologyId, edgeId, getCurrentVersionId(topologyId));
    }

    public TopologyEdge getTopologyEdge(Long topologyId, Long edgeId, Long versionId) {
        TopologyEdge topologyEdge = new TopologyEdge();
        topologyEdge.setId(edgeId);
        topologyEdge.setVersionId(versionId);
        TopologyEdge edge = dao.get(new StorableKey(TOPOLOGY_EDGE_NAMESPACE, topologyEdge.getPrimaryKey()));
        if (edge == null || !edge.getTopologyId().equals(topologyId)) {
            return null;
        }
        edge.setVersionTimestamp(getVersionTimestamp(versionId));
        return edge;
    }

    public TopologyEdge addTopologyEdge(Long topologyId, TopologyEdge topologyEdge) {
        return addTopologyEdge(topologyId, getCurrentVersionId(topologyId), topologyEdge);
    }

    public TopologyEdge addTopologyEdge(Long topologyId,
                                        Long versionId,
                                        TopologyEdge topologyEdge) {
        if (topologyEdge.getId() == null) {
            topologyEdge.setId(dao.nextId(TOPOLOGY_EDGE_NAMESPACE));
        }
        topologyEdge.setVersionId(versionId);
        topologyEdge.setTopologyId(topologyId);
        validateEdge(topologyEdge);
        checkDuplicateEdge(topologyEdge);
        dao.add(topologyEdge);
        topologyEdge.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return topologyEdge;
    }

    // validate from, to and stream ids of the edge
    private void validateEdge(TopologyEdge edge) {
        TopologySource source = getTopologySource(edge.getTopologyId(), edge.getFromId(), edge.getVersionId());
        TopologyProcessor processor = getTopologyProcessor(edge.getTopologyId(), edge.getFromId(), edge.getVersionId());
        if ((source == null || !source.getTopologyId().equals(edge.getTopologyId()))
                && (processor == null || !processor.getTopologyId().equals(edge.getTopologyId()))) {
            throw new IllegalArgumentException("Invalid source for edge " + edge);
        }
        TopologyOutputComponent outputComponent = source != null ? source : processor;
        processor = getTopologyProcessor(edge.getTopologyId(), edge.getToId(), edge.getVersionId());
        TopologySink sink = getTopologySink(edge.getTopologyId(), edge.getToId(), edge.getVersionId());
        if ((processor == null || !processor.getTopologyId().equals(edge.getTopologyId()))
                && (sink == null || !sink.getTopologyId().equals(edge.getTopologyId()))) {
            throw new IllegalArgumentException("Invalid destination for edge " + edge);
        }

        Set<Long> outputStreamIds = new HashSet<>();
        if (outputComponent.getOutputStreamIds() != null) {
            outputStreamIds.addAll(outputComponent.getOutputStreamIds());
        } else if (outputComponent.getOutputStreams() != null) {
            outputStreamIds.addAll(Collections2.transform(outputComponent.getOutputStreams(), new Function<StreamInfo, Long>() {
                @Override
                public Long apply(StreamInfo input) {
                    return input.getId();
                }
            }));
        }

        Collection<Long> edgeStreamIds = Collections2.transform(edge.getStreamGroupings(),
                                                                new Function<StreamGrouping, Long>() {
                                                                    public Long apply(StreamGrouping streamGrouping) {
                                                                        return streamGrouping.getStreamId();
                                                                    }
                                                                });
        if (!outputStreamIds.containsAll(edgeStreamIds)) {
            throw new IllegalArgumentException("Edge stream Ids " + edgeStreamIds +
                                                       " must be a subset of outputStreamIds " + outputStreamIds);
        }
        // check the fields specified in the fields grouping is a subset of the stream fields
        for (StreamGrouping streamGrouping : edge.getStreamGroupings()) {
            List<String> fields;
            if ((fields = streamGrouping.getFields()) != null) {
                Set<String> streamFields = new HashSet<>(
                        Collections2.transform(getStreamInfo(edge.getTopologyId(),
                                streamGrouping.getStreamId(),
                                edge.getVersionId())
                                .getFields(),
                                new Function<Schema.Field, String>() {
                                    public String apply(Schema.Field field) {
                                        return field.getName();
                                    }
                                }));
                if (!streamFields.containsAll(fields)) {
                    throw new IllegalArgumentException("Fields in the grouping " + fields +
                                                               " must be a subset the stream fields " + streamFields);
                }
            }
        }
    }

    // check if edge already exists for given topology between same source and dest
    private void checkDuplicateEdge(TopologyEdge edge) {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(TopologyEdge.TOPOLOGYID, edge.getTopologyId().toString()));
        queryParams.add(new QueryParam(TopologyEdge.VERSIONID, edge.getVersionId().toString()));
        queryParams.add(new QueryParam(TopologyEdge.FROMID, edge.getFromId().toString()));
        queryParams.add(new QueryParam(TopologyEdge.TOID, edge.getToId().toString()));

        try {
            Collection<TopologyEdge> existingEdges = listTopologyEdges(queryParams);
            if (existingEdges != null && !existingEdges.isEmpty()) {
                throw new IllegalArgumentException("Edge already exists between source and destination, use update api");
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public TopologyEdge addOrUpdateTopologyEdge(Long topologyId, Long id, TopologyEdge topologyEdge) {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologyEdge.setId(id);
        topologyEdge.setVersionId(currentTopologyVersionId);
        topologyEdge.setTopologyId(topologyId);
        validateEdge(topologyEdge);
        dao.addOrUpdate(topologyEdge);
        topologyEdge.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return topologyEdge;
    }

    public TopologyEdge removeTopologyEdge(Long topologyId, Long edgeId) {
        return removeTopologyEdge(topologyId, edgeId, getCurrentVersionId(topologyId));
    }

    public TopologyEdge removeTopologyEdge(Long topologyId, Long edgeId, Long versionId) {
        TopologyEdge topologyEdge = getTopologyEdge(topologyId, edgeId, versionId);
        if (topologyEdge != null) {
            topologyEdge = dao.remove(new StorableKey(TOPOLOGY_EDGE_NAMESPACE, topologyEdge.getPrimaryKey()));
            topologyEdge.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologyEdge;
    }

    public Collection<TopologyEdge> listTopologyEdges() {
        return dao.list(TOPOLOGY_EDGE_NAMESPACE);
    }

    public Collection<TopologyEdge> listTopologyEdges(List<QueryParam> params) throws Exception {
        return dao.find(TOPOLOGY_EDGE_NAMESPACE, params);
    }

    public StreamInfo getStreamInfo(Long topologyId, Long streamId) {
        return getStreamInfo(topologyId, streamId, getCurrentVersionId(topologyId));
    }

    public StreamInfo getStreamInfo(Long topologyId, Long streamId, Long versionId) {
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.setId(streamId);
        streamInfo.setVersionId(versionId);
        StreamInfo result = dao.get(new StorableKey(STREAMINFO_NAMESPACE, streamInfo.getPrimaryKey()));
        if (result == null || !result.getTopologyId().equals(topologyId)) {
            return null;
        }
        result.setVersionTimestamp(getVersionTimestamp(versionId));
        return result;
    }

    public StreamInfo getStreamInfoByName(Long topologyId, String streamId) {
        return getStreamInfoByName(topologyId, streamId, getCurrentVersionId(topologyId));
    }
    public StreamInfo getStreamInfoByName(Long topologyId,
                                          String streamId,
                                          Long versionId) {
        List<QueryParam> queryParams = WSUtils.buildTopologyIdAndVersionIdAwareQueryParams(topologyId, versionId, null);
        try {
            for (StreamInfo streamInfo : listStreamInfos(queryParams)) {
                if (streamInfo.getStreamId().equals(streamId)) {
                    return streamInfo;
                }
            }
        } catch (Exception ex) {
            LOG.error("Got exception ", ex);
            throw new RuntimeException(ex);
        }
      return null;
    }

    public StreamInfo addStreamInfo(Long topologyId, StreamInfo streamInfo) {
        return addStreamInfo(topologyId, getCurrentVersionId(topologyId), streamInfo);
    }

    public StreamInfo addStreamInfo(Long topologyId,
                                    Long versionId,
                                    StreamInfo streamInfo) {
        if (streamInfo.getId() == null) {
            streamInfo.setId(dao.nextId(STREAMINFO_NAMESPACE));
        }
        long timestamp = System.currentTimeMillis();
        streamInfo.setVersionTimestamp(timestamp);
        streamInfo.setVersionId(versionId);
        streamInfo.setTopologyId(topologyId);
        dao.add(streamInfo);
        updateVersionTimestamp(versionId, timestamp);
        return streamInfo;
    }

    public StreamInfo addOrUpdateStreamInfo(Long topologyId, Long id, StreamInfo stream) {
        stream.setId(id);
        Long currentVersionId = getCurrentVersionId(topologyId);
        stream.setVersionId(currentVersionId);
        stream.setTopologyId(topologyId);
        long timestamp = System.currentTimeMillis();
        stream.setVersionTimestamp(timestamp);
        dao.addOrUpdate(stream);
        updateVersionTimestamp(currentVersionId, timestamp);
        return stream;
    }

    public StreamInfo removeStreamInfo(Long topologyId, Long streamId) {
        return removeStreamInfo(topologyId, streamId, getCurrentVersionId(topologyId));
    }

    public StreamInfo removeStreamInfo(Long topologyId, Long streamId, Long versionId) {
        StreamInfo streamInfo = getStreamInfo(topologyId, streamId, versionId);
        if (streamInfo != null) {
            streamInfo = dao.remove(new StorableKey(STREAMINFO_NAMESPACE, streamInfo.getPrimaryKey()));
            streamInfo.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return streamInfo;
    }

    public Collection<StreamInfo> listStreamInfos() {
        return dao.list(STREAMINFO_NAMESPACE);
    }

    public Collection<StreamInfo> listStreamInfos(List<QueryParam> params) throws Exception {
        return dao.find(STREAMINFO_NAMESPACE, params);
    }

    public Collection<RuleInfo> listRules() {
        return dao.list(TOPOLOGY_RULEINFO_NAMESPACE);
    }

    public Collection<RuleInfo> listRules(List<QueryParam> params) throws Exception {
        return dao.find(TOPOLOGY_RULEINFO_NAMESPACE, params);
    }

    public RuleInfo addRule(Long topologyId, RuleInfo ruleInfo) throws Exception {
        return addRule(topologyId, getCurrentVersionId(topologyId), ruleInfo);
    }

    public RuleInfo addRule(Long topologyId,
                            Long versionId,
                            RuleInfo ruleInfo) throws Exception {
        if (ruleInfo.getId() == null) {
            ruleInfo.setId(dao.nextId(TOPOLOGY_RULEINFO_NAMESPACE));
        }
        ruleInfo.setVersionId(versionId);
        ruleInfo.setTopologyId(topologyId);
        String parsedRuleStr = parseAndSerialize(ruleInfo);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        ruleInfo.setParsedRuleStr(parsedRuleStr);
        dao.add(ruleInfo);
        ruleInfo.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return ruleInfo;
    }

    public RuleInfo getRule(Long topologyId, Long ruleId) throws Exception {
        return getRule(topologyId, ruleId, getCurrentVersionId(topologyId));
    }

    public RuleInfo getRule(Long topologyId, Long ruleId, Long versionId) throws Exception {
        RuleInfo topologyRuleInfo = new RuleInfo();
        topologyRuleInfo.setId(ruleId);
        topologyRuleInfo.setVersionId(versionId);
        RuleInfo ruleInfo = dao.get(new StorableKey(TOPOLOGY_RULEINFO_NAMESPACE, topologyRuleInfo.getPrimaryKey()));
        if (ruleInfo == null || !ruleInfo.getTopologyId().equals(topologyId)) {
            return null;
        }
        ruleInfo.setVersionTimestamp(getVersionTimestamp(versionId));
        return ruleInfo;
    }


    public RuleInfo addOrUpdateRule(Long topologyId, Long ruleId, RuleInfo ruleInfo) throws Exception {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        ruleInfo.setId(ruleId);
        ruleInfo.setVersionId(currentTopologyVersionId);
        ruleInfo.setTopologyId(topologyId);
        String parsedRuleStr = parseAndSerialize(ruleInfo);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        ruleInfo.setParsedRuleStr(parsedRuleStr);
        dao.addOrUpdate(ruleInfo);
        ruleInfo.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return ruleInfo;
    }

    public RuleInfo removeRule(Long topologyId, Long ruleId) throws Exception {
        return removeRule(topologyId, ruleId, getCurrentVersionId(topologyId));
    }

    public RuleInfo removeRule(Long topologyId, Long ruleId, Long versionId) throws Exception {
        RuleInfo ruleInfo = getRule(topologyId, ruleId, versionId);
        if (ruleInfo != null) {
            ruleInfo = dao.remove(new StorableKey(TOPOLOGY_RULEINFO_NAMESPACE, ruleInfo.getPrimaryKey()));
            ruleInfo.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return ruleInfo;
    }

    public Collection<WindowInfo> listWindows() {
        return dao.list(TOPOLOGY_WINDOWINFO_NAMESPACE);
    }

    public Collection<WindowInfo> listWindows(List<QueryParam> params) throws Exception {
        return dao.find(TOPOLOGY_WINDOWINFO_NAMESPACE, params);
    }

    public Collection<BranchRuleInfo> listBranchRules() {
        return dao.list(TOPOLOGY_BRANCHRULEINFO_NAMESPACE);
    }

    public Collection<BranchRuleInfo> listBranchRules(List<QueryParam> params) throws Exception {
        return dao.find(TOPOLOGY_BRANCHRULEINFO_NAMESPACE, params);
    }

    public BranchRuleInfo addBranchRule(Long topologyId, BranchRuleInfo branchRuleInfo) throws Exception {
        return addBranchRule(topologyId, getCurrentVersionId(topologyId), branchRuleInfo);
    }
    public BranchRuleInfo addBranchRule(Long topologyId,
                                        Long versionId,
                                        BranchRuleInfo branchRuleInfo) throws Exception {
        if (branchRuleInfo.getId() == null) {
            branchRuleInfo.setId(dao.nextId(TOPOLOGY_BRANCHRULEINFO_NAMESPACE));
        }
        branchRuleInfo.setTopologyId(topologyId);
        branchRuleInfo.setVersionId(versionId);
        String parsedRuleStr = parseAndSerialize(branchRuleInfo);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        branchRuleInfo.setParsedRuleStr(parsedRuleStr);
        dao.add(branchRuleInfo);
        branchRuleInfo.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return branchRuleInfo;
    }

    public BranchRuleInfo getBranchRule(Long topologyId, Long ruleId) throws Exception {
        return getBranchRule(topologyId, ruleId, getCurrentVersionId(topologyId));
    }

    public BranchRuleInfo getBranchRule(Long topologyId, Long ruleId, Long versionId) throws Exception {
        BranchRuleInfo branchRuleInfo = new BranchRuleInfo();
        branchRuleInfo.setId(ruleId);
        branchRuleInfo.setVersionId(versionId);
        branchRuleInfo = dao.get(new StorableKey(TOPOLOGY_BRANCHRULEINFO_NAMESPACE, branchRuleInfo.getPrimaryKey()));
        if (branchRuleInfo == null || !branchRuleInfo.getTopologyId().equals(topologyId)) {
            return null;
        }
        branchRuleInfo.setVersionTimestamp(getVersionTimestamp(versionId));
        return branchRuleInfo;
    }

    public BranchRuleInfo addOrUpdateBranchRule(Long topologyId, Long ruleId, BranchRuleInfo branchRuleInfo) throws Exception {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        branchRuleInfo.setId(ruleId);
        branchRuleInfo.setVersionId(currentTopologyVersionId);
        branchRuleInfo.setTopologyId(topologyId);
        String parsedRuleStr = parseAndSerialize(branchRuleInfo);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        branchRuleInfo.setParsedRuleStr(parsedRuleStr);
        dao.addOrUpdate(branchRuleInfo);
        branchRuleInfo.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return branchRuleInfo;
    }

    public BranchRuleInfo removeBranchRule(Long topologyId, Long id) throws Exception {
        return removeBranchRule(topologyId, id, getCurrentVersionId(topologyId));
    }

    public BranchRuleInfo removeBranchRule(Long topologyId, Long id, Long versionId) throws Exception {
        BranchRuleInfo branchRuleInfo = getBranchRule(topologyId, id, versionId);
        if (branchRuleInfo != null) {
            branchRuleInfo = dao.remove(new StorableKey(TOPOLOGY_BRANCHRULEINFO_NAMESPACE, branchRuleInfo.getPrimaryKey()));
            branchRuleInfo.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return branchRuleInfo;
    }

    public WindowInfo addWindow(Long topologyId, WindowInfo windowInfo) throws Exception {
        return addWindow(topologyId, getCurrentVersionId(topologyId), windowInfo);
    }

    public WindowInfo addWindow(Long topologyId,
                                Long versionId,
                                WindowInfo windowInfo) throws Exception {
        if (windowInfo.getId() == null) {
            windowInfo.setId(dao.nextId(TOPOLOGY_WINDOWINFO_NAMESPACE));
        }
        windowInfo.setTopologyId(topologyId);
        windowInfo.setVersionId(versionId);
        String parsedRuleStr = parseAndSerialize(windowInfo);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        windowInfo.setParsedRuleStr(parsedRuleStr);
        dao.add(windowInfo);
        windowInfo.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return windowInfo;
    }

    public WindowInfo getWindow(Long topologyId, Long windowId) throws Exception {
        return getWindow(topologyId, windowId, getCurrentVersionId(topologyId));
    }

    public WindowInfo getWindow(Long topologyId, Long windowId, Long versionId) throws Exception {
        WindowInfo topologyWindowInfo = new WindowInfo();
        topologyWindowInfo.setId(windowId);
        topologyWindowInfo.setVersionId(versionId);
        WindowInfo windowInfo = dao.get(new StorableKey(TOPOLOGY_WINDOWINFO_NAMESPACE, topologyWindowInfo.getPrimaryKey()));
        if (windowInfo == null || !windowInfo.getTopologyId().equals(topologyId)) {
            return null;
        }
        windowInfo.setVersionTimestamp(getVersionTimestamp(versionId));
        return windowInfo;
    }

    public WindowInfo addOrUpdateWindow(Long topologyId, Long windowId, WindowInfo windowInfo) throws Exception {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        windowInfo.setId(windowId);
        windowInfo.setVersionId(currentTopologyVersionId);
        windowInfo.setTopologyId(topologyId);
        String parsedRuleStr = parseAndSerialize(windowInfo);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        windowInfo.setParsedRuleStr(parsedRuleStr);
        dao.addOrUpdate(windowInfo);
        windowInfo.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return windowInfo;
    }

    public WindowInfo removeWindow(Long topologyId, Long windowId) throws Exception {
        return removeWindow(topologyId, windowId, getCurrentVersionId(topologyId));
    }

    public WindowInfo removeWindow(Long topologyId, Long windowId, Long versionId) throws Exception {
        WindowInfo windowInfo = getWindow(topologyId, windowId, versionId);
        if (windowInfo != null) {
            windowInfo = dao.remove(new StorableKey(TOPOLOGY_WINDOWINFO_NAMESPACE, windowInfo.getPrimaryKey()));
            windowInfo.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return windowInfo;
    }

    private String parseAndSerialize(RuleInfo ruleInfo) throws JsonProcessingException {
        Rule rule = new Rule();
        rule.setId(ruleInfo.getId());
        rule.setName(ruleInfo.getName());
        rule.setDescription(ruleInfo.getDescription());
        rule.setWindow(ruleInfo.getWindow());
        rule.setActions(ruleInfo.getActions());

        if (ruleInfo.getStreams() != null && !ruleInfo.getStreams().isEmpty()) {
            ruleInfo.setSql(getSqlString(ruleInfo.getStreams(), ruleInfo.getProjections(), ruleInfo.getCondition(), null));
        } else if (StringUtils.isEmpty(ruleInfo.getSql())) {
            throw new IllegalArgumentException("Either streams or sql string should be specified.");
        }
        updateRuleWithSql(rule, ruleInfo.getSql(), ruleInfo.getTopologyId(), ruleInfo.getVersionId());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(rule);
    }

    private String parseAndSerialize(BranchRuleInfo ruleInfo) throws JsonProcessingException {
        Rule rule = new Rule();
        rule.setId(ruleInfo.getId());
        rule.setName(ruleInfo.getName());
        rule.setDescription(ruleInfo.getDescription());
        rule.setActions(ruleInfo.getActions());
        String sql = getSqlString(Arrays.asList(ruleInfo.getStream()), null, ruleInfo.getCondition(), null);
        updateRuleWithSql(rule, sql, ruleInfo.getTopologyId(), ruleInfo.getVersionId());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(rule);
    }

    private String parseAndSerialize(WindowInfo windowInfo) throws JsonProcessingException {
        if (windowInfo.getStreams() == null || windowInfo.getStreams().isEmpty()) {
            LOG.error("Streams should be specified.");
            return StringUtils.EMPTY;
        }
        Rule rule = new Rule();
        rule.setId(windowInfo.getId());
        rule.setName(windowInfo.getName());
        rule.setDescription(windowInfo.getDescription());
        rule.setWindow(windowInfo.getWindow());
        rule.setActions(windowInfo.getActions());
        String sql = getSqlString(windowInfo.getStreams(),
                windowInfo.getProjections(),
                windowInfo.getCondition(),
                windowInfo.getGroupbykeys());
        updateRuleWithSql(rule, sql, windowInfo.getTopologyId(), windowInfo.getVersionId());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(rule);
    }

    private String getSqlString(List<String> streams,
                                List<Projection> projections,
                                String condition,
                                List<String> groupByKeys) {
        String SQL = select(projections).orElse("SELECT * ");
        SQL += join(" FROM ", getTable(streams)).get();
        SQL += join(" WHERE ", condition).orElse("");
        SQL += join(" GROUP BY ", groupByKeys).orElse("");
        return SQL;
    }

    private Optional<String> select(List<Projection> projections) {
        if (projections != null) {
            return join("SELECT ", Collections2.transform(projections, new Function<Projection, String>() {
                @Override
                public String apply(Projection input) {
                    return input.toString();
                }
            }));
        }
        return Optional.empty();
    }

    private Optional<String> join(String keyword, String part) {
        if (part != null) {
            return join(keyword, Collections.singletonList(part));
        }
        return Optional.empty();
    }

    private Optional<String> join(String keyword, Collection<String> parts) {
        if (parts != null && !parts.isEmpty()) {
            return Optional.of(keyword + Joiner.on(",").join(parts));
        }
        return Optional.empty();
    }

    private String getTable(List<String> streams) {
        if (streams == null || streams.isEmpty()) {
            LOG.warn("Rule condition is specified without input stream, will use default stream");
            return StreamlineEvent.DEFAULT_SOURCE_STREAM;
        } else if (streams.size() == 1) {
            return streams.iterator().next();
        } else {
            throw new UnsupportedOperationException("Joining multiple streams");
        }
    }

    private void updateRuleWithSql(Rule rule, String sql, Long topologyId, Long versionId) {
        // parse
        RuleParser ruleParser = new RuleParser(this, sql, topologyId, versionId);
        ruleParser.parse();

        // update rule with parsed sql constructs
        rule.setStreams(new HashSet<>(Collections2.transform(ruleParser.getStreams(), new Function<Stream, String>() {
            @Override
            public String apply(Stream input) {
                return input.getId();
            }
        })));
        rule.setProjection(ruleParser.getProjection());
        rule.setCondition(ruleParser.getCondition());
        rule.setGroupBy(ruleParser.getGroupBy());
        rule.setHaving(ruleParser.getHaving());
        rule.setReferredUdfs(ruleParser.getReferredUdfs());
    }


    public Collection<UDFInfo> listUDFs() {
        return this.dao.list(UDF_NAMESPACE);
    }

    public Collection<UDFInfo> listUDFs(List<QueryParam> queryParams) {
        return dao.find(UDF_NAMESPACE, queryParams);
    }

    public UDFInfo getUDF(Long id) {
        UDFInfo udfInfo = new UDFInfo();
        udfInfo.setId(id);
        return this.dao.get(new StorableKey(UDF_NAMESPACE, udfInfo.getPrimaryKey()));
    }

    public UDFInfo addUDF(UDFInfo udfInfo) {
        if (udfInfo.getId() == null) {
            udfInfo.setId(this.dao.nextId(UDF_NAMESPACE));
        }
        udfInfo.setName(udfInfo.getName().toUpperCase());
        this.dao.add(udfInfo);
        return udfInfo;
    }

    public Map<String, Class<?>> loadUdfsFromJar(File jarFile) throws IOException {
        Map<String, Class<?>> udafs = new HashMap<>();

        for (Class<?> udfClass : UDF_CLASSES) {
            for (Class<?> clazz : ProxyUtil.loadAllClassesFromJar(jarFile, udfClass)) {
                udafs.put(clazz.getCanonicalName(), clazz);
            }
        }

        return udafs;
    }

    public UDFInfo removeUDF(Long id) {
        UDFInfo udfInfo = new UDFInfo();
        udfInfo.setId(id);
        return dao.remove(new StorableKey(UDF_NAMESPACE, udfInfo.getPrimaryKey()));
    }

    public UDFInfo addOrUpdateUDF(Long udfId, UDFInfo udfInfo) {
        udfInfo.setId(udfId);
        udfInfo.setName(udfInfo.getName().toUpperCase());
        this.dao.addOrUpdate(udfInfo);
        return udfInfo;
    }

    public Cluster importClusterServices(ServiceNodeDiscoverer serviceNodeDiscoverer, Cluster cluster) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        // remove all of relevant services and associated components
        Collection<Service> services = listServices(cluster.getId());
        for (Service service : services) {
            Collection<Component> components = listComponents(service.getId());
            for (Component component : components) {
                removeComponent(component.getId());
            }

            removeService(service.getId());
        }

        List<String> availableServices = serviceNodeDiscoverer.getServices();

        availableServices.parallelStream()
                .filter(ServiceConfigurations::contains)
                .forEach(serviceName -> {
                    LOG.debug("service start {}", serviceName);

                    Map<String, Object> flattenConfigurations = new HashMap<>();
                    Map<String, Map<String, Object>> configurations = serviceNodeDiscoverer.getConfigurations(serviceName);

                    Service service = initializeService(cluster, serviceName);
                    addService(service);

                    LOG.debug("service added {}", serviceName);

                    configurations.entrySet().parallelStream()
                            .forEach(entry -> {
                                try {
                                    String confType = entry.getKey();
                                    Map<String, Object> configuration = entry.getValue();

                                    LOG.debug("conf-type start {}", confType);

                                    String actualFileName = serviceNodeDiscoverer.getActualFileName(confType);

                                    ServiceConfiguration serviceConfiguration = initializeServiceConfiguration(objectMapper,
                                            service.getId(), confType, actualFileName, configuration);

                                    addServiceConfiguration(serviceConfiguration);
                                    flattenConfigurations.putAll(configuration);

                                    LOG.debug("conf-type end {}", confType);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });

                    List<String> components = serviceNodeDiscoverer.getComponents(serviceName);
                    components.parallelStream().forEach(componentName -> {
                        LOG.debug("component start {}", componentName);

                        List<String> hosts = serviceNodeDiscoverer.getComponentNodes(serviceName, componentName);
                        Component component = initializeComponent(service, componentName, hosts);

                        setProtocolAndPortIfAvailable(flattenConfigurations, component);

                        addComponent(component);

                        LOG.debug("component end {}", componentName);
                    });
        });

        return cluster;
    }

    private TopologyLayout getTopologyLayout(Topology topology) throws IOException {
        return new TopologyLayout(topology.getId(), topology.getName(),
                                  topology.getConfig(), topology.getTopologyDag());
    }

    private org.apache.streamline.streams.layout.component.Component getComponentLayout(TopologyComponent component) {
        StreamlineComponent componentLayout = new StreamlineComponent() {
            @Override
            public void accept(TopologyDagVisitor visitor) {
                throw new UnsupportedOperationException("Not intended to be called here.");
            }
        };
        componentLayout.setId(component.getId().toString());
        componentLayout.setName(component.getName());
        return componentLayout;
    }

    private Service initializeService(Cluster cluster, String serviceName) {
        Service service = new Service();
        service.setId(this.dao.nextId(SERVICE_NAMESPACE));
        service.setName(serviceName);
        service.setClusterId(cluster.getId());
        service.setTimestamp(System.currentTimeMillis());
        return service;
    }

    private Component initializeComponent(Service service, String componentName, List<String> hosts) {
        Component component = new Component();
        component.setId(this.dao.nextId(COMPONENT_NAMESPACE));
        component.setName(componentName);
        component.setServiceId(service.getId());
        component.setTimestamp(System.currentTimeMillis());
        component.setHosts(hosts);
        return component;
    }

    private ServiceConfiguration initializeServiceConfiguration(ObjectMapper objectMapper, Long serviceId,
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

    private void setProtocolAndPortIfAvailable(Map<String, Object> configurations, Component component) {
        try {
            ComponentPropertyPattern confMap = ComponentPropertyPattern
                    .valueOf(component.getName());
            Object valueObj = configurations.get(confMap.getConnectionConfName());
            if (valueObj != null) {
                Matcher matcher = confMap.getParsePattern().matcher(valueObj.toString());

                if (matcher.matches()) {
                    String protocol = matcher.group(1);
                    String portStr = matcher.group(2);

                    if (!protocol.isEmpty()) {
                        component.setProtocol(protocol);
                    }
                    if (!portStr.isEmpty()) {
                        try {
                            component.setPort(Integer.parseInt(portStr));
                        } catch (NumberFormatException e) {
                            LOG.warn(
                                    "Protocol/Port information [{}] for component {} doesn't seem to known format [{}]."
                                            + "skip assigning...", valueObj, component.getName(), confMap.getParsePattern());

                            // reset protocol information
                            component.setProtocol(null);
                        }
                    }
                } else {
                    LOG.warn("Protocol/Port information [{}] for component {} doesn't seem to known format [{}]. "
                                     + "skip assigning...", valueObj, component.getName(), confMap.getParsePattern());
                }
            } else {
                LOG.warn("Protocol/Port related configuration ({}) is not set", confMap.getConnectionConfName());
            }
        } catch (IllegalArgumentException e) {
            // don't know port related configuration
        }
    }

    private void loadTransformationClassForBundle (TopologyComponentBundle topologyComponentBundle, InputStream bundleJar) {
        if (topologyComponentBundle.getStreamingEngine().equals(TopologyLayoutConstants.STORM_STREAMING_ENGINE)) {
            if (topologyComponentBundle.getBuiltin()) {
                // no transformation class validations for top level topology type
                if (topologyComponentBundle.getType() == TopologyComponentBundle.TopologyComponentType.TOPOLOGY) {
                    return;
                }
                try {
                    FluxComponent fluxComponent = (FluxComponent) Class.forName(topologyComponentBundle.getTransformationClass()).newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    LOG.debug("Got exception", e);
                    throw new RuntimeException("Check transformationClass property. Cannot load builtin transformation class " + topologyComponentBundle
                            .getTransformationClass());
                }
            } else {
                ProxyUtil<FluxComponent> fluxComponentProxyUtil = new ProxyUtil<FluxComponent>(FluxComponent.class);
                OutputStream os = null;
                InputStream is = null;
                try {
                    File tmpFile = File.createTempFile(UUID.randomUUID().toString(), ".jar");
                    tmpFile.deleteOnExit();
                    os = new FileOutputStream(tmpFile);
                    ByteStreams.copy(bundleJar, os);
                    FluxComponent fluxComponent = fluxComponentProxyUtil.loadClassFromJar(tmpFile.getAbsolutePath(), topologyComponentBundle
                            .getTransformationClass());
                } catch (Exception ex) {
                    LOG.debug("Got exception", ex);
                    throw new RuntimeException("Cannot load transformation class " + topologyComponentBundle.getTransformationClass() + " from bundle Jar: ");
                } finally {
                    try {
                        if (os != null) {
                            os.close();
                        }
                    } catch (IOException ex) {
                        LOG.error("Got exception", ex);
                    }
                }
            }
        }
    }

    private String getTopologyComponentBundleJarName (TopologyComponentBundle topologyComponentBundle) {
        List<String> jarFileName = Arrays.asList(topologyComponentBundle.getStreamingEngine(), topologyComponentBundle.getType().name(), topologyComponentBundle
                .getSubType(), UUID.randomUUID().toString(), ".jar");
        String bundleJarFileName = String.join("-", jarFileName);
        return bundleJarFileName;
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

    private Collection<FileInfo> listFiles(List<QueryParam> queryParams) {
        return dao.find(FileInfo.NAME_SPACE, queryParams);
    }

    private TopologyActions getTopologyActionsInstance(Topology ds) {
        Namespace namespace = getNamespace(ds.getNamespaceId());
        if (namespace == null) {
            throw new RuntimeException("Corresponding namespace not found: " + ds.getNamespaceId());
        }

        TopologyActions topologyActions = topologyActionsContainer.findInstance(namespace);
        if (topologyActions == null) {
            throw new RuntimeException("Can't find Topology Actions for such namespace " + ds.getNamespaceId());
        }
        return topologyActions;
    }

    private TopologyMetrics getTopologyMetricsInstance(Topology ds) {
        Namespace namespace = getNamespace(ds.getNamespaceId());
        if (namespace == null) {
            throw new RuntimeException("Corresponding namespace not found: " + ds.getNamespaceId());
        }

        TopologyMetrics topologyMetrics = topologyMetricsContainer.findInstance(namespace);
        if (topologyMetrics == null) {
            throw new RuntimeException("Can't find Topology Metrics for such namespace " + ds.getNamespaceId());
        }
        return topologyMetrics;
    }

    private void invalidateTopologyActionsMetricsInstances(Long namespaceId) {
        try {
            topologyActionsContainer.invalidateInstance(namespaceId);
            topologyMetricsContainer.invalidateInstance(namespaceId);
        } catch (Throwable e) {
            // swallow
        }
    }

}
