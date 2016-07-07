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
package com.hortonworks.iotas.service;

import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSet;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.Device;
import com.hortonworks.iotas.catalog.FileInfo;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.catalog.Tag;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.util.FileStorage;
import com.hortonworks.iotas.storage.DataSourceSubType;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.exception.StorageException;
import com.hortonworks.iotas.storage.util.CoreUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    private static final String FILE_NAMESPACE = FileInfo.NAME_SPACE;

    private StorageManager dao;
    private FileStorage fileStorage;
    private TagService tagService;


    public CatalogService(StorageManager dao, FileStorage fileStorage) {
        this.dao = dao;
        dao.registerStorables(getStorableClasses());
        this.fileStorage = fileStorage;
        this.tagService = new CatalogTagService(dao);
    }

    public static Collection<Class<? extends Storable>> getStorableClasses() {
        InputStream resourceAsStream = CatalogService.class.getClassLoader().getResourceAsStream("storables.props");
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


    public InputStream getFileFromJarStorage(String fileName) throws IOException {
        return this.fileStorage.downloadFile(fileName);
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

    public Collection<FileInfo> listFiles() {
        return dao.list(FILE_NAMESPACE);
    }

    public Collection<FileInfo> listFiles(List<QueryParam> queryParams) {
        return dao.find(FILE_NAMESPACE, queryParams);
    }

    public FileInfo getFile(Long jarId) {
        FileInfo file = new FileInfo();
        file.setId(jarId);
        return dao.get(new StorableKey(FILE_NAMESPACE, file.getPrimaryKey()));
    }

    public FileInfo removeFile(Long fileId) {
        FileInfo file = new FileInfo();
        file.setId(fileId);
        return dao.remove(new StorableKey(FILE_NAMESPACE, file.getPrimaryKey()));
    }

    public FileInfo addOrUpdateFile(FileInfo file) {
        if (file.getId() == null) {
            file.setId(dao.nextId(FILE_NAMESPACE));
        }
        if (file.getTimestamp() == null) {
            file.setTimestamp(System.currentTimeMillis());
        }
        dao.addOrUpdate(file);

        return file;
    }
}
