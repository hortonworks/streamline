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
package com.hortonworks.streamline.streams.service;

import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import com.hortonworks.streamline.common.Constants;
import com.hortonworks.streamline.common.FileEventHandler;
import com.hortonworks.streamline.common.FileWatcher;
import com.hortonworks.streamline.common.ModuleRegistration;
import com.hortonworks.streamline.common.util.FileStorage;
import com.hortonworks.streamline.registries.model.client.MLModelRegistryClient;
import com.hortonworks.streamline.registries.tag.client.TagClient;
import com.hortonworks.streamline.storage.StorageManager;
import com.hortonworks.streamline.storage.StorageManagerAware;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.TopologyVersion;
import com.hortonworks.streamline.streams.catalog.service.CatalogService;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyMetricsService;
import com.hortonworks.streamline.streams.notification.service.NotificationServiceImpl;
import com.hortonworks.streamline.streams.service.metadata.HBaseMetadataResource;
import com.hortonworks.streamline.streams.service.metadata.HiveMetadataResource;
import com.hortonworks.streamline.streams.service.metadata.KafkaMetadataResource;
import com.hortonworks.streamline.streams.service.metadata.StormMetadataResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation for the streams module for registration with web service module
 */
public class StreamsModule implements ModuleRegistration, StorageManagerAware {
    private FileStorage fileStorage;
    private Map<String, Object> config;
    private StorageManager storageManager;

    @Override
    public void init(Map<String, Object> config, FileStorage fileStorage) {
        this.config = config;
        this.fileStorage = fileStorage;
    }

    @Override
    public List<Object> getResources() {
        List<Object> result = new ArrayList<>();
        String catalogRootUrl = (String) config.get(Constants.CONFIG_CATALOG_ROOT_URL);
        MLModelRegistryClient modelRegistryClient = new MLModelRegistryClient(catalogRootUrl);
        final StreamCatalogService streamcatalogService = new StreamCatalogService(storageManager, fileStorage, modelRegistryClient);
        final EnvironmentService environmentService = new EnvironmentService(storageManager);
        TagClient tagClient = new TagClient(catalogRootUrl);
        final CatalogService catalogService = new CatalogService(storageManager, fileStorage, tagClient);
        final TopologyActionsService topologyActionsService = new TopologyActionsService(streamcatalogService,
                environmentService, fileStorage, modelRegistryClient, config);
        final TopologyMetricsService topologyMetricsService = new TopologyMetricsService(environmentService);

        environmentService.addNamespaceAwareContainer(topologyActionsService);
        environmentService.addNamespaceAwareContainer(topologyMetricsService);

        result.add(new MetricsResource(streamcatalogService, topologyMetricsService));
        result.addAll(getClusterRelatedResources(environmentService));
        result.add(new FileCatalogResource(catalogService));
        result.addAll(getTopologyRelatedResources(streamcatalogService, environmentService, topologyActionsService,
                topologyMetricsService));
        result.add(new RuleCatalogResource(streamcatalogService));
        result.add(new BranchRuleCatalogResource(streamcatalogService));
        result.add(new UDFCatalogResource(streamcatalogService, fileStorage));
        result.addAll(getNotificationsRelatedResources(streamcatalogService));
        result.add(new WindowCatalogResource(streamcatalogService));
        result.add(new SchemaResource(createSchemaRegistryClient()));
        result.addAll(getServiceMetadataResources(environmentService));
        result.add(new NamespaceCatalogResource(streamcatalogService, topologyActionsService, environmentService));
        watchFiles(streamcatalogService);
        setupPlaceholderEntities(streamcatalogService);
        return result;
    }

    private SchemaRegistryClient createSchemaRegistryClient() {
        Map<String, ?> conf = Collections.singletonMap(SchemaRegistryClient.Configuration.SCHEMA_REGISTRY_URL.name(),
                                                       config.get("schemaRegistryUrl"));
        return new SchemaRegistryClient(conf);
    }

