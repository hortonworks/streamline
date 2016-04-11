package com.hortonworks.iotas.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.catalog.Cluster;
import com.hortonworks.iotas.catalog.Component;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSet;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.catalog.NotifierInfo;
import com.hortonworks.iotas.catalog.Device;
import com.hortonworks.iotas.catalog.Tag;
import com.hortonworks.iotas.catalog.TagStorableMapping;
import com.hortonworks.iotas.catalog.Topology;
import com.hortonworks.iotas.catalog.TopologyEditorMetadata;
import com.hortonworks.iotas.processor.CustomProcessorInfo;
import com.hortonworks.iotas.storage.DataSourceSubType;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.exception.StorageException;
import com.hortonworks.iotas.topology.ConfigField;
import com.hortonworks.iotas.topology.TopologyActions;
import com.hortonworks.iotas.topology.TopologyComponent;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.topology.TopologyLayoutValidator;
import com.hortonworks.iotas.util.CoreUtils;
import com.hortonworks.iotas.util.JarStorage;
import com.hortonworks.iotas.util.JsonSchemaValidator;
import com.hortonworks.iotas.util.exception.BadTopologyLayoutException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A service layer where we could put our business logic.
 * Right now this exists as a very thin layer between the DAO and
 * the REST controllers.
 */
