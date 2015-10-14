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
import com.google.common.collect.Lists;
import com.hortonworks.iotas.cache.Cache;
import com.hortonworks.iotas.cache.impl.GuavaCache;
import com.hortonworks.iotas.cache.writer.StorageWriteThrough;
import com.hortonworks.iotas.cache.writer.StorageWriter;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.storage.CacheBackedStorageManager;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.impl.memory.InMemoryStorageManager;
import com.hortonworks.iotas.webservice.catalog.ClusterCatalogResource;
import com.hortonworks.iotas.webservice.catalog.ComponentCatalogResource;
import com.hortonworks.iotas.webservice.catalog.DataSourceCatalogResource;
import com.hortonworks.iotas.webservice.catalog.DataSourceWithDataFeedCatalogResource;
import com.hortonworks.iotas.webservice.catalog.FeedCatalogResource;
import com.hortonworks.iotas.webservice.catalog.NotifierInfoCatalogResource;
import com.hortonworks.iotas.webservice.catalog.ParserInfoCatalogResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.util.List;

public class IotasApplication extends Application<IotasConfiguration> {

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
        // kafka producer shouldn't be starting as part of REST api.
        // KafkaProducerManager kafkaProducerManager = new KafkaProducerManager(iotasConfiguration);
        // environment.lifecycle().manage(kafkaProducerManager);

        // ZkClient zkClient = new ZkClient(iotasConfiguration.getZookeeperHost());

        // final FeedResource feedResource = new FeedResource(kafkaProducerManager.getProducer(), zkClient);
        // environment.jersey().register(feedResource);

        // TODO we should load the implementation based on configuration
        final StorageManager cacheBackedDao = getCacheBackedDao();

        registerResources(iotasConfiguration, environment, cacheBackedDao);
    }

    private StorageManager getCacheBackedDao() {
        final InMemoryStorageManager dao = new InMemoryStorageManager();
        final CacheBuilder cacheBuilder = getGuavaCacheBuilder();
        final Cache<StorableKey, Storable> cache = getCache(dao, cacheBuilder);
        final StorageWriter storageWriter = getStorageWriter(dao);

        return doGetCacheBackedDao(cache, storageWriter);
    }

    private StorageWriter getStorageWriter(StorageManager dao) {
        return new StorageWriteThrough(dao);
    }

    private StorageManager doGetCacheBackedDao(Cache<StorableKey, Storable> cache, StorageWriter writer) {
//        return new InMemoryStorageManager();      // for quick debug purposes
        return new CacheBackedStorageManager(cache, writer);
    }

    private Cache<StorableKey, Storable> getCache(StorageManager dao, CacheBuilder guavaCacheBuilder) {
        return new GuavaCache(dao, guavaCacheBuilder);
    }

    private CacheBuilder getGuavaCacheBuilder() {
        final long maxSize = 1000;
        return  CacheBuilder.newBuilder().maximumSize(maxSize);
    }

    private void registerResources(IotasConfiguration iotasConfiguration, Environment environment, StorageManager manager) {
        final CatalogService catalogService = new CatalogService(getCacheBackedDao());
        final FeedCatalogResource feedResource = new FeedCatalogResource(catalogService);
        final ParserInfoCatalogResource parserResource = new ParserInfoCatalogResource(catalogService, iotasConfiguration);
        final DataSourceCatalogResource dataSourceResource = new DataSourceCatalogResource(catalogService);
        final DataSourceWithDataFeedCatalogResource dataSourceWithDataFeedCatalogResource = new DataSourceWithDataFeedCatalogResource(catalogService);

        // cluster related
        final ClusterCatalogResource clusterCatalogResource = new ClusterCatalogResource(catalogService);
        final ComponentCatalogResource componentCatalogResource = new ComponentCatalogResource(catalogService);

        // notifier
        final NotifierInfoCatalogResource notifierInfoCatalogResource = new NotifierInfoCatalogResource(catalogService);

        List<Object> resources = Lists.newArrayList(feedResource, parserResource, dataSourceResource,
                                                    clusterCatalogResource, componentCatalogResource,
                                                    dataSourceWithDataFeedCatalogResource, notifierInfoCatalogResource);
        for(Object resource : resources) {
            environment.jersey().register(resource);
        }
        environment.jersey().register(MultiPartFeature.class);
    }
}
