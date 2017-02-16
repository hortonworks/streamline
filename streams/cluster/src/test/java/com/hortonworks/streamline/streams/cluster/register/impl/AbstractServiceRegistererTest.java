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
package com.hortonworks.streamline.streams.cluster.register.impl;

import com.hortonworks.streamline.storage.StorageManager;
import com.hortonworks.streamline.storage.impl.memory.InMemoryStorageManager;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import java.io.File;

public class AbstractServiceRegistererTest<T extends AbstractServiceRegisterer> {

    public static final String REGISTER_RESOURCE_DIRECTORY = "register" + File.separator + "happycase" + File.separator;
    public static final String REGISTER_BADCASE_RESOURCE_DIRECTORY = "register" + File.separator + "badcase" + File.separator;

    private final Class<T> testClazz;

    public AbstractServiceRegistererTest(Class<T> testClazz) {
        this.testClazz = testClazz;
    }

    protected StorageManager dao;
    protected EnvironmentService environmentService;

    protected Cluster getTestCluster(Long clusterId) {
        Cluster cluster = new Cluster();
        cluster.setId(clusterId);
        cluster.setName("cluster" + clusterId);
        return cluster;
    }

    protected void resetEnvironmentService() {
        dao = new InMemoryStorageManager();
        environmentService = new EnvironmentService(dao);
    }

    protected T initializeServiceRegisterer() {
        try {
            T registerer = testClazz.newInstance();
            registerer.init(environmentService);
            return registerer;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}