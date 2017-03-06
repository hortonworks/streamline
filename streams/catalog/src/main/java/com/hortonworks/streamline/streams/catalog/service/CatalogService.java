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

import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.common.util.FileStorage;
import com.hortonworks.streamline.registries.tag.client.TagClient;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.StorageManager;
import com.hortonworks.streamline.storage.util.StorageUtils;
import com.hortonworks.streamline.streams.catalog.File;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
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
    private static final String FILE_NAMESPACE = File.NAME_SPACE;

    private final StorageManager dao;
    private final FileStorage fileStorage;


    public CatalogService(StorageManager dao, FileStorage fileStorage, TagClient tagClient) {
        this.dao = dao;
        this.fileStorage = fileStorage;
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

    public Collection<File> listFiles() {
        return dao.list(FILE_NAMESPACE);
    }

    public Collection<File> listFiles(List<QueryParam> queryParams) {
        return dao.find(FILE_NAMESPACE, queryParams);
    }

    public File getFile(Long jarId) {
        File file = new File();
        file.setId(jarId);
        return dao.get(new StorableKey(FILE_NAMESPACE, file.getPrimaryKey()));
    }

    public File removeFile(Long fileId) {
        File file = new File();
        file.setId(fileId);
        return dao.remove(new StorableKey(FILE_NAMESPACE, file.getPrimaryKey()));
    }

    // handle this check at application layer since in-memory storage etc does not contain unique key constraint
    private void validateFileInfo(File file) {
        StorageUtils.ensureUnique(file, this::listFiles, QueryParam.params("name", file.getName()));
    }

    public File addFile(File file) {
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

    public File addOrUpdateFile(Long fileId, File file) {
        file.setId(fileId);
        file.setTimestamp(System.currentTimeMillis());
        validateFileInfo(file);
        this.dao.addOrUpdate(file);
        return file;
    }


}
