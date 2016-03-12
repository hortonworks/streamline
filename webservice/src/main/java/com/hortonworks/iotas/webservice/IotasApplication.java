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
import com.hortonworks.iotas.notification.service.NotificationServiceImpl;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.storage.CacheBackedStorageManager;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.impl.jdbc.JdbcStorageManager;
import com.hortonworks.iotas.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.iotas.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.PhoenixClient;
import com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.factory.PhoenixExecutor;
import com.hortonworks.iotas.storage.impl.memory.InMemoryStorageManager;
import com.hortonworks.iotas.topology.TopologyActions;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.util.ReflectionHelper;
import com.hortonworks.iotas.webservice.catalog.*;
import com.zaxxer.hikari.HikariConfig;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IotasApplication extends Application<IotasConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(IotasApplication.class);

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
        final StorageManager cacheBackedDao = getCacheBackedDao(iotasConfiguration);

        registerResources(iotasConfiguration, environment, cacheBackedDao);
    }

    private StorageManager getCacheBackedDao(IotasConfiguration iotasConfiguration) {
        //TODO storage provider configuration should come from IotasConfiguration.
        String providerType = iotasConfiguration.getStorageProvier();
        LOG.info("################################### providerType = " + providerType);
        final StorageManager dao = providerType.equalsIgnoreCase("phoenix") ? createPhoenixStorageManager(iotasConfiguration) : new InMemoryStorageManager();
        final CacheBuilder cacheBuilder = getGuavaCacheBuilder();
        final Cache<StorableKey, Storable> cache = getCache(dao, cacheBuilder);
        final StorageWriter storageWriter = getStorageWriter(dao);

        return doGetCacheBackedDao(cache, storageWriter);
    }

    private StorageManager createPhoenixStorageManager(IotasConfiguration iotasConfig) {
        //TODO takes config from iotas configuration for the below information and refactor accordingly.
        HikariConfig hikariConfig = new HikariConfig();
        try {
            Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
            String jdbcUrl = "jdbc:phoenix:" + iotasConfig.getPhoenixZookeeperHosts();
            hikariConfig.setJdbcUrl(jdbcUrl);
            PhoenixClient phoenixClient = new PhoenixClient(jdbcUrl);
            LOG.info("creating tables");
            String createPath = "phoenix/create_tables.sql";
            phoenixClient.runScript(createPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new JdbcStorageManager(new PhoenixExecutor(new ExecutionConfig(-1), new HikariCPConnectionBuilder(hikariConfig)));
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

    private TopologyActions getTopologyActionsImpl (IotasConfiguration
                                                           configuration) {
        String className = configuration.getTopologyActionsImpl();
        // Note that iotasStormJar value needs to be changed in iotas.yaml
        // based on the location of the storm module jar of iotas project.
        // Reason for doing it this way is storm ui right now does not
        // support submitting a jar because of security vulnerability. Hence
        // for now, we just run the storm jar command in a shell on machine
        // where IoTaS is deployed. It is run in StormTopologyActionsImpl
        // class. This also adds a security vulnerability. We will change
        // this later on using our cluster entity when its handled right in
        // storm.
        String jar = configuration.getIotasStormJar();
        TopologyActions topologyActions;
        try {
            topologyActions = ReflectionHelper.newInstance(className);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        //pass any config info that might be needed in the constructor as a map
        Map conf = new HashMap();
        conf.put(TopologyLayoutConstants.STORM_JAR_LOCATION_KEY, jar);
        conf.put("catalog.root.url", configuration.getCatalogRootUrl());
        conf.put(TopologyLayoutConstants.STORM_HOME_DIR, configuration.getStormHomeDir());
        topologyActions.init(conf);
        return topologyActions;
    }


    private void registerResources(IotasConfiguration iotasConfiguration, Environment environment, StorageManager manager) {
        StorageManager storageManager = getCacheBackedDao(iotasConfiguration);
        TopologyActions topologyActions = getTopologyActionsImpl
                (iotasConfiguration);
        final CatalogService catalogService = new CatalogService
                (storageManager, topologyActions);
        final FeedCatalogResource feedResource = new FeedCatalogResource(catalogService);
        final ParserInfoCatalogResource parserResource = new ParserInfoCatalogResource(catalogService, iotasConfiguration);
        final DataSourceCatalogResource dataSourceResource = new DataSourceCatalogResource(catalogService);
        final DataSourceWithDataFeedCatalogResource dataSourceWithDataFeedCatalogResource =
                new DataSourceWithDataFeedCatalogResource(new DataSourceFacade(catalogService));
        final TopologyCatalogResource topologyCatalogResource = new TopologyCatalogResource(catalogService);

        // cluster related
        final ClusterCatalogResource clusterCatalogResource = new ClusterCatalogResource(catalogService);
        final ComponentCatalogResource componentCatalogResource = new ComponentCatalogResource(catalogService);
        final TopologyEditorMetadataResource topologyEditorMetadataResource = new TopologyEditorMetadataResource(catalogService);
        List<Object> resources = Lists.newArrayList(feedResource, parserResource, dataSourceResource, dataSourceWithDataFeedCatalogResource,
                                                    topologyCatalogResource, clusterCatalogResource, componentCatalogResource, topologyEditorMetadataResource);
        if (!iotasConfiguration.isNotificationsRestDisabled()) {
            resources.add(new NotifierInfoCatalogResource(catalogService));
            resources.add(new NotificationsResource(new NotificationServiceImpl()));
        }
        for(Object resource : resources) {
            environment.jersey().register(resource);
        }
        environment.jersey().register(MultiPartFeature.class);
    }
}