    @Override
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    private List<Object> getTopologyRelatedResources(StreamCatalogService streamcatalogService, EnvironmentService environmentService,
                                                     TopologyActionsService actionsService, TopologyMetricsService metricsService) {
        List<Object> result = new ArrayList<>();
        final TopologyCatalogResource topologyCatalogResource = new TopologyCatalogResource(streamcatalogService,
                environmentService, actionsService, metricsService);
        result.add(topologyCatalogResource);
        final TopologyComponentBundleResource topologyComponentResource = new TopologyComponentBundleResource(streamcatalogService, environmentService);
        result.add(topologyComponentResource);
        final TopologyStreamCatalogResource topologyStreamCatalogResource = new TopologyStreamCatalogResource(streamcatalogService);
        result.add(topologyStreamCatalogResource);
        final TopologyEditorMetadataResource topologyEditorMetadataResource = new TopologyEditorMetadataResource(streamcatalogService);
        result.add(topologyEditorMetadataResource);
        final TopologySourceCatalogResource topologySourceCatalogResource = new TopologySourceCatalogResource(streamcatalogService);
        result.add(topologySourceCatalogResource);
        final TopologySinkCatalogResource topologySinkCatalogResource = new TopologySinkCatalogResource(streamcatalogService);
        result.add(topologySinkCatalogResource);
        final TopologyProcessorCatalogResource topologyProcessorCatalogResource = new TopologyProcessorCatalogResource(streamcatalogService);
        result.add(topologyProcessorCatalogResource);
        final TopologyEdgeCatalogResource topologyEdgeCatalogResource = new TopologyEdgeCatalogResource(streamcatalogService);
        result.add(topologyEdgeCatalogResource);
        return result;
    }

    private List<Object> getClusterRelatedResources(EnvironmentService environmentService) {
        List<Object> result = new ArrayList<>();
        final ClusterCatalogResource clusterCatalogResource = new ClusterCatalogResource(environmentService);
        result.add(clusterCatalogResource);
        final ServiceCatalogResource serviceCatalogResource = new ServiceCatalogResource(environmentService);
        result.add(serviceCatalogResource);
        final ServiceConfigurationCatalogResource serviceConfigurationCatalogResource = new ServiceConfigurationCatalogResource(environmentService);
        result.add(serviceConfigurationCatalogResource);
        final ComponentCatalogResource componentCatalogResource = new ComponentCatalogResource(environmentService);
        result.add(componentCatalogResource);
        final ServiceBundleResource serviceBundleResource = new ServiceBundleResource(environmentService);
        result.add(serviceBundleResource);
        return result;
    }

    private List<Object> getServiceMetadataResources(EnvironmentService environmentService) {
        final List<Object> result = new ArrayList<>();
        final KafkaMetadataResource kafkaMetadataResource = new KafkaMetadataResource(environmentService);
        result.add(kafkaMetadataResource);
        final StormMetadataResource stormMetadataResource = new StormMetadataResource(environmentService);
        result.add(stormMetadataResource);
        final HiveMetadataResource hiveMetadataResource = new HiveMetadataResource(environmentService);
        result.add(hiveMetadataResource);
        final HBaseMetadataResource hbaseMetadataResource = new HBaseMetadataResource(environmentService);
        result.add(hbaseMetadataResource);
        return result;
    }

    private List<Object> getNotificationsRelatedResources(StreamCatalogService streamcatalogService) {
        List<Object> result = new ArrayList<>();
        result.add(new NotifierInfoCatalogResource(streamcatalogService, fileStorage));
        result.add(new NotificationsResource(new NotificationServiceImpl()));
        return result;
    }

    private void watchFiles(StreamCatalogService catalogService) {
        String customProcessorWatchPath = (String) config.get(com.hortonworks.streamline.streams.common.Constants.CONFIG_CP_WATCH_PATH);
        String customProcessorUploadFailPath = (String) config.get(com.hortonworks.streamline.streams.common.Constants.CONFIG_CP_UPLOAD_FAIL_PATH);
        String customProcessorUploadSuccessPath = (String) config.get(com.hortonworks.streamline.streams.common.Constants.CONFIG_CP_UPLOAD_SUCCESS_PATH);
        if (customProcessorWatchPath == null || customProcessorUploadFailPath == null || customProcessorUploadSuccessPath == null) {
            return;
        }
        FileEventHandler customProcessorUploadHandler = new CustomProcessorUploadHandler(customProcessorWatchPath, customProcessorUploadFailPath,
                                                                                         customProcessorUploadSuccessPath, catalogService);
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

    private void setupPlaceholderEntities(StreamCatalogService catalogService) {
        setupPlaceholderTopologyVersionInfo(catalogService);
    }

    private void setupPlaceholderTopologyVersionInfo(StreamCatalogService catalogService) {
        TopologyVersion versionInfo = catalogService.getTopologyVersionInfo(StreamCatalogService.PLACEHOLDER_ID);
        if (versionInfo == null) {
            TopologyVersion topologyVersion = new TopologyVersion();
            topologyVersion.setId(StreamCatalogService.PLACEHOLDER_ID);
            topologyVersion.setTopologyId(StreamCatalogService.PLACEHOLDER_ID);
            topologyVersion.setName("PLACEHOLDER_VERSIONINFO");
            topologyVersion.setDescription("PLACEHOLDER_VERSIONINFO");
            topologyVersion.setTimestamp(System.currentTimeMillis());
            catalogService.addOrUpdateTopologyVersionInfo(StreamCatalogService.PLACEHOLDER_ID, topologyVersion);
        }
    }

}
