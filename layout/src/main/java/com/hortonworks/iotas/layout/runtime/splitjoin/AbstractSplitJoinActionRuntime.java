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
package com.hortonworks.iotas.layout.runtime.splitjoin;

import com.hortonworks.iotas.client.CatalogRestClient;
import com.hortonworks.iotas.layout.runtime.rule.action.AbstractActionRuntime;
import com.hortonworks.iotas.util.CoreUtils;
import com.hortonworks.iotas.util.ProxyUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract class for Split/Join {@link com.hortonworks.iotas.layout.runtime.rule.action.ActionRuntime} classes
 */
public abstract class AbstractSplitJoinActionRuntime extends AbstractActionRuntime {
    private static final Logger log = LoggerFactory.getLogger(AbstractSplitJoinActionRuntime.class);

    protected String getJarPathFor(Long jarId) throws IOException {

        checkProperty(config, CoreUtils.LOCAL_FILES_PATH);
        checkProperty(config, CoreUtils.CATALOG_ROOT_URL);

        File filesDir = new File(config.get(CoreUtils.LOCAL_FILES_PATH).toString());
        ensureDirExists(filesDir);
        File localFile = null;
        do {
            localFile = new File(filesDir, jarId + "-" + UUID.randomUUID());
        } while(localFile.exists());
        localFile.deleteOnExit();

        final CatalogRestClient catalogRestClient = new CatalogRestClient(config.get(CoreUtils.CATALOG_ROOT_URL).toString());

        try(final FileOutputStream output = new FileOutputStream(localFile);
            final InputStream inputStream = catalogRestClient.getFile(jarId)) {
            IOUtils.copy(inputStream, output);
        }

        return localFile.getAbsolutePath();
    }

    protected void checkProperty(Map<String, Object> config, String propertyName) {
        if(!config.containsKey(propertyName)) {
            String errMsg = String.format("Config does not contain property with key [%s]", propertyName);
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    private void ensureDirExists(File filesDir) {
        if(!filesDir.exists()) {
            if(!filesDir.mkdirs()) {
                throw new RuntimeException("Given directory ["+filesDir+"]  could not be created.");
            }
        } else if (!filesDir.isDirectory()) {
            throw new RuntimeException("Given directory ["+filesDir+"]  is not a directory.");
        }
    }

    /**
     * Creates an instance of the given class which is loaded from the given jar or current class loader if {@code jarId} is null.
     *
     * @param <T> type of the object to be created
     * @param jarId id of the jar resource
     * @param fqcn FullyQualifiedClassName of the object to be created
     * @param klass Class instance of the object to be created
     * @return
     */
    protected  <T> T getInstance(Long jarId, String fqcn, Class<T> klass) {
        T instance = null;
        if (fqcn != null) {
            try {
                if (jarId != null) {
                    ProxyUtil<T> proxyUtil = new ProxyUtil<>(klass, AbstractSplitJoinActionRuntime.class.getClassLoader());
                    String jarPath = getJarPathFor(jarId);
                    instance = proxyUtil.loadClassFromJar(jarPath, fqcn);
                } else {
                    // FQCN is given but no jarId then that class is assumed to be accessible from current class loader.
                    instance = (T) Class.forName(fqcn, true, Thread.currentThread().getContextClassLoader()).newInstance();
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return instance;
    }
}
