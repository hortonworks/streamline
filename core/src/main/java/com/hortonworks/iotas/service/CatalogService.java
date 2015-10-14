package com.hortonworks.iotas.service;

import com.hortonworks.iotas.catalog.Cluster;
import com.hortonworks.iotas.catalog.Component;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.Device;
import com.hortonworks.iotas.catalog.NotifierInfo;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.catalog.DataStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.storage.DataSourceSubType;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.exception.StorageException;
import com.hortonworks.iotas.util.CoreUtils;
import com.hortonworks.iotas.util.DataStreamActions;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.hortonworks.iotas.util.DataStreamLayoutValidator;
import com.hortonworks.iotas.util.exception.BadDataStreamLayoutException;
import com.hortonworks.iotas.util.JsonSchemaValidator;

import java.net.URL;

/**
 * A service layer where we could put our business logic.
 * Right now this exists as a very thin layer between the DAO and
 * the REST controllers.
 */
public class CatalogService {

    // TODO: the namespace and Id generation logic should be moved inside DAO
    private static final String DATA_SOURCE_NAMESPACE = new DataSource().getNameSpace();
    private static final String DEVICE_NAMESPACE = new Device().getNameSpace();
    private static final String DATA_FEED_NAMESPACE = new DataFeed().getNameSpace();
    private static final String PARSER_INFO_NAMESPACE = new ParserInfo().getNameSpace();
    private static final String CLUSTER_NAMESPACE = new Cluster().getNameSpace();
    private static final String COMPONENT_NAMESPACE = new Component().getNameSpace();
    private static final String NOTIFIER_INFO_NAMESPACE = new NotifierInfo().getNameSpace();
    private static final String DATA_STREAM_NAMESPACE = new DataStream()
            .getNameSpace();

    private StorageManager dao;
    private DataStreamActions dataStreamActions;

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

    public CatalogService(StorageManager dao, DataStreamActions dataStreamActions) {
        this.dao = dao;
        this.dataStreamActions = dataStreamActions;
    }

    private String getNamespaceForDataSourceType(DataSource.Type dataSourceType) {
        if (dataSourceType == DataSource.Type.DEVICE) {
            return DEVICE_NAMESPACE;
        }
        return DataSource.Type.UNKNOWN.toString();
    }

    private Class<? extends DataSourceSubType> getClassForDataSourceType(DataSource.Type dataSourceType) {
        if (dataSourceType == DataSource.Type.DEVICE) {
            return Device.class;
        }
        throw new IllegalArgumentException("Unknown data source type " + dataSourceType);
    }

    // TODO: implement pagination
    public Collection<DataSource> listDataSources() throws IOException {
        Collection<DataSource> dataSources = this.dao.<DataSource>list(DATA_SOURCE_NAMESPACE);
        if (dataSources != null) {
            for (DataSource ds : dataSources) {
                String ns = getNamespaceForDataSourceType(ds.getType());
                DataSourceSubType subType = dao.get(new StorableKey(ns, ds.getPrimaryKey()));
                ds.setTypeConfig(CoreUtils.storableToJson(subType));
            }
        }
        return dataSources;
    }

    public Collection<DataSource> listDataSourcesForType(DataSource.Type type, List<QueryParam> params) throws Exception {
        List<DataSource> dataSources = new ArrayList<DataSource>();
        String ns = getNamespaceForDataSourceType(type);
        Collection<DataSourceSubType> subTypes = dao.<DataSourceSubType>find(ns, params);
        for(DataSourceSubType st: subTypes) {
            dataSources.add(getDataSource(st.getDataSourceId()));
        }
        return dataSources;
    }

    public DataSource getDataSource(Long id) throws IOException {
        DataSource ds = new DataSource();
        ds.setDataSourceId(id);
        DataSource result = dao.<DataSource>get(new StorableKey(DATA_SOURCE_NAMESPACE, ds.getPrimaryKey()));
        if (result != null) {
            String ns = getNamespaceForDataSourceType(result.getType());
            DataSourceSubType subType = dao.get(new StorableKey(ns, ds.getPrimaryKey()));
            result.setTypeConfig(CoreUtils.storableToJson(subType));
        }
        return result;
    }

    public DataSource addDataSource(DataSource dataSource) throws IOException {
        if (dataSource.getDataSourceId() == null) {
            dataSource.setDataSourceId(this.dao.nextId(DATA_SOURCE_NAMESPACE));
        }
        if (dataSource.getTimestamp() == null) {
            dataSource.setTimestamp(System.currentTimeMillis());
        }
        DataSourceSubType subType = CoreUtils.jsonToStorable(dataSource.getTypeConfig(),
                getClassForDataSourceType(dataSource.getType()));
        subType.setDataSourceId(dataSource.getDataSourceId());
        this.dao.add(dataSource);
        this.dao.add(subType);
        return dataSource;
    }

    public DataSource removeDataSource(Long dataSourceId) throws IOException {
        DataSource dataSource = getDataSource(dataSourceId);
        if (dataSource != null) {
            /*
            * Delete the child entity first
            */
            String ns = getNamespaceForDataSourceType(dataSource.getType());
            this.dao.remove(new StorableKey(ns, dataSource.getPrimaryKey()));
            dao.<DataSource>remove(new StorableKey(DATA_SOURCE_NAMESPACE, dataSource.getPrimaryKey()));
        }
        return dataSource;
    }

