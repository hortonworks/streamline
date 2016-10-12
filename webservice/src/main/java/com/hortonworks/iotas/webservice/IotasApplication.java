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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.iotas.webservice;

import com.google.common.cache.CacheBuilder;
import com.hortonworks.iotas.cache.Cache;
import com.hortonworks.iotas.common.Constants;
import com.hortonworks.iotas.common.ModuleRegistration;
import com.hortonworks.iotas.common.util.FileStorage;
import com.hortonworks.iotas.common.util.ReflectionHelper;
import com.hortonworks.iotas.storage.CacheBackedStorageManager;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.StorageManagerAware;
import com.hortonworks.iotas.storage.cache.impl.GuavaCache;
import com.hortonworks.iotas.storage.cache.writer.StorageWriteThrough;
import com.hortonworks.iotas.storage.cache.writer.StorageWriter;
import com.hortonworks.iotas.streams.exception.ConfigException;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IotasApplication extends Application<IotasConfiguration> {

    private static final Logger log = LoggerFactory.getLogger(IotasApplication.class);
    private static final String JDBC = "jdbc";

    public static void main(String[] args) throws Exception {
        new IotasApplication().run(args);
    }

    @Override
    public String getName() {
        return "IoTaS Web Service";
    }

    @Override
    public void initialize(Bootstrap<IotasConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/ui", "index.html", "static"));
        super.initialize(bootstrap);
    }

    @Override
    public void run(IotasConfiguration iotasConfiguration, Environment environment) throws Exception {
        registerResources(iotasConfiguration, environment);
    }

    private StorageManager getCacheBackedDao(IotasConfiguration iotasConfiguration) {
        StorageProviderConfiguration storageProviderConfiguration = iotasConfiguration.getStorageProviderConfiguration();
        final StorageManager dao = getStorageManager(storageProviderConfiguration);
        final CacheBuilder cacheBuilder = getGuavaCacheBuilder();
        final Cache<StorableKey, Storable> cache = getCache(dao, cacheBuilder);
        final StorageWriter storageWriter = getStorageWriter(dao);

        return doGetCacheBackedDao(cache, storageWriter);
    }

    private StorageManager getStorageManager(StorageProviderConfiguration storageProviderConfiguration) {
        final String providerClass = storageProviderConfiguration.getProviderClass();
        StorageManager storageManager = null;
        try {
            storageManager = (StorageManager) Class.forName(providerClass).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        storageManager.init(storageProviderConfiguration.getProperties());

        return storageManager;
    }

    private StorageWriter getStorageWriter(StorageManager dao) {
        return new StorageWriteThrough(dao);
    }

    private StorageManager doGetCacheBackedDao(Cache<StorableKey, Storable> cache, StorageWriter writer) {
        return new CacheBackedStorageManager(cache, writer);
    }

    private Cache<StorableKey, Storable> getCache(StorageManager dao, CacheBuilder guavaCacheBuilder) {
        return new GuavaCache(dao, guavaCacheBuilder);
    }

    private CacheBuilder getGuavaCacheBuilder() {
        final long maxSize = 1000;
        return CacheBuilder.newBuilder().maximumSize(maxSize);
    }

    private FileStorage getJarStorage (IotasConfiguration configuration) {
        FileStorage fileStorage = null;
        try {
            fileStorage = ReflectionHelper.newInstance(configuration.getFileStorageConfiguration().getClassName());
            fileStorage.init(configuration.getFileStorageConfiguration().getProperties());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return fileStorage;
    }

    private void registerResources(IotasConfiguration iotasConfiguration, Environment environment) throws ConfigException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        StorageManager storageManager = getCacheBackedDao(iotasConfiguration);
        FileStorage fileStorage = this.getJarStorage(iotasConfiguration);
        int appPort = ((HttpConnectorFactory) ((DefaultServerFactory) iotasConfiguration.getServerFactory()).getApplicationConnectors().get(0)).getPort();
        String catalogRootUrl = iotasConfiguration.getCatalogRootUrl().replaceFirst("8080", appPort +"");
        List<ModuleConfiguration> modules = iotasConfiguration.getModules();
        List<Object> resourcesToRegister = new ArrayList<>();
        for (ModuleConfiguration moduleConfiguration: modules) {
            ModuleRegistration moduleRegistration = (ModuleRegistration) Class.forName(moduleConfiguration.getClassName()).newInstance();
            if (moduleConfiguration.getConfig() == null) {
                moduleConfiguration.setConfig(new HashMap<String, Object>());
            }
            moduleConfiguration.getConfig().put(Constants.CONFIG_TIME_SERIES_DB, iotasConfiguration.getTimeSeriesDBConfiguration());
            moduleConfiguration.getConfig().put(Constants.CONFIG_CATALOG_ROOT_URL, catalogRootUrl);
            moduleRegistration.init(moduleConfiguration.getConfig(), fileStorage);
            StorageManagerAware storageManagerAware = (StorageManagerAware) moduleRegistration;
            storageManagerAware.setStorageManager(storageManager);
            resourcesToRegister.addAll(moduleRegistration.getResources());
        }
        for(Object resource : resourcesToRegister) {
            environment.jersey().register(resource);
        }
        environment.jersey().register(MultiPartFeature.class);
    }
}