public class CatalogService {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogService.class);

    // TODO: the namespace and Id generation logic should be moved inside DAO
    private static final String DATA_SOURCE_NAMESPACE = new DataSource().getNameSpace();
    private static final String DEVICE_NAMESPACE = new Device().getNameSpace();
    private static final String DATASET_NAMESPACE = new DataSet().getNameSpace();
    private static final String DATA_FEED_NAMESPACE = new DataFeed().getNameSpace();
    private static final String PARSER_INFO_NAMESPACE = new ParserInfo().getNameSpace();
    private static final String CLUSTER_NAMESPACE = new Cluster().getNameSpace();
    private static final String COMPONENT_NAMESPACE = new Component().getNameSpace();
    private static final String NOTIFIER_INFO_NAMESPACE = new NotifierInfo().getNameSpace();
    private static final String TOPOLOGY_NAMESPACE = new Topology()
            .getNameSpace();
    private static final String TAG_NAMESPACE = new Tag().getNameSpace();
    private static final String TAG_STORABLE_MAPPING_NAMESPACE = new TagStorableMapping().getNameSpace();


    private StorageManager dao;
    private TopologyActions topologyActions;
    private JarStorage jarStorage;
    private TagService tagService;

    public static class QueryParam {

        public final String name;
        public final String value;
        public QueryParam(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            QueryParam that = (QueryParam) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            return !(value != null ? !value.equals(that.value) : that.value != null);

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }
    }

    public CatalogService(StorageManager dao, TopologyActions topologyActions, JarStorage jarStorage) {
        this.dao = dao;
        this.topologyActions = topologyActions;
        this.jarStorage = jarStorage;
        this.tagService = new CatalogTagService(dao);
    }

    private String getNamespaceForDataSourceType(DataSource.Type dataSourceType) {
        if (dataSourceType == DataSource.Type.DEVICE) {
            return DEVICE_NAMESPACE;
        } else if (dataSourceType == DataSource.Type.DATASET) {
            return DATASET_NAMESPACE;
        }
        return DataSource.Type.UNKNOWN.toString();
    }

    private DataSourceSubType getSubtypeFromDataSource(DataSource ds) throws IllegalAccessException, InstantiationException {
        String ns = getNamespaceForDataSourceType(ds.getType());
        Class<? extends DataSourceSubType> classForDataSourceType = getClassForDataSourceType(ds.getType());
        DataSourceSubType dataSourcesubType = classForDataSourceType.newInstance();
        dataSourcesubType.setDataSourceId(ds.getId());
        return dao.get(new StorableKey(ns, dataSourcesubType.getPrimaryKey()));
    }

    private Class<? extends DataSourceSubType> getClassForDataSourceType(DataSource.Type dataSourceType) {
        if (dataSourceType == DataSource.Type.DEVICE) {
            return Device.class;
        } else if (dataSourceType == DataSource.Type.DATASET) {
            return DataSet.class;
        }
        throw new IllegalArgumentException("Unknown data source type " + dataSourceType);
    }

    // TODO: implement pagination
    public Collection<DataSource> listDataSources() throws IOException, IllegalAccessException, InstantiationException {
        Collection<DataSource> dataSources = this.dao.<DataSource>list(DATA_SOURCE_NAMESPACE);
        if (dataSources != null) {
            for (DataSource ds : dataSources) {
                DataSourceSubType dataSourcesubType = getSubtypeFromDataSource(ds);
                ds.setTypeConfig(CoreUtils.storableToJson(dataSourcesubType));
                ds.setTags(tagService.getTags(ds));
            }
        }
        return dataSources;
    }

    public Collection<DataSource> listDataSourcesForType(DataSource.Type type, List<QueryParam> params) throws Exception {
        List<DataSource> dataSources = new ArrayList<DataSource>();
        String ns = getNamespaceForDataSourceType(type);
        Collection<DataSourceSubType> subTypes = dao.<DataSourceSubType>find(ns, params);
        for (DataSourceSubType st : subTypes) {
            dataSources.add(getDataSource(st.getDataSourceId()));
        }
        return dataSources;
    }

    public DataSource getDataSource(Long id) throws IOException, InstantiationException, IllegalAccessException {
        DataSource ds = new DataSource();
        ds.setId(id);
        DataSource result = dao.<DataSource>get(new StorableKey(DATA_SOURCE_NAMESPACE, ds.getPrimaryKey()));
        if (result != null) {
            DataSourceSubType subType = getSubtypeFromDataSource(result);
            result.setTypeConfig(CoreUtils.storableToJson(subType));
            result.setTags(tagService.getTags(result));
        }
        return result;
    }

    public DataSource addDataSource(DataSource dataSource) throws IOException {
        if (dataSource.getId() == null) {
            dataSource.setId(this.dao.nextId(DATA_SOURCE_NAMESPACE));
        }
        if (dataSource.getTimestamp() == null) {
            dataSource.setTimestamp(System.currentTimeMillis());
        }
        DataSourceSubType subType = CoreUtils.jsonToStorable(dataSource.getTypeConfig(),
                getClassForDataSourceType(dataSource.getType()));
        subType.setDataSourceId(dataSource.getId());
        this.dao.add(dataSource);
        this.dao.add(subType);
        tagService.addTagsForStorable(dataSource, dataSource.getTags());
        return dataSource;
    }

    public DataSource removeDataSource(Long dataSourceId) throws IOException, IllegalAccessException, InstantiationException {
        DataSource dataSource = getDataSource(dataSourceId);
        if (dataSource != null) {
            /*
            * Delete the child entity first
            */
            String ns = getNamespaceForDataSourceType(dataSource.getType());
            this.dao.remove(new StorableKey(ns, dataSource.getPrimaryKey()));
            dao.<DataSource>remove(new StorableKey(DATA_SOURCE_NAMESPACE, dataSource.getPrimaryKey()));
            tagService.removeTagsFromStorable(dataSource, dataSource.getTags());
        }
        return dataSource;
    }

    public DataSource addOrUpdateDataSource(Long id, DataSource dataSource) throws IOException {
        dataSource.setId(id);
        dataSource.setTimestamp(System.currentTimeMillis());
        DataSourceSubType subType = CoreUtils.jsonToStorable(dataSource.getTypeConfig(),
                getClassForDataSourceType(dataSource.getType()));
        subType.setDataSourceId(dataSource.getId());
        this.dao.addOrUpdate(dataSource);
        this.dao.addOrUpdate(subType);
        tagService.addOrUpdateTagsForStorable(dataSource, dataSource.getTags());
        return dataSource;
    }

    public Collection<DataFeed> listDataFeeds() {
        return this.dao.<DataFeed>list(DATA_FEED_NAMESPACE);
    }

    public Collection<DataFeed> listDataFeeds(List<QueryParam> params) throws Exception {
        return dao.<DataFeed>find(DATA_FEED_NAMESPACE, params);
    }

    public DataFeed getDataFeed(Long dataFeedId) {
        DataFeed df = new DataFeed();
        df.setId(dataFeedId);
        return this.dao.<DataFeed>get(new StorableKey(DATA_FEED_NAMESPACE, df.getPrimaryKey()));
    }

    public DataFeed addDataFeed(DataFeed feed) {
        if (feed.getId() == null) {
            feed.setId(this.dao.nextId(DATA_FEED_NAMESPACE));
        }
        validateDatafeed(feed);
        this.dao.add(feed);
        return feed;
    }

    /**
     * Basic validation
     * 1. Datasource referenced in this datafeed should exist
     * 2. End-points should be unique for Datasets
     */
    private void validateDatafeed(DataFeed feed) {
        DataSource dataSource = null;
        LOG.debug("Validating data feed [{}]", feed);
        try {
            dataSource = getDataSource(feed.getDataSourceId());
        } catch (Exception ex) {
            throw new StorageException("Got exception while validating datafeed [" + feed + "]", ex);
        }
        if (dataSource == null) {
            throw new StorageException("Cannot add Datafeed [" + feed + "] for non existent datasource");
        } else if (dataSource.getType() == DataSource.Type.DATASET) {
            QueryParam qp = new QueryParam("type", feed.getType());
            Collection<DataFeed> existing = null;
            try {
                existing = listDataFeeds(Collections.singletonList(qp));
            } catch (Exception ex) {
                throw new StorageException("Got excepting while listing data feeds with query param " + qp, ex);
            }
            if (!existing.isEmpty()) {
                throw new StorageException("Datafeed type must be unique for a Dataset");
            }
        }
    }

    public DataFeed removeDataFeed(Long dataFeedId) {
        DataFeed feed = new DataFeed();
        feed.setId(dataFeedId);
        return dao.<DataFeed>remove(new StorableKey(DATA_FEED_NAMESPACE, feed.getPrimaryKey()));
    }

    public DataFeed addOrUpdateDataFeed(Long id, DataFeed feed) {
        feed.setId(id);
        this.dao.addOrUpdate(feed);
        return feed;
    }


    public Collection<ParserInfo> listParsers() {
        return dao.<ParserInfo>list(PARSER_INFO_NAMESPACE);
    }

    public Collection<ParserInfo> listParsers(List<QueryParam> queryParams) {
        return dao.<ParserInfo>find(PARSER_INFO_NAMESPACE, queryParams);
    }

    public ParserInfo getParserInfo(Long parserId) {
        ParserInfo parserInfo = new ParserInfo();
        parserInfo.setId(parserId);
        return dao.<ParserInfo>get(new StorableKey(PARSER_INFO_NAMESPACE, parserInfo.getPrimaryKey()));
    }

    public ParserInfo removeParser(Long parserId) {
        ParserInfo parserInfo = new ParserInfo();
        parserInfo.setId(parserId);
        return this.dao.<ParserInfo>remove(new StorableKey(PARSER_INFO_NAMESPACE, parserInfo.getPrimaryKey()));
    }

    public ParserInfo addParserInfo(ParserInfo parserInfo) {
        if (parserInfo.getId() == null) {
            parserInfo.setId(this.dao.nextId(PARSER_INFO_NAMESPACE));
        }
        if (parserInfo.getTimestamp() == null) {
            parserInfo.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(parserInfo);
        return parserInfo;
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
        cluster.setId(clusterId);
        cluster.setTimestamp(System.currentTimeMillis());
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
            this.topologyActions.validate(result);
        }
        return result;
    }

    public void deployTopology (Topology topology) throws Exception {
        this.addUpdateNotifierInfoFromTopology(topology);
        this.topologyActions.deploy(topology);
        return;
    }

    public void killTopology (Topology topology) throws Exception {
        this.topologyActions.kill(topology);
        return;
    }

    public void suspendTopology (Topology topology) throws Exception {
        this.topologyActions.suspend(topology);
        return;
    }

    public void resumeTopology (Topology topology) throws Exception {
        this.topologyActions.resume(topology);
        return;
    }

    public TopologyActions.Status topologyStatus (Topology topology) throws Exception {
        return this.topologyActions.status(topology);
    }

    public Collection<TopologyComponent.TopologyComponentType> listTopologyComponentTypes () {
        return Arrays.asList(TopologyComponent.TopologyComponentType.values());
    }

    public Collection<TopologyComponent> listTopologyComponentsForTypeWithFilter (TopologyComponent.TopologyComponentType componentType, List<QueryParam> params) {
        List<TopologyComponent> topologyComponents = new
                ArrayList<TopologyComponent>();
        String ns = TopologyComponent.NAME_SPACE;
        Collection<TopologyComponent> filtered = dao.<TopologyComponent>find(ns, params);
        for (TopologyComponent tc: filtered) {
            if (tc.getType().equals(componentType)) {
                topologyComponents.add(tc);
            }
        }
        return topologyComponents;
    }

    public TopologyComponent getTopologyComponent (Long topologyComponentId) {
        TopologyComponent topologyComponent = new TopologyComponent();
        topologyComponent.setId(topologyComponentId);
        TopologyComponent result = this.dao.get(topologyComponent.getStorableKey());
        return result;
    }

    public TopologyComponent addTopologyComponent (TopologyComponent
                                                   topologyComponent) {
        if (topologyComponent.getId() == null) {
            topologyComponent.setId(this.dao.nextId(TopologyComponent.NAME_SPACE));
        }
        if (topologyComponent.getTimestamp() == null) {
            topologyComponent.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(topologyComponent);
        return topologyComponent;
    }

    public TopologyComponent addOrUpdateTopologyComponent (Long id, TopologyComponent topologyComponent) {
        topologyComponent.setId(id);
        topologyComponent.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(topologyComponent);
        return topologyComponent;
    }

    public TopologyComponent removeTopologyComponent (Long id) {
        TopologyComponent topologyComponent = new TopologyComponent();
        topologyComponent.setId(id);
        return dao.remove(new StorableKey(TopologyComponent.NAME_SPACE,
                topologyComponent.getPrimaryKey()));
    }

    public InputStream getCustomProcessorFile (String fileName) throws IOException {
        final InputStream inputStream = this.jarStorage.downloadJar(fileName);
        return inputStream;
    }

    public Collection<CustomProcessorInfo> listCustomProcessorsWithFilter (List<QueryParam> params) throws IOException {
        Collection<TopologyComponent> customProcessors = this.listCustomProcessorsComponentsWithFilter(params);
        Collection<CustomProcessorInfo> result = new ArrayList<>();
        for (TopologyComponent cp: customProcessors) {
            CustomProcessorInfo customProcessorInfo = new CustomProcessorInfo();
            customProcessorInfo.fromTopologyComponent(cp);
            result.add(customProcessorInfo);
        }
        return result;
    }

    private Collection<TopologyComponent> listCustomProcessorsComponentsWithFilter (List<QueryParam> params) throws IOException {
        List<QueryParam> queryParamsForTopologyComponent = new ArrayList<>();
        queryParamsForTopologyComponent.add(new QueryParam(TopologyComponent.SUB_TYPE, TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_SUB_TYPE));
        for (QueryParam qp : params) {
            if (qp.getName().equals(TopologyComponent.STREAMING_ENGINE)) {
                queryParamsForTopologyComponent.add(qp);
            }
        }
        Collection<TopologyComponent> customProcessors = this.listTopologyComponentsForTypeWithFilter(TopologyComponent.TopologyComponentType.PROCESSOR,
                queryParamsForTopologyComponent);
        Collection<TopologyComponent> result = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (TopologyComponent cp: customProcessors) {
            List<ConfigField> configFields = mapper.readValue(cp.getConfig(), new TypeReference<List<ConfigField>>() { });
            Map<String, Object> config  = new HashMap<>();
            for (ConfigField configField: configFields) {
                config.put(configField.getName(), configField.getDefaultValue());
            }
            boolean matches = true;
            for (QueryParam qp: params) {
                if (!qp.getName().equals(TopologyComponent.STREAMING_ENGINE) && !qp.getValue().equals(config.get(qp.getName()))) {
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
        this.jarStorage.uploadJar(jarFile, customProcessorInfo.getJarFileName());
        this.jarStorage.uploadJar(imageFile, customProcessorInfo.getImageFileName());
        TopologyComponent topologyComponent = customProcessorInfo.toTopologyComponent();
        topologyComponent.setId(this.dao.nextId(TopologyComponent.NAME_SPACE));
        this.dao.add(topologyComponent);
        return customProcessorInfo;
    }

    public CustomProcessorInfo updateCustomProcessorInfo (CustomProcessorInfo customProcessorInfo, InputStream jarFile, InputStream imageFile) throws
            IOException {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(customProcessorInfo.NAME, customProcessorInfo.getName()));
        Collection<TopologyComponent> result = this.listCustomProcessorsComponentsWithFilter(queryParams);
        if (result.isEmpty() || result.size() != 1) {
            throw new IOException("Failed to update custom processor with name:" + customProcessorInfo.getName());
        }
        this.jarStorage.uploadJar(jarFile, customProcessorInfo.getJarFileName());
        this.jarStorage.uploadJar(imageFile, customProcessorInfo.getImageFileName());
        TopologyComponent customProcessorComponent = result.iterator().next();
        TopologyComponent newCustomProcessorComponent = customProcessorInfo.toTopologyComponent();
        newCustomProcessorComponent.setId(customProcessorComponent.getId());
        this.dao.addOrUpdate(newCustomProcessorComponent);
        return customProcessorInfo;
    }

    public CustomProcessorInfo removeCustomProcessorInfo (String name) throws IOException {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(CustomProcessorInfo.NAME, name));
        Collection<TopologyComponent> result = this.listCustomProcessorsComponentsWithFilter(queryParams);
        if (result.isEmpty() || result.size() != 1) {
            throw new IOException("Failed to delete custom processor with name:" + name);
        }
        TopologyComponent customProcessorComponent = result.iterator().next();
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

    // This method is present because currently NotificationBolt expects a notifier name and queries NotifierInfo rest endpoint to retrieve the information
    // for that notifier. However, there is no UI present for NotifierInfo rest endpoint. As a result, in order for NotificationBolt to work as is, we need to
    // add or update any notification sink components present in the topology json before we deploy the topology
    private void addUpdateNotifierInfoFromTopology (Topology topology) throws Exception {
        String config = topology.getConfig();
        ObjectMapper objectMapper = new ObjectMapper();
        Map jsonMap = objectMapper.readValue(config, Map.class);
        List<Map> notificationSinks = new ArrayList();
        if (jsonMap != null) {
            List<Map> dataSinks = (List) jsonMap.get(TopologyLayoutConstants.JSON_KEY_DATA_SINKS);
            for (Map dataSink: dataSinks) {
                if (("NOTIFICATION").equals(dataSink.get(TopologyLayoutConstants.JSON_KEY_TYPE))) {
                    notificationSinks.add(dataSink);
                }
            }
            for (Map notificationSink: notificationSinks) {
                NotifierInfo notifierInfo = populateNotifierInfo(notificationSink);
                NotifierInfo existingNotifierInfo = this.getNotifierInfoByName(notifierInfo.getName());
                if (existingNotifierInfo != null) {
                    this.addOrUpdateNotifierInfo(existingNotifierInfo.getId(), notifierInfo);
                } else {
                    this.addNotifierInfo(notifierInfo);
                }
            }
        }
    }

    private NotifierInfo populateNotifierInfo (Map notificationSinkConfig) {
        NotifierInfo notifierInfo = new NotifierInfo();
        Map notifierInfoConfig = (Map) notificationSinkConfig.get(TopologyLayoutConstants.JSON_KEY_CONFIG);
        notifierInfo.setName((String) notifierInfoConfig.get(TopologyLayoutConstants.JSON_KEY_NOTIFIER_NAME));
        notifierInfo.setClassName((String) notifierInfoConfig.get(TopologyLayoutConstants.JSON_KEY_NOTIFIER_CLASSNAME));
        notifierInfo.setJarFileName((String) notifierInfoConfig.get(TopologyLayoutConstants.JSON_KEY_NOTIFIER_JAR_FILENAME));
        Map properties = (Map) notifierInfoConfig.get(TopologyLayoutConstants.JSON_KEY_NOTIFIER_PROPERTIES);
        notifierInfo.setProperties(convertMapValuesToString(properties));
        Map fieldValues = (Map) notifierInfoConfig.get(TopologyLayoutConstants.JSON_KEY_NOTIFIER_FIELD_VALUES);
        notifierInfo.setFieldValues(convertMapValuesToString(fieldValues));
        return notifierInfo;
    }

    private Map<String, String> convertMapValuesToString (Map<String, Object> map) {
        Map<String, String> result = new HashMap<String, String>();
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

    public Tag addTag(Tag tag) {
        return tagService.addTag(tag);
    }

    public Tag getTag(Long tagId) {
        return tagService.getTag(tagId);
    }

    public Tag removeTag(Long tagId) {
        return tagService.removeTag(tagId);
    }

    public Tag addOrUpdateTag(Long tagId, Tag tag) {
        return tagService.addOrUpdateTag(tagId, tag);
    }
    
    public Collection<Tag> listTags() {
        return tagService.listTags();
    }

    public Collection<Tag> listTags(List<QueryParam> queryParams) {
        return tagService.listTags(queryParams);
    }

    public List<Storable> getEntities(Long tagId) {
        return tagService.getEntities(tagId, true);
    }

}
