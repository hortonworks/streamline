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
import com.hortonworks.iotas.common.cache.Cache;
import com.hortonworks.iotas.storage.cache.impl.GuavaCache;
import com.hortonworks.iotas.storage.cache.writer.StorageWriteThrough;
import com.hortonworks.iotas.storage.cache.writer.StorageWriter;
import com.hortonworks.iotas.common.CustomProcessorUploadHandler;
import com.hortonworks.iotas.common.FileEventHandler;
import com.hortonworks.iotas.common.errors.ConfigException;
import com.hortonworks.iotas.metrics.TimeSeriesQuerier;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.service.FileWatcher;
import com.hortonworks.iotas.storage.CacheBackedStorageManager;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.impl.jdbc.JdbcStorageManager;
import com.hortonworks.iotas.storage.impl.memory.InMemoryStorageManager;
import com.hortonworks.iotas.streams.notification.service.NotificationServiceImpl;
import com.hortonworks.iotas.topology.TopologyActions;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.topology.TopologyMetrics;
import com.hortonworks.iotas.util.FileStorage;
import com.hortonworks.iotas.common.util.ReflectionHelper;
import com.hortonworks.iotas.webservice.catalog.ClusterCatalogResource;
import com.hortonworks.iotas.webservice.catalog.ComponentCatalogResource;
import com.hortonworks.iotas.webservice.catalog.DataSourceCatalogResource;
import com.hortonworks.iotas.webservice.catalog.DataSourceFacade;
import com.hortonworks.iotas.webservice.catalog.DataSourceWithDataFeedCatalogResource;
import com.hortonworks.iotas.webservice.catalog.FeedCatalogResource;
import com.hortonworks.iotas.webservice.catalog.FileCatalogResource;
import com.hortonworks.iotas.webservice.catalog.NotifierInfoCatalogResource;
import com.hortonworks.iotas.webservice.catalog.ParserInfoCatalogResource;
import com.hortonworks.iotas.webservice.catalog.RuleCatalogResource;
import com.hortonworks.iotas.webservice.catalog.TopologyStreamCatalogResource;
import com.hortonworks.iotas.webservice.catalog.TagCatalogResource;
import com.hortonworks.iotas.webservice.catalog.TopologyCatalogResource;
import com.hortonworks.iotas.webservice.catalog.TopologyEdgeCatalogResource;
import com.hortonworks.iotas.webservice.catalog.TopologyEditorMetadataResource;
import com.hortonworks.iotas.webservice.catalog.TopologyProcessorCatalogResource;
import com.hortonworks.iotas.webservice.catalog.TopologySinkCatalogResource;
import com.hortonworks.iotas.webservice.catalog.TopologySourceCatalogResource;
import com.hortonworks.iotas.webservice.catalog.UDFCatalogResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // kafka producer shouldn't be starting as part of REST api.
        // KafkaProducerManager kafkaProducerManager = new KafkaProducerManager(iotasConfiguration);
        // environment.lifecycle().manage(kafkaProducerManager);

        // ZkClient zkClient = new ZkClient(iotasConfiguration.getZookeeperHost());

        // final FeedResource feedResource = new FeedResource(kafkaProducerManager.getProducer(), zkClient);
        // environment.jersey().register(feedResource);

        registerResources(iotasConfiguration, environment);
    }

    private StorageManager getCacheBackedDao(IotasConfiguration iotasConfiguration) {
        StorageProviderConfiguration storageProviderConfiguration = iotasConfiguration.getStorageProviderConfiguration();
        final String providerType = storageProviderConfiguration.getType();
        final StorageManager dao = providerType.equalsIgnoreCase(JDBC) ?
                JdbcStorageManager.createStorageManager(storageProviderConfiguration.getProperties()) : new InMemoryStorageManager();
        final CacheBuilder cacheBuilder = getGuavaCacheBuilder();
        final Cache<StorableKey, Storable> cache = getCache(dao, cacheBuilder);
        final StorageWriter storageWriter = getStorageWriter(dao);

        return doGetCacheBackedDao(cache, storageWriter);
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

    private TopologyActions getTopologyActionsImpl (IotasConfiguration configuration) {
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
        Map<String, String> conf = new HashMap<>();
        conf.put(TopologyLayoutConstants.STORM_JAR_LOCATION_KEY, jar);
        conf.put(TopologyLayoutConstants.YAML_KEY_CATALOG_ROOT_URL, configuration.getCatalogRootUrl());
        conf.put(TopologyLayoutConstants.STORM_HOME_DIR, configuration.getStormHomeDir());
        conf.put(TopologyLayoutConstants.JAVA_JAR_COMMAND, configuration.getJavaJarCommand());
        topologyActions.init(conf);
        return topologyActions;
    }

    private TopologyMetrics getTopologyMetricsImpl(IotasConfiguration configuration) throws ConfigException {
        String className = configuration.getTopologyMetricsImpl();
        TopologyMetrics topologyMetrics;
        try {
            topologyMetrics = ReflectionHelper.newInstance(className);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        //pass any config info that might be needed in the constructor as a map
        Map<String, String> conf = new HashMap<>();
        conf.put(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY, configuration.getStormApiRootUrl());
        topologyMetrics.init(conf);

        TimeSeriesQuerier timeSeriesQuerier = getTimeSeriesQuerier(configuration);
        if (timeSeriesQuerier != null) {
            topologyMetrics.setTimeSeriesQuerier(timeSeriesQuerier);
        }

        return topologyMetrics;
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

    private void registerResources(IotasConfiguration iotasConfiguration, Environment environment) throws ConfigException {
        StorageManager storageManager = getCacheBackedDao(iotasConfiguration);
        TopologyActions topologyActions = getTopologyActionsImpl(iotasConfiguration);
        TopologyMetrics topologyMetrics = getTopologyMetricsImpl(iotasConfiguration);
        FileStorage fileStorage = this.getJarStorage(iotasConfiguration);

        final CatalogService catalogService = new CatalogService(storageManager, topologyActions, topologyMetrics, fileStorage);
        final FeedCatalogResource feedResource = new FeedCatalogResource(catalogService);
        final ParserInfoCatalogResource parserResource = new ParserInfoCatalogResource(catalogService);
        final DataSourceCatalogResource dataSourceResource = new DataSourceCatalogResource(catalogService);
        final DataSourceWithDataFeedCatalogResource dataSourceWithDataFeedCatalogResource =
                new DataSourceWithDataFeedCatalogResource(new DataSourceFacade(catalogService));
        final TopologyCatalogResource topologyCatalogResource = new TopologyCatalogResource(catalogService);
        final MetricsResource metricsResource = new MetricsResource(catalogService);
        final TopologyStreamCatalogResource topologyStreamCatalogResource = new TopologyStreamCatalogResource(catalogService);

        // cluster related
        final ClusterCatalogResource clusterCatalogResource = new ClusterCatalogResource(catalogService, fileStorage);
        final ComponentCatalogResource componentCatalogResource = new ComponentCatalogResource(catalogService);
        final TopologyEditorMetadataResource topologyEditorMetadataResource = new TopologyEditorMetadataResource(catalogService);
        final TagCatalogResource tagCatalogResource = new TagCatalogResource(catalogService);
        final FileCatalogResource fileCatalogResource = new FileCatalogResource(catalogService);

        // topology related
        final TopologySourceCatalogResource topologySourceCatalogResource = new TopologySourceCatalogResource(catalogService);
        final TopologySinkCatalogResource topologySinkCatalogResource = new TopologySinkCatalogResource(catalogService);
        final TopologyProcessorCatalogResource topologyProcessorCatalogResource = new TopologyProcessorCatalogResource(catalogService);
        final TopologyEdgeCatalogResource topologyEdgeCatalogResource = new TopologyEdgeCatalogResource(catalogService);
        final RuleCatalogResource ruleCatalogResource = new RuleCatalogResource(catalogService);

        // UDF catalaog resource
        final UDFCatalogResource udfCatalogResource = new UDFCatalogResource(catalogService, fileStorage);

        List<Object> resources = Lists.newArrayList(feedResource, parserResource, dataSourceResource, dataSourceWithDataFeedCatalogResource,
                topologyCatalogResource, clusterCatalogResource, componentCatalogResource,
                topologyEditorMetadataResource, tagCatalogResource, fileCatalogResource, metricsResource, topologyStreamCatalogResource,
                topologySourceCatalogResource, topologySinkCatalogResource, topologyProcessorCatalogResource, topologyEdgeCatalogResource,
                ruleCatalogResource, udfCatalogResource);
        if (!iotasConfiguration.isNotificationsRestDisabled()) {
            resources.add(new NotifierInfoCatalogResource(catalogService));
            resources.add(new NotificationsResource(new NotificationServiceImpl()));
        }

        for(Object resource : resources) {
            environment.jersey().register(resource);
        }

        environment.jersey().register(MultiPartFeature.class);
        watchFiles(iotasConfiguration, catalogService);
    }

    private TimeSeriesQuerier getTimeSeriesQuerier(IotasConfiguration iotasConfiguration) {
        if (iotasConfiguration.getTimeSeriesDBConfiguration() == null) {
            return null;
        }

        try {
            TimeSeriesQuerier timeSeriesQuerier = ReflectionHelper.newInstance(iotasConfiguration.getTimeSeriesDBConfiguration().getClassName());
            timeSeriesQuerier.init(iotasConfiguration.getTimeSeriesDBConfiguration().getProperties());
            return timeSeriesQuerier;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void watchFiles (IotasConfiguration iotasConfiguration, CatalogService catalogService) {
        if (iotasConfiguration.getCustomProcessorWatchPath() == null || iotasConfiguration.getCustomProcessorUploadFailPath() == null || iotasConfiguration
                .getCustomProcessorUploadSuccessPath() == null) {
            return;
        }
        FileEventHandler customProcessorUploadHandler = new CustomProcessorUploadHandler(iotasConfiguration.getCustomProcessorWatchPath(), iotasConfiguration
                .getCustomProcessorUploadFailPath(), iotasConfiguration.getCustomProcessorUploadSuccessPath(), catalogService);
        List<FileEventHandler> fileEventHandlers = new ArrayList<>();
        fileEventHandlers.add(customProcessorUploadHandler);
        final FileWatcher fileWatcher = new FileWatcher(fileEventHandlers);
        fileWatcher.register();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (fileWatcher.processEvents()) {

                }
            }
        });
        thread.start();
    }

}