    public DataSource addOrUpdateDataSource(Long id, DataSource dataSource) throws IOException {
        dataSource.setDataSourceId(id);
        dataSource.setTimestamp(System.currentTimeMillis());
        DataSourceSubType subType = CoreUtils.jsonToStorable(dataSource.getTypeConfig(),
                                                             getClassForDataSourceType(dataSource.getType()));
        subType.setDataSourceId(dataSource.getDataSourceId());
        this.dao.addOrUpdate(dataSource);
        this.dao.addOrUpdate(subType);
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
        df.setDataFeedId(dataFeedId);
        return this.dao.<DataFeed>get(new StorableKey(DATA_FEED_NAMESPACE, df.getPrimaryKey()));
    }

    public DataFeed addDataFeed(DataFeed feed) {
        if (feed.getDataFeedId() == null) {
            feed.setDataFeedId(this.dao.nextId(DATA_FEED_NAMESPACE));
        }
        this.dao.add(feed);
        return feed;
    }

    public DataFeed removeDataFeed(Long dataFeedId) {
        DataFeed feed = new DataFeed();
        feed.setDataFeedId(dataFeedId);
        return dao.<DataFeed>remove(new StorableKey(DATA_FEED_NAMESPACE, feed.getPrimaryKey()));
    }


    public DataFeed addOrUpdateDataFeed(Long id, DataFeed feed) {
        feed.setDataFeedId(id);
        this.dao.addOrUpdate(feed);
        return feed;
    }

    public Collection<ParserInfo> listParsers() {
        return dao.<ParserInfo>list(PARSER_INFO_NAMESPACE);
    }

    public ParserInfo getParserInfo(Long parserId) {
        ParserInfo parserInfo = new ParserInfo();
        parserInfo.setParserId(parserId);
        return dao.<ParserInfo>get(new StorableKey(PARSER_INFO_NAMESPACE, parserInfo.getPrimaryKey()));
    }

    public ParserInfo removeParser(Long parserId) {
        ParserInfo parserInfo = new ParserInfo();
        parserInfo.setParserId(parserId);
        return this.dao.<ParserInfo>remove(new StorableKey(PARSER_INFO_NAMESPACE, parserInfo.getPrimaryKey()));
    }

    public ParserInfo addParserInfo(ParserInfo parserInfo) {
        if (parserInfo.getParserId() == null) {
            parserInfo.setParserId(this.dao.nextId(PARSER_INFO_NAMESPACE));
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
        if(StringUtils.isEmpty(notifierInfo.getNotifierName())) {
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

    public Collection<DataStream> listDataStreams () {
        Collection<DataStream> dataStreams = this.dao.list(DATA_STREAM_NAMESPACE);
        return dataStreams;
    }

    public DataStream getDataStream (Long dataStreamId) {
        DataStream ds = new DataStream();
        ds.setDataStreamId(dataStreamId);
        DataStream result = this.dao.get(ds.getStorableKey());
        return result;
    }

    public DataStream addDataStream (DataStream dataStream) {
        if (dataStream.getDataStreamId() == null) {
            dataStream.setDataStreamId(this.dao.nextId(DATA_STREAM_NAMESPACE));
        }
        if (dataStream.getTimestamp() == null) {
            dataStream.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(dataStream);
        return dataStream;
    }

    public DataStream removeDataStream (Long dataStreamId) {
        DataStream dataStream = new DataStream();
        dataStream.setDataStreamId(dataStreamId);
        return dao.remove(new StorableKey(DATA_STREAM_NAMESPACE, dataStream
                .getPrimaryKey()));
    }

    public DataStream addOrUpdateDataStream (Long dataStreamId, DataStream
            dataStream) {
        dataStream.setDataStreamId(dataStreamId);
        dataStream.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(dataStream);
        return dataStream;
    }

    public DataStream validateDataStream (URL schema, Long dataStreamId)
            throws BadDataStreamLayoutException {
        DataStream ds = new DataStream();
        ds.setDataStreamId(dataStreamId);
        DataStream result = this.dao.get(ds.getStorableKey());
        boolean isValidAsPerSchema;
        if (result != null) {
            String json = result.getJson();
            try {
                // first step is to validate agains json schema provided
                isValidAsPerSchema = JsonSchemaValidator
                        .isValidJsonAsPerSchema(schema, json);
            } catch (Exception e) {
                throw new BadDataStreamLayoutException(e);
            }
            if (!isValidAsPerSchema) {
                throw new BadDataStreamLayoutException("DataStream with id "
                        + dataStreamId + " failed to validate against json "
                        + "schema");
            }
            // if first step succeeds, proceed to other validations that
            // cannot be covered using json schema
            DataStreamLayoutValidator.validateDataStreamLayout(json, this.dao);
        }
        return result;
    }

    public void deployDataStream (DataStream dataStream) throws Exception {
        this.dataStreamActions.deploy(dataStream);
        return;
    }

    public void killDataStream (DataStream dataStream) throws Exception {
        this.dataStreamActions.kill(dataStream);
        return;
    }

}
