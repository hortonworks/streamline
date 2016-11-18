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

import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.FileStorage;
import org.apache.streamline.registries.parser.ParserInfo;
import org.apache.streamline.registries.parser.client.ParserClient;
import org.apache.streamline.registries.tag.client.TagClient;
import org.apache.streamline.storage.Storable;
import org.apache.streamline.storage.StorableKey;
import org.apache.streamline.storage.StorageManager;
import org.apache.streamline.storage.util.StorageUtils;
import org.apache.streamline.streams.catalog.FileInfo;
import org.apache.commons.io.IOUtils;
import org.apache.streamline.common.exception.DuplicateEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


/**
 * A service layer where we could put our business logic.
 * Right now this exists as a very thin layer between the DAO and
 * the REST controllers.
 */
public class CatalogService {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogService.class);

    // TODO: the namespace and Id generation logic should be moved inside DAO
    private static final String FILE_NAMESPACE = FileInfo.NAME_SPACE;

    private final StorageManager dao;
    private final FileStorage fileStorage;
    private final ParserClient parserClient;


    public CatalogService(StorageManager dao, FileStorage fileStorage, TagClient tagClient, ParserClient parserClient) {
        this.dao = dao;
        dao.registerStorables(getStorableClasses());
        this.fileStorage = fileStorage;
        this.parserClient = parserClient;
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

    public String uploadFileToStorage(InputStream inputStream, String jarFileName) throws IOException {
        return fileStorage.uploadFile(inputStream, jarFileName);
    }

    public InputStream downloadFileFromStorage(String jarName) throws IOException {
        return fileStorage.downloadFile(jarName);
    }

    public boolean deleteFileFromStorage(String jarName) throws IOException {
        return fileStorage.deleteFile(jarName);
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

    // handle this check at application layer since in-memory storage etc does not contain unique key constraint
    private void validateFileInfo(FileInfo fileInfo) {
        StorageUtils.ensureUniqueName(fileInfo, this::listFiles, fileInfo.getName());
    }

    public FileInfo addFile(FileInfo file) {
        if (file.getId() == null) {
            file.setId(dao.nextId(FILE_NAMESPACE));
        }
        if (file.getTimestamp() == null) {
            file.setTimestamp(System.currentTimeMillis());
        }
        validateFileInfo(file);
        dao.addOrUpdate(file);
        return file;
    }

    public FileInfo addOrUpdateFile(Long fileId, FileInfo file) {
        file.setId(fileId);
        file.setTimestamp(System.currentTimeMillis());
        validateFileInfo(file);
        this.dao.addOrUpdate(file);
        return file;
    }

    public ParserInfo getParserInfo (Long parserId) {
        return parserClient.getParserInfo(parserId);
    }
}
