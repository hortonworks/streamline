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
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.Schema;
import org.apache.streamline.common.util.FileStorage;
import org.apache.streamline.common.util.JsonSchemaValidator;
import org.apache.streamline.common.util.ProxyUtil;
import org.apache.streamline.storage.Storable;
import org.apache.streamline.storage.StorableKey;
import org.apache.streamline.storage.StorageManager;
import org.apache.streamline.storage.exception.StorageException;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.Component;
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
import org.apache.streamline.streams.catalog.UDFInfo;
import org.apache.streamline.streams.catalog.WindowInfo;
import org.apache.streamline.streams.catalog.configuration.ConfigFileType;
import org.apache.streamline.streams.catalog.configuration.ConfigFileWriter;
import org.apache.streamline.streams.catalog.processor.CustomProcessorInfo;
import org.apache.streamline.streams.catalog.rule.RuleParser;
import org.apache.streamline.streams.catalog.topology.ConfigField;
import org.apache.streamline.streams.catalog.topology.TopologyComponentDefinition;
import org.apache.streamline.streams.catalog.topology.TopologyLayoutValidator;
import org.apache.streamline.streams.catalog.topology.component.TopologyDagBuilder;
import org.apache.streamline.streams.cluster.discovery.ServiceNodeDiscoverer;
import org.apache.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import org.apache.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import org.apache.streamline.streams.layout.TopologyLayoutConstants;
import org.apache.streamline.streams.layout.component.InputComponent;
import org.apache.streamline.streams.layout.component.Stream;
import org.apache.streamline.streams.layout.component.TopologyActions;
import org.apache.streamline.streams.layout.component.TopologyDag;
import org.apache.streamline.streams.layout.component.TopologyLayout;
import org.apache.streamline.streams.layout.component.impl.NotificationSink;
import org.apache.streamline.streams.layout.component.rule.Rule;
import org.apache.streamline.streams.layout.exception.BadTopologyLayoutException;
import org.apache.streamline.streams.metrics.topology.TopologyMetrics;
import org.apache.streamline.streams.rule.UDAF;
import org.apache.streamline.streams.rule.UDAF2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Set;
import java.util.regex.Matcher;

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
    private static final String STREAMINFO_NAMESPACE = new StreamInfo().getNameSpace();
    private static final String TOPOLOGY_COMPONENT_NAMESPACE = new TopologyComponent().getNameSpace();
    private static final String TOPOLOGY_SOURCE_NAMESPACE = new TopologySource().getNameSpace();
    private static final String TOPOLOGY_SOURCE_STREAM_MAPPING_NAMESPACE = new TopologySourceStreamMapping().getNameSpace();
    private static final String TOPOLOGY_SINK_NAMESPACE = new TopologySink().getNameSpace();
    private static final String TOPOLOGY_PROCESSOR_NAMESPACE = new TopologyProcessor().getNameSpace();
    private static final String TOPOLOGY_PROCESSOR_STREAM_MAPPING_NAMESPACE = new TopologyProcessorStreamMapping().getNameSpace();
    private static final String TOPOLOGY_EDGE_NAMESPACE = new TopologyEdge().getNameSpace();
    private static final String TOPOLOGY_RULEINFO_NAMESPACE = new RuleInfo().getNameSpace();
    private static final String TOPOLOGY_WINDOWINFO_NAMESPACE = new WindowInfo().getNameSpace();
    private static final String UDF_NAMESPACE = new UDFInfo().getNameSpace();

    private final StorageManager dao;
    private final TopologyActions topologyActions;
    private final TopologyMetrics topologyMetrics;
    private final FileStorage fileStorage;
    private final TopologyDagBuilder topologyDagBuilder;
    private final ConfigFileWriter configFileWriter;

    public StreamCatalogService(StorageManager dao,
                                TopologyActions topologyActions,
                                TopologyMetrics topologyMetrics,
                                FileStorage fileStorage) {
        this.dao = dao;
        dao.registerStorables(getStorableClasses());
        this.topologyActions = topologyActions;
        this.topologyMetrics = topologyMetrics;
        this.fileStorage = fileStorage;
        this.topologyDagBuilder = new TopologyDagBuilder(this);
        this.configFileWriter = new ConfigFileWriter();
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
        return this.dao.find(SERVICE_NAMESPACE,
            Collections.singletonList(new QueryParam("clusterId", clusterId.toString())));
    }

    public Collection<Service> listServices(List<QueryParam> params) {
        return dao.find(SERVICE_NAMESPACE, params);
    }

    public Service getService(Long serviceId) {
        Service service = new Service();
        service.setId(serviceId);
        return this.dao.get(new StorableKey(SERVICE_NAMESPACE, service.getPrimaryKey()));
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
        return dao.find(COMPONENT_NAMESPACE,
            Collections.singletonList(new QueryParam("serviceId", serviceId.toString())));
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
        return dao.find(SERVICE_CONFIGURATION_NAMESPACE,
            Collections.singletonList(new QueryParam("serviceId", serviceId.toString())));
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


    public Collection<Topology> listTopologies() {
        return this.dao.list(TOPOLOGY_NAMESPACE);
    }

    public Topology getTopology(Long topologyId) {
        Topology topology = new Topology();
        topology.setId(topologyId);
        return this.dao.get(topology.getStorableKey());
    }

    public Topology addTopology(Topology topology) {
        if (topology.getId() == null) {
            topology.setId(this.dao.nextId(TOPOLOGY_NAMESPACE));
        }
        if (topology.getTimestamp() == null) {
            topology.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(topology);
        return topology;
    }

    public Topology removeTopology(Long topologyIdId) {
        Topology topology = new Topology();
        topology.setId(topologyIdId);
        return dao.remove(new StorableKey(TOPOLOGY_NAMESPACE, topology
                .getPrimaryKey()));
    }

    public Topology addOrUpdateTopology(Long topologyId, Topology
            topology) {
        topology.setId(topologyId);
        topology.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(topology);
        return topology;
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
                throw new BadTopologyLayoutException("Topology with id "
                        + topologyId + " failed to validate against json "
                        + "schema");
            }
            // if first step succeeds, proceed to other validations that
            // cannot be covered using json schema
            TopologyLayoutValidator validator = new TopologyLayoutValidator(json);
            validator.validate();

            // finally pass it on for streaming engine based config validations
            this.topologyActions.validate(getTopologyLayout(result));
        }
        return result;
    }

    public void deployTopology(Topology topology) throws Exception {
        TopologyDag dag = topologyDagBuilder.getDag(topology);
        topology.setTopologyDag(dag);
        LOG.debug("Deploying topology {}", topology);
        addUpdateNotifierInfoFromTopology(topology);
        setUpClusterArtifacts(topology);
        setUpUdfJars(topology);
        topologyActions.deploy(getTopologyLayout(topology));
    }

    private void setUpUdfJars(Topology topology) throws IOException {
        StormTopologyUdfJarHandler udfHandler = new StormTopologyUdfJarHandler();
        topology.getTopologyDag().traverse(udfHandler);
        Set<String> udfs = udfHandler.getUdfs();
        Path udfJarsDir = topologyActions.getExtraJarsLocation(getTopologyLayout(topology));
        Set<UDFInfo> udfsToShip = new HashSet<>();
        makeEmptyDir(udfJarsDir);
        for (String udf: udfs) {
            Collection<UDFInfo> udfInfos = listUDFs(Collections.singletonList(new QueryParam(UDFInfo.NAME, udf)));
            if(udfInfos.size() > 1) {
                throw new IllegalArgumentException("Multiple UDF definitions with name:" + udf);
            } else if (udfInfos.size() == 1) {
                udfsToShip.add(udfInfos.iterator().next());
            }
        }
        Set<String> copiedJars = new HashSet<>();
        for (UDFInfo udf : udfsToShip) {
            String jarPath = udf.getJarStoragePath();
            if (!copiedJars.contains(jarPath)) {
                File destPath = Paths.get(udfJarsDir.toString(), Paths.get(jarPath).getFileName().toString()).toFile();
                try (InputStream src = fileStorage.downloadFile(jarPath);
                     FileOutputStream dest = new FileOutputStream(destPath)
                ) {
                    IOUtils.copy(src, dest);
                    copiedJars.add(jarPath);
                    LOG.debug("Jar {} copied to {}", jarPath, destPath);
                }
            }
        }
    }

    private void setUpClusterArtifacts(Topology topology) throws IOException {
        String config = topology.getConfig();
        ObjectMapper objectMapper = new ObjectMapper();
        Map jsonMap = objectMapper.readValue(config, Map.class);
        Path artifactsDir = topologyActions.getArtifactsLocation(getTopologyLayout(topology));
        makeEmptyDir(artifactsDir);
        if (jsonMap != null) {
            List<Object> serviceList = (List<Object>) jsonMap.get(TopologyLayoutConstants.JSON_KEY_SERVICES);
            if (serviceList != null) {
                List<Service> services = objectMapper.readValue(objectMapper.writeValueAsString(serviceList),
                        new TypeReference<List<Service>>() {
                        });

                for (Service service : services) {
                    Collection<ServiceConfiguration> configurations = listServiceConfigurations(service.getId());

                    for (ServiceConfiguration configuration : configurations) {
                        writeConfigurationFile(objectMapper, artifactsDir, configuration);
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
                final String errorMessage = String
                    .format("Location '%s' must be a directory.", path);
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
        topologyActions.kill(getTopologyLayout(topology));
    }

    public void suspendTopology(Topology topology) throws Exception {
        topologyActions.suspend(getTopologyLayout(topology));
    }

    public void resumeTopology(Topology topology) throws Exception {
        topologyActions.resume(getTopologyLayout(topology));
    }

    public TopologyActions.Status topologyStatus(Topology topology) throws Exception {
        return this.topologyActions.status(getTopologyLayout(topology));
    }

    public Map<String, TopologyMetrics.ComponentMetric> getTopologyMetrics(Topology topology) throws Exception {
        return this.topologyMetrics.getMetricsForTopology(getTopologyLayout(topology));
    }

    public Map<Long, Double> getCompleteLatency(Topology topology, String sourceId, long from, long to) throws Exception {
        return this.topologyMetrics.getCompleteLatency(getTopologyLayout(topology), sourceId, from, to);
    }

    public Map<String, Map<Long, Double>> getComponentStats(Topology topology, String sourceId, Long from, Long to) throws IOException {
        return this.topologyMetrics.getComponentStats(getTopologyLayout(topology), sourceId, from, to);
    }

    public Map<String, Map<Long, Double>> getKafkaTopicOffsets(Topology topology, String sourceId, Long from, Long to) throws IOException {
        return this.topologyMetrics.getkafkaTopicOffsets(getTopologyLayout(topology), sourceId, from, to);
    }

    public Map<String, Map<Long, Double>> getMetrics(String metricName, String parameters, Long from, Long to) {
        return this.topologyMetrics.getTimeSeriesQuerier().getRawMetrics(metricName, parameters, from, to);
    }

    public TopologyMetrics.TopologyMetric getTopologyMetric(Topology topology) throws IOException {
        return this.topologyMetrics.getTopologyMetric(getTopologyLayout(topology));
    }

    public Collection<TopologyComponentDefinition.TopologyComponentType> listTopologyComponentTypes() {
        return Arrays.asList(TopologyComponentDefinition.TopologyComponentType.values());
    }

    public Collection<TopologyComponentDefinition> listTopologyComponentsForTypeWithFilter(TopologyComponentDefinition.TopologyComponentType componentType, List<QueryParam> params) {
        List<TopologyComponentDefinition> topologyComponentDefinitions = new ArrayList<>();
        String ns = TopologyComponentDefinition.NAME_SPACE;
        Collection<TopologyComponentDefinition> filtered = dao.find(ns, params);
        for (TopologyComponentDefinition tc : filtered) {
            if (tc.getType().equals(componentType)) {
                topologyComponentDefinitions.add(tc);
            }
        }
        return topologyComponentDefinitions;
    }

    public TopologyComponentDefinition getTopologyComponent(Long topologyComponentId) {
        TopologyComponentDefinition topologyComponentDefinition = new TopologyComponentDefinition();
        topologyComponentDefinition.setId(topologyComponentId);
        return this.dao.get(topologyComponentDefinition.getStorableKey());
    }

    public TopologyComponentDefinition addTopologyComponent(TopologyComponentDefinition
                                                                    topologyComponentDefinition) {
        if (topologyComponentDefinition.getId() == null) {
            topologyComponentDefinition.setId(this.dao.nextId(TopologyComponentDefinition.NAME_SPACE));
        }
        if (topologyComponentDefinition.getTimestamp() == null) {
            topologyComponentDefinition.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(topologyComponentDefinition);
        return topologyComponentDefinition;
    }

    public TopologyComponentDefinition addOrUpdateTopologyComponent(Long id, TopologyComponentDefinition topologyComponentDefinition) {
        topologyComponentDefinition.setId(id);
        topologyComponentDefinition.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(topologyComponentDefinition);
        return topologyComponentDefinition;
    }

    public TopologyComponentDefinition removeTopologyComponent(Long id) {
        TopologyComponentDefinition topologyComponentDefinition = new TopologyComponentDefinition();
        topologyComponentDefinition.setId(id);
        return dao.remove(new StorableKey(TopologyComponentDefinition.NAME_SPACE, topologyComponentDefinition.getPrimaryKey()));
    }

    public InputStream getFileFromJarStorage(String fileName) throws IOException {
        return this.fileStorage.downloadFile(fileName);
    }

    public Collection<CustomProcessorInfo> listCustomProcessorsWithFilter(List<QueryParam> params) throws IOException {
        Collection<TopologyComponentDefinition> customProcessors = this.listCustomProcessorsComponentsWithFilter(params);
        Collection<CustomProcessorInfo> result = new ArrayList<>();
        for (TopologyComponentDefinition cp : customProcessors) {
            CustomProcessorInfo customProcessorInfo = new CustomProcessorInfo();
            customProcessorInfo.fromTopologyComponent(cp);
            result.add(customProcessorInfo);
        }
        return result;
    }

    private Collection<TopologyComponentDefinition> listCustomProcessorsComponentsWithFilter(List<QueryParam> params) throws IOException {
        List<QueryParam> queryParamsForTopologyComponent = new ArrayList<>();
        queryParamsForTopologyComponent.add(new QueryParam(TopologyComponentDefinition.SUB_TYPE, TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_SUB_TYPE));
        for (QueryParam qp : params) {
            if (qp.getName().equals(TopologyComponentDefinition.STREAMING_ENGINE)) {
                queryParamsForTopologyComponent.add(qp);
            }
        }
        Collection<TopologyComponentDefinition> customProcessors = this.listTopologyComponentsForTypeWithFilter(TopologyComponentDefinition.TopologyComponentType.PROCESSOR,
                queryParamsForTopologyComponent);
        Collection<TopologyComponentDefinition> result = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (TopologyComponentDefinition cp : customProcessors) {
            List<ConfigField> configFields = mapper.readValue(cp.getConfig(), new TypeReference<List<ConfigField>>() {
            });
            Map<String, Object> config = new HashMap<>();
            for (ConfigField configField : configFields) {
                config.put(configField.getName(), configField.getDefaultValue());
            }
            boolean matches = true;
            for (QueryParam qp : params) {
                if (!qp.getName().equals(TopologyComponentDefinition.STREAMING_ENGINE) && !qp.getValue().equals(config.get(qp.getName()))) {
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

    public CustomProcessorInfo addCustomProcessorInfo(CustomProcessorInfo customProcessorInfo, InputStream jarFile) throws IOException {
        try {
            uploadFileToStorage(jarFile, customProcessorInfo.getJarFileName());
            TopologyComponentDefinition topologyComponentDefinition = customProcessorInfo.toTopologyComponent();
            topologyComponentDefinition.setId(this.dao.nextId(TopologyComponentDefinition.NAME_SPACE));
            this.dao.add(topologyComponentDefinition);
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

    public CustomProcessorInfo updateCustomProcessorInfo(CustomProcessorInfo customProcessorInfo, InputStream jarFile) throws
            IOException {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(CustomProcessorInfo.NAME, customProcessorInfo.getName()));
        Collection<TopologyComponentDefinition> result = this.listCustomProcessorsComponentsWithFilter(queryParams);
        if (result.isEmpty() || result.size() != 1) {
            throw new IOException("Failed to update custom processor with name:" + customProcessorInfo.getName());
        }

        uploadFileToStorage(jarFile, customProcessorInfo.getJarFileName());
        TopologyComponentDefinition customProcessorComponent = result.iterator().next();
        TopologyComponentDefinition newCustomProcessorComponent = customProcessorInfo.toTopologyComponent();
        newCustomProcessorComponent.setId(customProcessorComponent.getId());
        this.dao.addOrUpdate(newCustomProcessorComponent);

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

    public CustomProcessorInfo removeCustomProcessorInfo(String name) throws IOException {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(CustomProcessorInfo.NAME, name));
        Collection<TopologyComponentDefinition> result = this.listCustomProcessorsComponentsWithFilter(queryParams);
        if (result.isEmpty() || result.size() != 1) {
            throw new IOException("Failed to delete custom processor with name:" + name);
        }
        TopologyComponentDefinition customProcessorComponent = result.iterator().next();
        CustomProcessorInfo customProcessorInfo = new CustomProcessorInfo();
        customProcessorInfo.fromTopologyComponent(customProcessorComponent);
        deleteFileFromStorage(customProcessorInfo.getJarFileName());
        this.dao.remove(customProcessorComponent.getStorableKey());
        return customProcessorInfo;
    }

    public Collection<TopologyEditorMetadata> listTopologyEditorMetadata() {
        return this.dao.list(TopologyEditorMetadata.NAME_SPACE);
    }

    public TopologyEditorMetadata getTopologyEditorMetadata(Long topologyId) {
        TopologyEditorMetadata topologyEditorMetadata = new TopologyEditorMetadata();
        topologyEditorMetadata.setTopologyId(topologyId);
        return this.dao.get(topologyEditorMetadata.getStorableKey());
    }

    public TopologyEditorMetadata addTopologyEditorMetadata(TopologyEditorMetadata topologyEditorMetadata) {
        if (topologyEditorMetadata.getTimestamp() == null) {
            topologyEditorMetadata.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(topologyEditorMetadata);
        return topologyEditorMetadata;
    }

    public TopologyEditorMetadata addOrUpdateTopologyEditorMetadata(Long topologyId, TopologyEditorMetadata topologyEditorMetadata) {
        topologyEditorMetadata.setTopologyId(topologyId);
        topologyEditorMetadata.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(topologyEditorMetadata);
        return topologyEditorMetadata;
    }

    public TopologyEditorMetadata removeTopologyEditorMetadata(Long topologyId) {
        TopologyEditorMetadata topologyEditorMetadata = new TopologyEditorMetadata();
        topologyEditorMetadata.setTopologyId(topologyId);
        return dao.remove(topologyEditorMetadata.getStorableKey());
    }

    /*
     * This method is present because currently NotificationBolt expects a notifier name and queries NotifierInfo
     * rest endpoint to retrieve the information for that notifier. However, there is no UI present for NotifierInfo
     * rest endpoint. As a result, in order for NotificationBolt to work as is, we need to
     * add or update any notification sink components present in the topology json before we deploy the topology
     */
    private void addUpdateNotifierInfoFromTopology(Topology topology) throws Exception {
        for (InputComponent inputComponent : topology.getTopologyDag().getInputComponents()) {
            if (inputComponent instanceof NotificationSink) {
                NotifierInfo notifierInfo = populateNotifierInfo((NotificationSink) inputComponent);
                NotifierInfo existingNotifierInfo = this.getNotifierInfoByName(notifierInfo.getName());
                if (existingNotifierInfo != null) {
                    this.addOrUpdateNotifierInfo(existingNotifierInfo.getId(), notifierInfo);
                } else {
                    this.addNotifierInfo(notifierInfo);
                }
            }
        }
    }

    private NotifierInfo populateNotifierInfo(NotificationSink notificationSink) {
        NotifierInfo notifierInfo = new NotifierInfo();
        notifierInfo.setName(notificationSink.getNotifierName());
        notifierInfo.setClassName(notificationSink.getNotifierClassName());
        notifierInfo.setJarFileName(notificationSink.getNotifierJarFileName());
        notifierInfo.setProperties(convertMapValuesToString(notificationSink.getNotifierProperties()));
        notifierInfo.setFieldValues(convertMapValuesToString(notificationSink.getNotifierFieldValues()));
        return notifierInfo;
    }

    private Map<String, String> convertMapValuesToString(Map<String, Object> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            Object val = e.getValue();
            if (val != null) {
                result.put(e.getKey(), val.toString());
            }
        }
        return result;
    }

    private NotifierInfo getNotifierInfoByName(String notifierName) throws Exception {
        NotifierInfo notifierInfo = null;
        QueryParam queryParam = new QueryParam(NotifierInfo.NOTIFIER_NAME, notifierName);
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(queryParam);
        Collection<NotifierInfo> existingNotifiers = this.listNotifierInfos(queryParams);
        if ((existingNotifiers != null) && !existingNotifiers.isEmpty()) {
            notifierInfo = existingNotifiers.iterator().next();
        }
        return notifierInfo;
    }

    public TopologySource getTopologySource(Long id) {
        TopologySource topologySource = new TopologySource();
        topologySource.setId(id);
        TopologySource source = dao.get(new StorableKey(TOPOLOGY_SOURCE_NAMESPACE, topologySource.getPrimaryKey()));
        fillSourceStreams(source);
        return source;
    }

    /**
     * Generate id from the {@link TopologyComponent} namespace
     * so that its unique across source, sink and processors.
     * Similar to Table per concrete class hibernate strategy.
     */
    private long getNextTopologyComponentId() {
        TopologyComponent component = new TopologyComponent();
        Long id = dao.nextId(TOPOLOGY_COMPONENT_NAMESPACE);
        component.setId(id);
        dao.add(component);
        dao.remove(component.getStorableKey());
        return id;
    }

    public TopologySource addTopologySource(Long topologyId, TopologySource topologySource) {
        if (topologySource.getId() == null) {
            topologySource.setId(getNextTopologyComponentId());
        }
        topologySource.setTopologyId(topologyId);
        List<StreamInfo> streamInfos = addTopologyOutputComponent(topologySource);
        addSourceStreamMapping(topologySource, topologySource.getOutputStreamIds());
        topologySource.setOutputStreams(streamInfos);
        return topologySource;
    }

    private List<StreamInfo> addTopologyOutputComponent(TopologyOutputComponent outputComponent) {
        List<StreamInfo> streamInfos;
        if (outputComponent.getOutputStreamIds() != null) {
            streamInfos = getOutputStreams(outputComponent.getOutputStreamIds());
        } else if (outputComponent.getOutputStreams() != null) {
            streamInfos = addOutputStreams(outputComponent.getTopologyId(), outputComponent.getOutputStreams());
            outputComponent.setOutputStreamIds(new ArrayList<>(Collections2.transform(streamInfos, new Function<StreamInfo, Long>() {
                @Override
                public Long apply(StreamInfo input) {
                    return input.getId();
                }
            })));
        } else {
            streamInfos = Collections.emptyList();
            outputComponent.setOutputStreamIds(Collections.<Long>emptyList());
        }
        dao.add(outputComponent);
        return streamInfos;
    }

    private List<StreamInfo> getOutputStreams(List<Long> outputStreamIds) {
        List<StreamInfo> streamInfos = new ArrayList<>();
        for (Long outputStreamId : outputStreamIds) {
            StreamInfo streamInfo;
            if ((streamInfo = getStreamInfo(outputStreamId)) == null) {
                throw new IllegalArgumentException("Output stream with id '" + outputStreamId + "' does not exist.");
            }
            streamInfos.add(streamInfo);
        }
        return streamInfos;
    }

    public TopologySource addOrUpdateTopologySource(Long topologyid, Long id, TopologySource topologySource) {
        topologySource.setId(id);
        topologySource.setTopologyId(topologyid);
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
        return getTopologySource(id);
    }

    private List<Long> updateOutputStreams(TopologyOutputComponent outputComponent) {
        List<Long> newStreamIds = new ArrayList<>();
        for (StreamInfo streamInfo : outputComponent.getOutputStreams()) {
            if (streamInfo.getId() != null && getStreamInfo(streamInfo.getId()) != null) {
                addOrUpdateStreamInfo(outputComponent.getTopologyId(), streamInfo.getId(), streamInfo);
                newStreamIds.add(streamInfo.getId());
            } else {
                newStreamIds.add(addStreamInfo(outputComponent.getTopologyId(), streamInfo).getId());
            }
        }
        return newStreamIds;
    }

    public TopologySource removeTopologySource(Long id) {
        TopologySource topologySource = getTopologySource(id);
        dao.<TopologySource>remove(new StorableKey(TOPOLOGY_SOURCE_NAMESPACE, topologySource.getPrimaryKey()));
        removeSourceStreamMapping(topologySource);
        return topologySource;
    }

    public Collection<TopologySource> listTopologySources() {
        return fillSourceStreams(dao.<TopologySource>list(TOPOLOGY_SOURCE_NAMESPACE));
    }

    public Collection<TopologySource> listTopologySources(List<QueryParam> params) throws Exception {
        return fillSourceStreams(dao.<TopologySource>find(TOPOLOGY_SOURCE_NAMESPACE, params));
    }

    private List<StreamInfo> addOutputStreams(Long topologyId, List<StreamInfo> streams) {
        List<StreamInfo> streamInfos = new ArrayList<>();
        for (StreamInfo outputStream : streams) {
            streamInfos.add(addStreamInfo(topologyId, outputStream));
        }
        return streamInfos;
    }

    private void addSourceStreamMapping(TopologySource topologySource, List<Long> streamIds) {
        for (Long outputStreamId : streamIds) {
            dao.<TopologySourceStreamMapping>add(new TopologySourceStreamMapping(topologySource.getId(), outputStreamId));
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
                TopologySourceStreamMapping mapping = new TopologySourceStreamMapping(topologySource.getId(), outputStreamId);
                dao.<TopologySourceStreamMapping>remove(mapping.getStorableKey());
            }
        }
    }

    private List<Long> getOutputStreamIds(TopologySource topologySource) {
        List<Long> streamIds = new ArrayList<>();
        if (topologySource != null) {
            QueryParam qp1 = new QueryParam(TopologySourceStreamMapping.FIELD_SOURCE_ID,
                    String.valueOf(topologySource.getId()));
            for (TopologySourceStreamMapping mapping : listTopologySourceStreamMapping(ImmutableList.of(qp1))) {
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
            for (TopologyProcessorStreamMapping mapping : listTopologyProcessorStreamMapping(ImmutableList.of(qp1))) {
                StreamInfo streamInfo = getStreamInfo(mapping.getStreamId());
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
            for (TopologySourceStreamMapping mapping : listTopologySourceStreamMapping(ImmutableList.of(qp1))) {
                StreamInfo streamInfo = getStreamInfo(mapping.getStreamId());
                if (streamInfo != null) {
                    streams.add(streamInfo);
                }
            }
        }
        return streams;
    }

    public TopologySink getTopologySink(Long id) {
        TopologySink topologySink = new TopologySink();
        topologySink.setId(id);
        return dao.get(new StorableKey(TOPOLOGY_SINK_NAMESPACE, topologySink.getPrimaryKey()));
    }

    public TopologySink addTopologySink(Long topologyId, TopologySink topologySink) {
        if (topologySink.getId() == null) {
            topologySink.setId(getNextTopologyComponentId());
        }
        topologySink.setTopologyId(topologyId);
        dao.add(topologySink);
        return topologySink;
    }

    public TopologySink addOrUpdateTopologySink(Long topologyid, Long id, TopologySink topologySink) {
        topologySink.setId(id);
        topologySink.setTopologyId(topologyid);
        dao.addOrUpdate(topologySink);
        return topologySink;
    }

    public TopologySink removeTopologySink(Long id) {
        TopologySink topologySink = new TopologySink();
        topologySink.setId(id);
        return dao.remove(new StorableKey(TOPOLOGY_SINK_NAMESPACE, topologySink.getPrimaryKey()));
    }

    public Collection<TopologySink> listTopologySinks() {
        return dao.list(TOPOLOGY_SINK_NAMESPACE);
    }

    public Collection<TopologySink> listTopologySinks(List<QueryParam> params) throws Exception {
        return dao.find(TOPOLOGY_SINK_NAMESPACE, params);
    }

    public TopologyProcessor getTopologyProcessor(Long id) {
        TopologyProcessor topologyProcessor = new TopologyProcessor();
        topologyProcessor.setId(id);
        TopologyProcessor processor = dao.get(
                new StorableKey(TOPOLOGY_PROCESSOR_NAMESPACE, topologyProcessor.getPrimaryKey()));
        fillProcessorStreams(processor);
        return processor;
    }

    public TopologyProcessor addTopologyProcessor(Long topologyId, TopologyProcessor topologyProcessor) {
        if (topologyProcessor.getId() == null) {
            topologyProcessor.setId(getNextTopologyComponentId());
        }
        topologyProcessor.setTopologyId(topologyId);
        List<StreamInfo> streamInfos = addTopologyOutputComponent(topologyProcessor);
        addProcessorStreamMapping(topologyProcessor, topologyProcessor.getOutputStreamIds());
        topologyProcessor.setOutputStreams(streamInfos);
        return topologyProcessor;
    }

    public TopologyProcessor addOrUpdateTopologyProcessor(Long topologyid, Long id, TopologyProcessor topologyProcessor) {
        topologyProcessor.setId(id);
        topologyProcessor.setTopologyId(topologyid);
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
        return getTopologyProcessor(id);
    }

    public TopologyProcessor removeTopologyProcessor(Long id) {
        TopologyProcessor topologyProcessor = getTopologyProcessor(id);
        dao.<TopologyProcessor>remove(new StorableKey(TOPOLOGY_PROCESSOR_NAMESPACE, topologyProcessor.getPrimaryKey()));
        removeProcessorStreamMapping(topologyProcessor);
        return topologyProcessor;
    }

    public Collection<TopologyProcessor> listTopologyProcessors() {
        return fillProcessorStreams(dao.<TopologyProcessor>list(TOPOLOGY_PROCESSOR_NAMESPACE));
    }

    public Collection<TopologyProcessor> listTopologyProcessors(List<QueryParam> params) throws Exception {
        return fillProcessorStreams(dao.<TopologyProcessor>find(TOPOLOGY_PROCESSOR_NAMESPACE, params));
    }

    private void createProcessorStreamMapping(TopologyProcessor topologyProcessor, List<StreamInfo> streams) {
        for (StreamInfo outputStream : streams) {
            StreamInfo addedStream = addStreamInfo(topologyProcessor.getTopologyId(), outputStream);
            dao.<TopologyProcessorStreamMapping>add(new TopologyProcessorStreamMapping(topologyProcessor.getId(), addedStream.getId()));
        }
    }

    private void addProcessorStreamMapping(TopologyProcessor topologyProcessor, List<Long> streamIds) {
        for (Long outputStreamId : streamIds) {
            dao.<TopologyProcessorStreamMapping>add(new TopologyProcessorStreamMapping(topologyProcessor.getId(), outputStreamId));
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
                TopologyProcessorStreamMapping mapping = new TopologyProcessorStreamMapping(topologyProcessor.getId(), outputStreamId);
                dao.<TopologyProcessorStreamMapping>remove(mapping.getStorableKey());
            }
        }
    }

    private List<Long> getOutputStreamIds(TopologyProcessor topologyProcessor) {
        List<Long> streamIds = new ArrayList<>();
        if (topologyProcessor != null) {
            QueryParam qp1 = new QueryParam(TopologyProcessorStreamMapping.FIELD_PROCESSOR_ID,
                    String.valueOf(topologyProcessor.getId()));
            for (TopologyProcessorStreamMapping mapping : listTopologyProcessorStreamMapping(ImmutableList.of(qp1))) {
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

    public TopologyEdge getTopologyEdge(Long id) {
        TopologyEdge topologyEdge = new TopologyEdge();
        topologyEdge.setId(id);
        return dao.get(new StorableKey(TOPOLOGY_EDGE_NAMESPACE, topologyEdge.getPrimaryKey()));
    }

    public TopologyEdge addTopologyEdge(Long topologyId, TopologyEdge topologyEdge) {
        if (topologyEdge.getId() == null) {
            topologyEdge.setId(dao.nextId(TOPOLOGY_EDGE_NAMESPACE));
        }
        topologyEdge.setTopologyId(topologyId);
        validateEdge(topologyEdge);
        checkDuplicateEdge(topologyEdge);
        dao.add(topologyEdge);
        return topologyEdge;
    }

    // validate from, to and stream ids of the edge
    private void validateEdge(TopologyEdge edge) {
        TopologySource source = getTopologySource(edge.getFromId());
        TopologyProcessor processor = getTopologyProcessor(edge.getFromId());
        if ((source == null || !source.getTopologyId().equals(edge.getTopologyId()))
                && (processor == null || !processor.getTopologyId().equals(edge.getTopologyId()))) {
            throw new IllegalArgumentException("Invalid source for edge " + edge);
        }
        TopologyOutputComponent outputComponent = source != null ? source : processor;
        processor = getTopologyProcessor(edge.getToId());
        TopologySink sink = getTopologySink(edge.getToId());
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
                        Collections2.transform(getStreamInfo(streamGrouping.getStreamId()).getFields(),
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
        queryParams.add(new QueryParam("topologyId", edge.getTopologyId().toString()));
        queryParams.add(new QueryParam("fromId", edge.getFromId().toString()));
        queryParams.add(new QueryParam("toId", edge.getToId().toString()));

        try {
            Collection<TopologyEdge> existingEdges = listTopologyEdges(queryParams);
            if (existingEdges != null && !existingEdges.isEmpty()) {
                throw new IllegalArgumentException("Edge already exists between source and destination, use update api");
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public TopologyEdge addOrUpdateTopologyEdge(Long topologyid, Long id, TopologyEdge topologyEdge) {
        topologyEdge.setId(id);
        topologyEdge.setTopologyId(topologyid);
        validateEdge(topologyEdge);
        dao.addOrUpdate(topologyEdge);
        return topologyEdge;
    }

    public TopologyEdge removeTopologyEdge(Long id) {
        TopologyEdge topologyEdge = new TopologyEdge();
        topologyEdge.setId(id);
        return dao.remove(new StorableKey(TOPOLOGY_EDGE_NAMESPACE, topologyEdge.getPrimaryKey()));
    }

    public Collection<TopologyEdge> listTopologyEdges() {
        return dao.list(TOPOLOGY_EDGE_NAMESPACE);
    }

    public Collection<TopologyEdge> listTopologyEdges(List<QueryParam> params) throws Exception {
        return dao.find(TOPOLOGY_EDGE_NAMESPACE, params);
    }

    public StreamInfo getStreamInfo(Long id) {
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.setId(id);
        return dao.get(new StorableKey(STREAMINFO_NAMESPACE, streamInfo.getPrimaryKey()));
    }

    public StreamInfo addStreamInfo(Long topologyId, StreamInfo streamInfo) {
        if (streamInfo.getId() == null) {
            streamInfo.setId(dao.nextId(STREAMINFO_NAMESPACE));
        }
        if (streamInfo.getTimestamp() == null) {
            streamInfo.setTimestamp(System.currentTimeMillis());
        }
        streamInfo.setTopologyId(topologyId);
        dao.add(streamInfo);
        return streamInfo;
    }

    public StreamInfo addOrUpdateStreamInfo(Long topologyId, Long id, StreamInfo stream) {
        stream.setId(id);
        stream.setTopologyId(topologyId);
        stream.setTimestamp(System.currentTimeMillis());
        dao.addOrUpdate(stream);
        return stream;
    }

    public StreamInfo removeStreamInfo(Long id) {
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.setId(id);
        return dao.remove(new StorableKey(STREAMINFO_NAMESPACE, streamInfo.getPrimaryKey()));
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
        if (ruleInfo.getId() == null) {
            ruleInfo.setId(dao.nextId(TOPOLOGY_RULEINFO_NAMESPACE));
        }
        ruleInfo.setTopologyId(topologyId);
        String parsedRuleStr = parseAndSerialize(ruleInfo);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        ruleInfo.setParsedRuleStr(parsedRuleStr);
        dao.add(ruleInfo);
        return ruleInfo;
    }

    public RuleInfo getRule(Long id) throws Exception {
        RuleInfo ruleInfo = new RuleInfo();
        ruleInfo.setId(id);
        return dao.get(new StorableKey(TOPOLOGY_RULEINFO_NAMESPACE, ruleInfo.getPrimaryKey()));
    }

    public RuleInfo addOrUpdateRule(Long topologyid, Long ruleId, RuleInfo ruleInfo) throws Exception {
        ruleInfo.setId(ruleId);
        ruleInfo.setTopologyId(topologyid);
        String parsedRuleStr = parseAndSerialize(ruleInfo);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        ruleInfo.setParsedRuleStr(parsedRuleStr);
        dao.addOrUpdate(ruleInfo);
        return ruleInfo;
    }

    public RuleInfo removeRule(Long id) {
        RuleInfo ruleInfo = new RuleInfo();
        ruleInfo.setId(id);
        return dao.remove(
                new StorableKey(TOPOLOGY_RULEINFO_NAMESPACE, ruleInfo.getPrimaryKey()));
    }

    public Collection<WindowInfo> listWindows() {
        return dao.list(TOPOLOGY_WINDOWINFO_NAMESPACE);
    }

    public Collection<WindowInfo> listWindows(List<QueryParam> params) throws Exception {
        return dao.find(TOPOLOGY_WINDOWINFO_NAMESPACE, params);
    }

    public WindowInfo addWindow(Long topologyId, WindowInfo windowInfo) throws Exception {
        if (windowInfo.getId() == null) {
            windowInfo.setId(dao.nextId(TOPOLOGY_WINDOWINFO_NAMESPACE));
        }
        windowInfo.setTopologyId(topologyId);
        String parsedRuleStr = parseAndSerialize(windowInfo);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        windowInfo.setParsedRuleStr(parsedRuleStr);
        dao.add(windowInfo);
        return windowInfo;
    }

    public WindowInfo getWindow(Long id) throws Exception {
        WindowInfo windowInfo = new WindowInfo();
        windowInfo.setId(id);
        return dao.get(new StorableKey(TOPOLOGY_WINDOWINFO_NAMESPACE, windowInfo.getPrimaryKey()));
    }

    public WindowInfo addOrUpdateWindow(Long topologyid, Long ruleId, WindowInfo windowInfo) throws Exception {
        windowInfo.setId(ruleId);
        windowInfo.setTopologyId(topologyid);
        String parsedRuleStr = parseAndSerialize(windowInfo);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        windowInfo.setParsedRuleStr(parsedRuleStr);
        dao.addOrUpdate(windowInfo);
        return windowInfo;
    }

    public WindowInfo removeWindow(Long id) {
        WindowInfo ruleInfo = new WindowInfo();
        ruleInfo.setId(id);
        return dao.remove(
                new StorableKey(TOPOLOGY_WINDOWINFO_NAMESPACE, ruleInfo.getPrimaryKey()));
    }

    private String parseAndSerialize(RuleInfo ruleInfo) throws JsonProcessingException {
        Rule rule = new Rule();
        rule.setId(ruleInfo.getId());
        rule.setName(ruleInfo.getName());
        rule.setDescription(ruleInfo.getDescription());
        rule.setWindow(ruleInfo.getWindow());
        rule.setActions(ruleInfo.getActions());
        if (ruleInfo.getStreams() != null && !ruleInfo.getStreams().isEmpty()) {
            ruleInfo.setSql(
                    getSqlString(ruleInfo.getStreams(), null, ruleInfo.getCondition(), null));
        } else if (StringUtils.isEmpty(ruleInfo.getSql())) {
            throw new IllegalArgumentException("Either streams or sql string should be specified.");
        }
        parseSql(rule, ruleInfo.getSql(), ruleInfo.getTopologyId());
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
        parseSql(rule, sql, windowInfo.getTopologyId());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(rule);
    }

    private String getSqlString(List<String> streams,
                                List<WindowInfo.Projection> projections,
                                String condition,
                                List<String> groupByKeys) {
        String SQL = select(projections).or("SELECT * ");
        SQL += join(" FROM ", getTable(streams)).get();
        SQL += join(" WHERE ", condition).or("");
        SQL += join(" GROUP BY ", groupByKeys).or("");
        return SQL;
    }

    private Optional<String> select(List<WindowInfo.Projection> projections) {
        if (projections != null) {
            return join("SELECT ", Collections2.transform(projections, new Function<WindowInfo.Projection, String>() {
                @Override
                public String apply(WindowInfo.Projection input) {
                    return input.toString();
                }
            }));
        }
        return Optional.absent();
    }

    private Optional<String> join(String keyword, String part) {
        if (part != null) {
            return join(keyword, Collections.singletonList(part));
        }
        return Optional.absent();
    }

    private Optional<String> join(String keyword, Collection<String> parts) {
        if (parts != null && !parts.isEmpty()) {
            return Optional.of(keyword + Joiner.on(",").join(parts));
        }
        return Optional.absent();
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


    private void parseSql(Rule rule, String sql, long topologyId) {
        // parse
        RuleParser ruleParser = new RuleParser(this, sql, topologyId);
        ruleParser.parse();
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

    public Map<String, Class<?>> loadUdafsFromJar(File jarFile) throws IOException {
        Map<String, Class<?>> udafs = new HashMap<>();
        for (Class<?> clazz : ProxyUtil.loadAllClassesFromJar(jarFile, UDAF.class)) {
            udafs.put(clazz.getCanonicalName(), clazz);
        }
        for (Class<?> clazz : ProxyUtil.loadAllClassesFromJar(jarFile, UDAF2.class)) {
            udafs.put(clazz.getCanonicalName(), clazz);
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

        for (ServiceConfigurations svcConfMap : ServiceConfigurations.values()) {
            String serviceName = svcConfMap.name();

            if (availableServices.contains(serviceName)) {
                Map<String, Object> flattenConfigurations = new HashMap<>();
                Map<String, Map<String, Object>> configurations = serviceNodeDiscoverer.getConfigurations(serviceName);

                Service service = initializeService(cluster, serviceName);
                addService(service);

                for (Map.Entry<String, Map<String, Object>> confTypeToConfiguration : configurations
                    .entrySet()) {
                    String confType = confTypeToConfiguration.getKey();
                    Map<String, Object> configuration = confTypeToConfiguration.getValue();
                    String actualFileName = serviceNodeDiscoverer.getActualFileName(confType);

                    ServiceConfiguration serviceConfiguration = initializeServiceConfiguration(objectMapper,
                        service.getId(), confType, actualFileName, configuration);

                    addServiceConfiguration(serviceConfiguration);
                    flattenConfigurations.putAll(configuration);
                }

                List<String> components = serviceNodeDiscoverer.getComponents(serviceName);
                for (String componentName : components) {
                    List<String> hosts = serviceNodeDiscoverer.getComponentNodes(serviceName, componentName);
                    Component component = initializeComponent(service, componentName, hosts);

                    setProtocolAndPortIfAvailable(flattenConfigurations, component);

                    addComponent(component);
                }
            }
        }

        return cluster;
    }

    private TopologyLayout getTopologyLayout(Topology topology) throws IOException {
        return new TopologyLayout(topology.getId(), topology.getName(),
                topology.getConfig(), topology.getTopologyDag());
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

}
