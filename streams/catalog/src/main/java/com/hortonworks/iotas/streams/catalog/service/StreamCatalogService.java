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
package com.hortonworks.iotas.streams.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hortonworks.iotas.common.util.FileStorage;
import com.hortonworks.iotas.common.util.JsonSchemaValidator;
import com.hortonworks.iotas.streams.catalog.*;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.DataSourceSubType;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.exception.StorageException;
import com.hortonworks.iotas.streams.catalog.processor.CustomProcessorInfo;
import com.hortonworks.iotas.streams.catalog.rule.RuleParser;
import com.hortonworks.iotas.streams.catalog.topology.ConfigField;
import com.hortonworks.iotas.streams.catalog.topology.TopologyComponentDefinition;
import com.hortonworks.iotas.streams.catalog.topology.TopologyLayoutValidator;
import com.hortonworks.iotas.streams.catalog.topology.component.TopologyDagBuilder;
import com.hortonworks.iotas.streams.layout.component.TopologyActions;
import com.hortonworks.iotas.streams.layout.component.TopologyLayout;
import com.hortonworks.iotas.streams.layout.TopologyLayoutConstants;
import com.hortonworks.iotas.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.iotas.streams.layout.component.InputComponent;
import com.hortonworks.iotas.streams.layout.component.Stream;
import com.hortonworks.iotas.streams.layout.component.TopologyDag;
import com.hortonworks.iotas.streams.layout.component.impl.NotificationSink;
import com.hortonworks.iotas.streams.layout.component.rule.Rule;
import com.hortonworks.iotas.streams.layout.exception.BadTopologyLayoutException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import static com.hortonworks.iotas.streams.catalog.TopologyEdge.StreamGrouping;

/**
 * A service layer where we could put our business logic.
 * Right now this exists as a very thin layer between the DAO and
 * the REST controllers.
 */
public class StreamCatalogService {

    private static final Logger LOG = LoggerFactory.getLogger(StreamCatalogService.class);

    // TODO: the namespace and Id generation logic should be moved inside DAO
    private static final String CLUSTER_NAMESPACE = new Cluster().getNameSpace();
    private static final String COMPONENT_NAMESPACE = new Component().getNameSpace();
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
    private static final String UDF_NAMESPACE = new UDFInfo().getNameSpace();

    private StorageManager dao;
    private TopologyActions topologyActions;
    private TopologyMetrics topologyMetrics;
    private FileStorage fileStorage;
    private TopologyDagBuilder topologyDagBuilder;

    public StreamCatalogService(StorageManager dao, TopologyActions topologyActions, TopologyMetrics topologyMetrics, FileStorage fileStorage) {
        this.dao = dao;
        dao.registerStorables(getStorableClasses());
        this.topologyActions = topologyActions;
        this.topologyMetrics = topologyMetrics;
        this.fileStorage = fileStorage;
        this.topologyDagBuilder = new TopologyDagBuilder(this);
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
        return this.dao.<Cluster>list(CLUSTER_NAMESPACE);
    }


    public Collection<Cluster> listClusters(List<QueryParam> params) throws Exception {
        return dao.<Cluster>find(CLUSTER_NAMESPACE, params);
    }


    public Cluster getCluster(Long clusterId) {
        Cluster cluster = new Cluster();
        cluster.setId(clusterId);
        return this.dao.<Cluster>get(new StorableKey(CLUSTER_NAMESPACE, cluster.getPrimaryKey()));
    }

