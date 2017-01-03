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
package org.apache.streamline.streams.runtime.splitjoin;

import org.apache.streamline.common.Constants;
import org.apache.streamline.common.util.ProxyUtil;
import org.apache.streamline.streams.common.utils.CatalogRestClient;
import org.apache.streamline.streams.layout.component.rule.action.Action;
import org.apache.streamline.streams.runtime.rule.action.AbstractActionRuntime;
import org.apache.streamline.streams.runtime.rule.action.ActionRuntime;
import org.apache.streamline.streams.runtime.rule.action.ActionRuntimeContext;
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
 * Abstract class for Split/Join {@link ActionRuntime} classes
 */
public abstract class AbstractSplitJoinActionRuntime extends AbstractActionRuntime {
    private static final Logger log = LoggerFactory.getLogger(AbstractSplitJoinActionRuntime.class);

    @Override
    public void setActionRuntimeContext(ActionRuntimeContext actionRuntimeContext) {
        super.setActionRuntimeContext(actionRuntimeContext);
        final Action action = actionRuntimeContext.getAction();
        if(actionRuntimeContext.getRule() != null &&
                action != null &&
                (action.getOutputStreams() == null || action.getOutputStreams().isEmpty())) {
            action.setOutputStreams(actionRuntimeContext.getRule().getStreams());
        }
    }

    protected String getJarPathFor(Long jarId) throws IOException {

        checkProperty(config, Constants.LOCAL_FILES_PATH);
        checkProperty(config, Constants.CATALOG_ROOT_URL);

        File filesDir = new File(config.get(Constants.LOCAL_FILES_PATH).toString());
        ensureDirExists(filesDir);
        File localFile;
        do {
            localFile = new File(filesDir, jarId + "-" + UUID.randomUUID());
        } while(localFile.exists());
        localFile.deleteOnExit();

        final CatalogRestClient catalogRestClient = new CatalogRestClient(config.get(Constants.CATALOG_ROOT_URL).toString());

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
     * @return instance of the given class loaded from the given jar or current class loader if {@code jarId} is null.
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