    public Cluster removeCluster(Long clusterId) {
        Cluster cluster = new Cluster();
        cluster.setId(clusterId);
        return dao.<Cluster>remove(new StorableKey(CLUSTER_NAMESPACE, cluster.getPrimaryKey()));
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

    public Component addComponent(Long clusterId, Component component) {
        if (component.getId() == null) {
            component.setId(this.dao.nextId(COMPONENT_NAMESPACE));
        }
        if (component.getTimestamp() == null) {
            component.setTimestamp(System.currentTimeMillis());
        }
        component.setClusterId(clusterId);
        this.dao.add(component);
        return component;
    }

    public Collection<Component> listComponents() {
        return this.dao.<Component>list(COMPONENT_NAMESPACE);

    }

    public Collection<Component> listComponents(List<QueryParam> queryParams) throws Exception {
        return dao.<Component>find(COMPONENT_NAMESPACE, queryParams);
    }

    public Component getComponent(Long componentId) {
        Component component = new Component();
        component.setId(componentId);
        return this.dao.<Component>get(new StorableKey(COMPONENT_NAMESPACE, component.getPrimaryKey()));
    }

    public Component removeComponent(Long componentId) {
        Component component = new Component();
        component.setId(componentId);
        return dao.<Component>remove(new StorableKey(COMPONENT_NAMESPACE, component.getPrimaryKey()));
    }


    public Component addOrUpdateComponent(Long clusterId, Component component) {
        return addOrUpdateComponent(clusterId, component.getId(), component);
    }

    public Component addOrUpdateComponent(Long clusterId, Long componentId, Component component) {
        component.setClusterId(clusterId);
        component.setId(componentId);
        component.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(component);
        return component;
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
        return this.dao.<NotifierInfo>get(new StorableKey(NOTIFIER_INFO_NAMESPACE, notifierInfo.getPrimaryKey()));
    }

    public Collection<NotifierInfo> listNotifierInfos() {
        return this.dao.<NotifierInfo>list(NOTIFIER_INFO_NAMESPACE);
    }

    public Collection<NotifierInfo> listNotifierInfos(List<QueryParam> params) throws Exception {
        return dao.<NotifierInfo>find(NOTIFIER_INFO_NAMESPACE, params);
    }


    public NotifierInfo removeNotifierInfo(Long notifierId) {
        NotifierInfo notifierInfo = new NotifierInfo();
        notifierInfo.setId(notifierId);
        return dao.<NotifierInfo>remove(new StorableKey(NOTIFIER_INFO_NAMESPACE, notifierInfo.getPrimaryKey()));
    }


    public NotifierInfo addOrUpdateNotifierInfo(Long id, NotifierInfo notifierInfo) {
        notifierInfo.setId(id);
        notifierInfo.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(notifierInfo);
        return notifierInfo;
    }


    public Collection<Topology> listTopologies () {
        Collection<Topology> topologies = this.dao.list(TOPOLOGY_NAMESPACE);
        return topologies;
    }

    public Topology getTopology (Long topologyId) {
        Topology topology = new Topology();
        topology.setId(topologyId);
        Topology result = this.dao.get(topology.getStorableKey());
        return result;
    }

    public Topology addTopology (Topology topology) {
        if (topology.getId() == null) {
            topology.setId(this.dao.nextId(TOPOLOGY_NAMESPACE));
        }
        if (topology.getTimestamp() == null) {
            topology.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(topology);
        return topology;
    }

    public Topology removeTopology (Long topologyIdId) {
        Topology topology = new Topology();
        topology.setId(topologyIdId);
        return dao.remove(new StorableKey(TOPOLOGY_NAMESPACE, topology
                .getPrimaryKey()));
    }

    public Topology addOrUpdateTopology (Long topologyId, Topology
            topology) {
        topology.setId(topologyId);
        topology.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(topology);
        return topology;
    }

    public Topology validateTopology (URL schema, Long topologyId)
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
        topologyActions.deploy(getTopologyLayout(topology));
    }

    private void setUpClusterArtifacts(Topology topology) throws IOException {
        String config = topology.getConfig();
        ObjectMapper objectMapper = new ObjectMapper();
        Map jsonMap = objectMapper.readValue(config, Map.class);
        if (jsonMap != null) {
            List<Object> clusterList = (List<Object>) jsonMap.get(TopologyLayoutConstants.JSON_KEY_CLUSTERS);
            if (clusterList != null) {
                List<Cluster> clusters = objectMapper.readValue(objectMapper.writeValueAsString(clusterList),
                                                                new TypeReference<List<Cluster>>() {
                                                                });
                Path artifactsDir = topologyActions.getArtifactsLocation(getTopologyLayout(topology));
                if (artifactsDir.toFile().exists()) {
                    if (artifactsDir.toFile().isDirectory()) {
                        FileUtils.cleanDirectory(artifactsDir.toFile());
                    } else {
                        final String errorMessage = String.format("Artifacts location '%s' must be a directory.", artifactsDir);
                        LOG.error(errorMessage);
                        throw new IOException(errorMessage);
                    }
                } else if (!artifactsDir.toFile().mkdirs()) {
                    LOG.error("Could not create artifacts dir {}", artifactsDir);
                    throw new IOException("Could not create artifacts dir: " + artifactsDir);
                }
                for (Cluster c : clusters) {
                    Cluster cluster = getCluster(c.getId());
                    String resource = cluster.getClusterConfigStorageName();
                    File destPath = Paths.get(artifactsDir.toString(), cluster.getClusterConfigFileName()).toFile();
                    try (
                            InputStream src = fileStorage.downloadFile(resource);
                            FileOutputStream dest = new FileOutputStream(destPath);
                    ) {
                        IOUtils.copy(src, dest);
                        LOG.debug("Resource {} copied to {}", resource, destPath);
                    }
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

    public Map<String, TopologyMetrics.ComponentMetric> getTopologyMetrics (Topology topology) throws Exception {
        return this.topologyMetrics.getMetricsForTopology(getTopologyLayout(topology));
    }

    public Map<Long, Double> getCompleteLatency (Topology topology, String sourceId, long from, long to) throws Exception {
        return this.topologyMetrics.getCompleteLatency(getTopologyLayout(topology), sourceId, from, to);
    }

    public Map<String, Map<Long, Double>> getComponentStats(Topology topology, String sourceId, Long from, Long to) {
        return this.topologyMetrics.getComponentStats(getTopologyLayout(topology), sourceId, from, to);
    }

    public Map<String, Map<Long, Double>> getKafkaTopicOffsets(Topology topology, String sourceId, Long from, Long to) {
        return this.topologyMetrics.getkafkaTopicOffsets(getTopologyLayout(topology), sourceId, from, to);
    }

    public Map<String, Map<Long, Double>> getMetrics(String metricName, String parameters, Long from, Long to) {
        return this.topologyMetrics.getTimeSeriesQuerier().getRawMetrics(metricName, parameters, from, to);
    }

    public Collection<TopologyComponentDefinition.TopologyComponentType> listTopologyComponentTypes() {
        return Arrays.asList(TopologyComponentDefinition.TopologyComponentType.values());
    }

    public Collection<TopologyComponentDefinition> listTopologyComponentsForTypeWithFilter (TopologyComponentDefinition.TopologyComponentType componentType, List<QueryParam> params) {
        List<TopologyComponentDefinition> topologyComponentDefinitions = new
                ArrayList<TopologyComponentDefinition>();
        String ns = TopologyComponentDefinition.NAME_SPACE;
        Collection<TopologyComponentDefinition> filtered = dao.<TopologyComponentDefinition>find(ns, params);
        for (TopologyComponentDefinition tc: filtered) {
            if (tc.getType().equals(componentType)) {
                topologyComponentDefinitions.add(tc);
            }
        }
        return topologyComponentDefinitions;
    }

    public TopologyComponentDefinition getTopologyComponent (Long topologyComponentId) {
        TopologyComponentDefinition topologyComponentDefinition = new TopologyComponentDefinition();
        topologyComponentDefinition.setId(topologyComponentId);
        TopologyComponentDefinition result = this.dao.get(topologyComponentDefinition.getStorableKey());
        return result;
    }

    public TopologyComponentDefinition addTopologyComponent (TopologyComponentDefinition
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

    public TopologyComponentDefinition addOrUpdateTopologyComponent (Long id, TopologyComponentDefinition topologyComponentDefinition) {
        topologyComponentDefinition.setId(id);
        topologyComponentDefinition.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(topologyComponentDefinition);
        return topologyComponentDefinition;
    }

    public TopologyComponentDefinition removeTopologyComponent (Long id) {
        TopologyComponentDefinition topologyComponentDefinition = new TopologyComponentDefinition();
        topologyComponentDefinition.setId(id);
        return dao.remove(new StorableKey(TopologyComponentDefinition.NAME_SPACE, topologyComponentDefinition.getPrimaryKey()));
    }

    public InputStream getFileFromJarStorage(String fileName) throws IOException {
        return this.fileStorage.downloadFile(fileName);
    }

    public Collection<CustomProcessorInfo> listCustomProcessorsWithFilter (List<QueryParam> params) throws IOException {
        Collection<TopologyComponentDefinition> customProcessors = this.listCustomProcessorsComponentsWithFilter(params);
        Collection<CustomProcessorInfo> result = new ArrayList<>();
        for (TopologyComponentDefinition cp: customProcessors) {
            CustomProcessorInfo customProcessorInfo = new CustomProcessorInfo();
            customProcessorInfo.fromTopologyComponent(cp);
            result.add(customProcessorInfo);
        }
        return result;
    }

    private Collection<TopologyComponentDefinition> listCustomProcessorsComponentsWithFilter (List<QueryParam> params) throws IOException {
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
        for (TopologyComponentDefinition cp: customProcessors) {
            List<ConfigField> configFields = mapper.readValue(cp.getConfig(), new TypeReference<List<ConfigField>>() { });
            Map<String, Object> config  = new HashMap<>();
            for (ConfigField configField: configFields) {
                config.put(configField.getName(), configField.getDefaultValue());
            }
            boolean matches = true;
            for (QueryParam qp: params) {
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

    public CustomProcessorInfo addCustomProcessorInfo (CustomProcessorInfo customProcessorInfo, InputStream jarFile, InputStream imageFile) throws IOException {
        uploadFileToStorage(jarFile, customProcessorInfo.getJarFileName());
        uploadFileToStorage(imageFile, customProcessorInfo.getImageFileName());
        TopologyComponentDefinition topologyComponentDefinition = customProcessorInfo.toTopologyComponent();
        topologyComponentDefinition.setId(this.dao.nextId(TopologyComponentDefinition.NAME_SPACE));
        this.dao.add(topologyComponentDefinition);
        return customProcessorInfo;
    }

    public CustomProcessorInfo updateCustomProcessorInfo (CustomProcessorInfo customProcessorInfo, InputStream jarFile, InputStream imageFile) throws
            IOException {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(CustomProcessorInfo.NAME, customProcessorInfo.getName()));
        Collection<TopologyComponentDefinition> result = this.listCustomProcessorsComponentsWithFilter(queryParams);
        if (result.isEmpty() || result.size() != 1) {
            throw new IOException("Failed to update custom processor with name:" + customProcessorInfo.getName());
        }

        uploadFileToStorage(jarFile, customProcessorInfo.getJarFileName());
        uploadFileToStorage(imageFile, customProcessorInfo.getImageFileName());
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

    public CustomProcessorInfo removeCustomProcessorInfo (String name) throws IOException {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(CustomProcessorInfo.NAME, name));
        Collection<TopologyComponentDefinition> result = this.listCustomProcessorsComponentsWithFilter(queryParams);
        if (result.isEmpty() || result.size() != 1) {
            throw new IOException("Failed to delete custom processor with name:" + name);
        }
        TopologyComponentDefinition customProcessorComponent = result.iterator().next();
        this.dao.remove(customProcessorComponent.getStorableKey());
        return new CustomProcessorInfo().fromTopologyComponent(customProcessorComponent);
    }

    public Collection<TopologyEditorMetadata> listTopologyEditorMetadata () {
        Collection<TopologyEditorMetadata> topologyEditorMetadatas = this.dao.list(TopologyEditorMetadata.NAME_SPACE);
        return topologyEditorMetadatas;
    }

    public TopologyEditorMetadata getTopologyEditorMetadata (Long topologyId) {
        TopologyEditorMetadata topologyEditorMetadata = new TopologyEditorMetadata();
        topologyEditorMetadata.setTopologyId(topologyId);
        TopologyEditorMetadata result = this.dao.get(topologyEditorMetadata.getStorableKey());
        return result;
    }

    public TopologyEditorMetadata addTopologyEditorMetadata (TopologyEditorMetadata topologyEditorMetadata) {
        if (topologyEditorMetadata.getTimestamp() == null) {
            topologyEditorMetadata.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(topologyEditorMetadata);
        return topologyEditorMetadata;
    }

    public TopologyEditorMetadata addOrUpdateTopologyEditorMetadata (Long topologyId, TopologyEditorMetadata topologyEditorMetadata) {
        topologyEditorMetadata.setTopologyId(topologyId);
        topologyEditorMetadata.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(topologyEditorMetadata);
        return topologyEditorMetadata;
    }

    public TopologyEditorMetadata removeTopologyEditorMetadata (Long topologyId) {
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
    private void addUpdateNotifierInfoFromTopology (Topology topology) throws Exception {
        for (InputComponent inputComponent: topology.getTopologyDag().getInputComponents()) {
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

    private NotifierInfo populateNotifierInfo (NotificationSink notificationSink) {
        NotifierInfo notifierInfo = new NotifierInfo();
        notifierInfo.setName(notificationSink.getNotifierName());
        notifierInfo.setClassName(notificationSink.getNotifierClassName());
        notifierInfo.setJarFileName(notificationSink.getNotifierJarFileName());
        notifierInfo.setProperties(convertMapValuesToString(notificationSink.getNotifierProperties()));
        notifierInfo.setFieldValues(convertMapValuesToString(notificationSink.getNotifierFieldValues()));
        return notifierInfo;
    }

    private Map<String, String> convertMapValuesToString (Map<String, Object> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            Object val = e.getValue();
            if (val != null) {
                result.put(e.getKey(), val.toString());
            }
        }
        return result;
    }

    private NotifierInfo getNotifierInfoByName (String notifierName) throws Exception {
        NotifierInfo notifierInfo = null;
        QueryParam queryParam = new QueryParam(NotifierInfo.NOTIFIER_NAME, notifierName);
        List<QueryParam> queryParams = new ArrayList<QueryParam>();
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
        setOutputStreamIds(source);
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
        dao.add(topologySource);
        addSourceStreamMapping(topologySource, topologySource.getOutputStreamIds());
        return topologySource;
    }

    public TopologySource addOrUpdateTopologySource(Long topologyid, Long id, TopologySource topologySource) {
        topologySource.setId(id);
        topologySource.setTopologyId(topologyid);
        dao.addOrUpdate(topologySource);
        List<Long> newList = topologySource.getOutputStreamIds();
        List<Long> existing = getOutputStreamIds(topologySource);
        Sets.SetView<Long> streamIdsToRemove = Sets.difference(ImmutableSet.copyOf(existing), ImmutableSet.copyOf(newList));
        Sets.SetView<Long> streamIdsToAdd = Sets.difference(ImmutableSet.copyOf(newList), ImmutableSet.copyOf(existing));
        removeSourceStreamMapping(topologySource, Lists.newArrayList(streamIdsToRemove));
        addSourceStreamMapping(topologySource, Lists.newArrayList(streamIdsToAdd));
        return topologySource;
    }

    public TopologySource removeTopologySource(Long id) {
        TopologySource topologySource = new TopologySource();
        topologySource.setId(id);
        TopologySource source = dao.<TopologySource>remove(new StorableKey(TOPOLOGY_SOURCE_NAMESPACE, topologySource.getPrimaryKey()));
        removeSourceStreamMapping(source);
        return source;
    }

    public Collection<TopologySource> listTopologySources() {
        return fillSourceStreamIds(dao.<TopologySource>list(TOPOLOGY_SOURCE_NAMESPACE));
    }

    public Collection<TopologySource> listTopologySources(List<QueryParam> params) throws Exception {
        return fillSourceStreamIds(dao.<TopologySource>find(TOPOLOGY_SOURCE_NAMESPACE, params));
    }

    private void addSourceStreamMapping(TopologySource topologySource, List<Long> streamIds) {
        if (topologySource.getOutputStreamIds() != null) {
            for (Long outputStreamId : streamIds) {
                dao.<TopologySourceStreamMapping>add(new TopologySourceStreamMapping(topologySource.getId(), outputStreamId));
            }
        }
    }

    private void removeSourceStreamMapping(TopologySource topologySource) {
        if (topologySource != null) {
            removeSourceStreamMapping(topologySource, topologySource.getOutputStreamIds());
        }
    }
    private void removeSourceStreamMapping(TopologySource topologySource, List<Long> streamIds) {
        if (topologySource != null) {
            for (Long outputStreamId: streamIds) {
                TopologySourceStreamMapping mapping = new TopologySourceStreamMapping(topologySource.getId(), outputStreamId);
                dao.<TopologySourceStreamMapping>remove(mapping.getStorableKey());
            }
        }
    }

    private void setOutputStreamIds(TopologySource source) {
        if (source != null) {
            source.setOutputStreamIds(getOutputStreamIds(source));
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
            return dao.<TopologySourceStreamMapping>find(TOPOLOGY_SOURCE_STREAM_MAPPING_NAMESPACE, params);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Collection<TopologySource> fillSourceStreamIds(Collection<TopologySource> sources) {
        for(TopologySource source: sources) {
            source.setOutputStreamIds(getOutputStreamIds(source));
        }
        return sources;
    }

    public TopologySink getTopologySink(Long id) {
        TopologySink topologySink = new TopologySink();
        topologySink.setId(id);
        return dao.<TopologySink>get(new StorableKey(TOPOLOGY_SINK_NAMESPACE, topologySink.getPrimaryKey()));
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
        return dao.<TopologySink>remove(new StorableKey(TOPOLOGY_SINK_NAMESPACE, topologySink.getPrimaryKey()));
    }

    public Collection<TopologySink> listTopologySinks() {
        return dao.<TopologySink>list(TOPOLOGY_SINK_NAMESPACE);
    }

    public Collection<TopologySink> listTopologySinks(List<QueryParam> params) throws Exception {
        return dao.<TopologySink>find(TOPOLOGY_SINK_NAMESPACE, params);
    }

    public TopologyProcessor getTopologyProcessor(Long id) {
        TopologyProcessor topologyProcessor = new TopologyProcessor();
        topologyProcessor.setId(id);
        TopologyProcessor processor = dao.<TopologyProcessor>get(
                new StorableKey(TOPOLOGY_PROCESSOR_NAMESPACE, topologyProcessor.getPrimaryKey()));
        setOutputStreamIds(processor);
        return processor;
    }

    public TopologyProcessor addTopologyProcessor(Long topologyId, TopologyProcessor topologyProcessor) {
        if (topologyProcessor.getId() == null) {
            topologyProcessor.setId(getNextTopologyComponentId());
        }
        topologyProcessor.setTopologyId(topologyId);
        dao.add(topologyProcessor);
        addProcessorStreamMapping(topologyProcessor, topologyProcessor.getOutputStreamIds());
        return topologyProcessor;
    }

    public TopologyProcessor addOrUpdateTopologyProcessor(Long topologyid, Long id, TopologyProcessor topologyProcessor) {
        topologyProcessor.setId(id);
        topologyProcessor.setTopologyId(topologyid);
        dao.addOrUpdate(topologyProcessor);
        List<Long> newList = topologyProcessor.getOutputStreamIds();
        List<Long> existing = getOutputStreamIds(topologyProcessor);
        Sets.SetView<Long> streamIdsToRemove = Sets.difference(ImmutableSet.copyOf(existing), ImmutableSet.copyOf(newList));
        Sets.SetView<Long> streamIdsToAdd = Sets.difference(ImmutableSet.copyOf(newList), ImmutableSet.copyOf(existing));
        removeProcessorStreamMapping(topologyProcessor, Lists.newArrayList(streamIdsToRemove));
        addProcessorStreamMapping(topologyProcessor, Lists.newArrayList(streamIdsToAdd));
        return topologyProcessor;
    }

    public TopologyProcessor removeTopologyProcessor(Long id) {
        TopologyProcessor topologyProcessor = new TopologyProcessor();
        topologyProcessor.setId(id);
        TopologyProcessor processor = dao.<TopologyProcessor>remove(
                new StorableKey(TOPOLOGY_PROCESSOR_NAMESPACE, topologyProcessor.getPrimaryKey()));
        removeProcessorStreamMapping(processor);
        return processor;
    }

    public Collection<TopologyProcessor> listTopologyProcessors() {
        return fillProcessorStreamIds(dao.<TopologyProcessor>list(TOPOLOGY_PROCESSOR_NAMESPACE));
    }

    public Collection<TopologyProcessor> listTopologyProcessors(List<QueryParam> params) throws Exception {
        return fillProcessorStreamIds(dao.<TopologyProcessor>find(TOPOLOGY_PROCESSOR_NAMESPACE, params));
    }

    private void addProcessorStreamMapping(TopologyProcessor topologyProcessor, List<Long> streamIds) {
        if (topologyProcessor.getOutputStreamIds() != null) {
            for (Long outputStreamId : streamIds) {
                dao.<TopologyProcessorStreamMapping>add(new TopologyProcessorStreamMapping(topologyProcessor.getId(), outputStreamId));
            }
        }
    }

    private void removeProcessorStreamMapping(TopologyProcessor topologyProcessor) {
        if (topologyProcessor != null) {
            removeProcessorStreamMapping(topologyProcessor, topologyProcessor.getOutputStreamIds());
        }
    }
    private void removeProcessorStreamMapping(TopologyProcessor topologyProcessor, List<Long> streamIds) {
        if (topologyProcessor != null) {
            for (Long outputStreamId: streamIds) {
                TopologyProcessorStreamMapping mapping = new TopologyProcessorStreamMapping(topologyProcessor.getId(), outputStreamId);
                dao.<TopologyProcessorStreamMapping>remove(mapping.getStorableKey());
            }
        }
    }

    private void setOutputStreamIds(TopologyProcessor processor) {
        if (processor != null) {
            processor.setOutputStreamIds(getOutputStreamIds(processor));
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
            return dao.<TopologyProcessorStreamMapping>find(TOPOLOGY_PROCESSOR_STREAM_MAPPING_NAMESPACE, params);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Collection<TopologyProcessor> fillProcessorStreamIds(Collection<TopologyProcessor> processors) {
        for (TopologyProcessor processor : processors) {
            processor.setOutputStreamIds(getOutputStreamIds(processor));
        }
        return processors;
    }


    public TopologyEdge getTopologyEdge(Long id) {
        TopologyEdge topologyEdge = new TopologyEdge();
        topologyEdge.setId(id);
        return dao.<TopologyEdge>get(new StorableKey(TOPOLOGY_EDGE_NAMESPACE, topologyEdge.getPrimaryKey()));
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

        Set<Long> outputStreamIds = new HashSet<>(outputComponent.getOutputStreamIds());
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
        List<QueryParam> queryParams = new ArrayList<QueryParam>();
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
        return dao.<TopologyEdge>remove(new StorableKey(TOPOLOGY_EDGE_NAMESPACE, topologyEdge.getPrimaryKey()));
    }

    public Collection<TopologyEdge> listTopologyEdges() {
        return dao.<TopologyEdge>list(TOPOLOGY_EDGE_NAMESPACE);
    }

    public Collection<TopologyEdge> listTopologyEdges(List<QueryParam> params) throws Exception {
        return dao.<TopologyEdge>find(TOPOLOGY_EDGE_NAMESPACE, params);
    }

    public StreamInfo getStreamInfo(Long id) {
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.setId(id);
        return dao.<StreamInfo>get(new StorableKey(STREAMINFO_NAMESPACE, streamInfo.getPrimaryKey()));
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
        return dao.<StreamInfo>remove(new StorableKey(STREAMINFO_NAMESPACE, streamInfo.getPrimaryKey()));
    }

    public Collection<StreamInfo> listStreamInfos() {
        return dao.<StreamInfo>list(STREAMINFO_NAMESPACE);
    }

    public Collection<StreamInfo> listStreamInfos(List<QueryParam> params) throws Exception {
        return dao.<StreamInfo>find(STREAMINFO_NAMESPACE, params);
    }

    public Collection<RuleInfo> listRules() {
        return dao.<RuleInfo>list(TOPOLOGY_RULEINFO_NAMESPACE);
    }

    public Collection<RuleInfo> listRules(List<QueryParam> params) throws Exception {
        return dao.<RuleInfo>find(TOPOLOGY_RULEINFO_NAMESPACE, params);
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
        return dao.<RuleInfo>get(new StorableKey(TOPOLOGY_RULEINFO_NAMESPACE, ruleInfo.getPrimaryKey()));
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
        RuleInfo removedRuleInfo = dao.<RuleInfo>remove(
                new StorableKey(TOPOLOGY_RULEINFO_NAMESPACE, ruleInfo.getPrimaryKey()));
        return removedRuleInfo;
    }

    private String parseAndSerialize(RuleInfo ruleInfo) throws JsonProcessingException {
        Rule rule = new Rule();
        rule.setId(ruleInfo.getId());
        rule.setName(ruleInfo.getName());
        rule.setDescription(ruleInfo.getDescription());
        rule.setWindow(ruleInfo.getWindow());
        rule.setActions(ruleInfo.getActions());
        // parse
        RuleParser ruleParser = new RuleParser(this, ruleInfo);
        ruleParser.parse();
        rule.setStreams(new HashSet(Collections2.transform(ruleParser.getStreams(), new Function<Stream, String>() {
            @Override
            public String apply(Stream input) {
                return input.getId();
            }
        })));
        rule.setProjection(ruleParser.getProjection());
        rule.setCondition(ruleParser.getCondition());
        rule.setGroupBy(ruleParser.getGroupBy());
        rule.setHaving(ruleParser.getHaving());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(rule);
    }

    public Collection<UDFInfo> listUDFs() {
        return this.dao.<UDFInfo>list(UDF_NAMESPACE);
    }

    public Collection<UDFInfo> listUDFs(List<QueryParam> queryParams) {
        return dao.<UDFInfo>find(UDF_NAMESPACE, queryParams);
    }

    public UDFInfo getUDF(Long id) {
        UDFInfo udfInfo = new UDFInfo();
        udfInfo.setId(id);
        return this.dao.<UDFInfo>get(new StorableKey(UDF_NAMESPACE, udfInfo.getPrimaryKey()));
    }

    public UDFInfo addUDF(UDFInfo udfInfo) {
        if (udfInfo.getId() == null) {
            udfInfo.setId(this.dao.nextId(UDF_NAMESPACE));
        }
        this.dao.add(udfInfo);
        return udfInfo;
    }

    public UDFInfo removeUDF(Long id) {
        UDFInfo udfInfo = new UDFInfo();
        udfInfo.setId(id);
        return dao.<UDFInfo>remove(new StorableKey(UDF_NAMESPACE, udfInfo.getPrimaryKey()));
    }

    public UDFInfo addOrUpdateUDF(Long udfId, UDFInfo udfInfo) {
        udfInfo.setId(udfId);
        this.dao.addOrUpdate(udfInfo);
        return udfInfo;
    }

    private TopologyLayout getTopologyLayout(Topology topology) {
        return new TopologyLayout(topology.getId(), topology.getName(),
                topology.getConfig(), topology.getTopologyDag());
    }
}
