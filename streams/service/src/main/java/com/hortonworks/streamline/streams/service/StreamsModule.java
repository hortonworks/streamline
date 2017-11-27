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

import com.hortonworks.registries.common.transaction.TransactionIsolation;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import com.hortonworks.registries.storage.StorageManagerAware;
import com.hortonworks.registries.storage.TransactionManager;
import com.hortonworks.registries.storage.TransactionManagerAware;
import com.hortonworks.streamline.common.Constants;
import com.hortonworks.streamline.common.FileEventHandler;
import com.hortonworks.streamline.common.FileWatcher;
import com.hortonworks.streamline.common.ModuleRegistration;
import com.hortonworks.registries.common.util.FileStorage;
import com.hortonworks.streamline.registries.model.client.MLModelRegistryClient;
import com.hortonworks.streamline.registries.tag.client.TagClient;
import com.hortonworks.registries.storage.StorageManager;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.TopologyVersion;
import com.hortonworks.streamline.streams.catalog.service.CatalogService;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.resource.ClusterCatalogResource;
import com.hortonworks.streamline.streams.cluster.resource.ComponentCatalogResource;
import com.hortonworks.streamline.streams.cluster.resource.ServiceBundleResource;
import com.hortonworks.streamline.streams.cluster.resource.ServiceCatalogResource;
import com.hortonworks.streamline.streams.cluster.resource.ServiceConfigurationCatalogResource;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyMetricsService;
import com.hortonworks.streamline.streams.notification.service.NotificationServiceImpl;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.service.SecurityCatalogResource;
import com.hortonworks.streamline.streams.security.service.SecurityCatalogService;
import com.hortonworks.streamline.streams.service.metadata.HBaseMetadataResource;
import com.hortonworks.streamline.streams.service.metadata.HiveMetadataResource;
import com.hortonworks.streamline.streams.service.metadata.KafkaMetadataResource;
import com.hortonworks.streamline.streams.service.metadata.StormMetadataResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

/**
 * Implementation for the streams module for registration with web service module
 */
public class StreamsModule implements ModuleRegistration, StorageManagerAware, TransactionManagerAware {
    private FileStorage fileStorage;
    private Map<String, Object> config;
    private StorageManager storageManager;
    private TransactionManager transactionManager;

    @Override
    public void init(Map<String, Object> config, FileStorage fileStorage) {
        this.config = config;
        this.fileStorage = fileStorage;
    }

    @Override
    public List<Object> getResources() {
        List<Object> result = new ArrayList<>();
        String catalogRootUrl = (String) config.get(Constants.CONFIG_CATALOG_ROOT_URL);
        final Subject subject = (Subject) config.get(Constants.CONFIG_SUBJECT);  // Authorized subject
        MLModelRegistryClient modelRegistryClient = new MLModelRegistryClient(catalogRootUrl, subject);
        final StreamCatalogService streamcatalogService = new StreamCatalogService(storageManager, fileStorage, modelRegistryClient);
        final EnvironmentService environmentService = new EnvironmentService(storageManager, transactionManager);
        TagClient tagClient = new TagClient(catalogRootUrl);
        final CatalogService catalogService = new CatalogService(storageManager, fileStorage, tagClient);
        final TopologyActionsService topologyActionsService = new TopologyActionsService(streamcatalogService,
                environmentService, fileStorage, modelRegistryClient, config, subject, transactionManager);
        final TopologyMetricsService topologyMetricsService = new TopologyMetricsService(environmentService, subject);

        environmentService.addNamespaceAwareContainer(topologyActionsService);
        environmentService.addNamespaceAwareContainer(topologyMetricsService);

        // authorizer
        final StreamlineAuthorizer authorizer = (StreamlineAuthorizer) config.get(Constants.CONFIG_AUTHORIZER);
        if (authorizer == null) {
            throw new IllegalStateException("Authorizer not set");
        }
        final SecurityCatalogService securityCatalogService =
                (SecurityCatalogService) config.get(Constants.CONFIG_SECURITY_CATALOG_SERVICE);

        result.addAll(getAuthorizerResources(authorizer, securityCatalogService));
        result.add(new MetricsResource(authorizer, streamcatalogService, topologyMetricsService));
        result.addAll(getClusterRelatedResources(authorizer, environmentService));
        result.add(new FileCatalogResource(authorizer, catalogService));
        result.addAll(getTopologyRelatedResources(authorizer, streamcatalogService, environmentService, topologyActionsService,
                topologyMetricsService, securityCatalogService, subject));
        result.add(new UDFCatalogResource(authorizer, streamcatalogService, fileStorage));
        result.addAll(getNotificationsRelatedResources(authorizer, streamcatalogService));
        result.add(new SchemaResource(createSchemaRegistryClient()));
        result.addAll(getServiceMetadataResources(authorizer, environmentService, subject));
        result.add(new NamespaceCatalogResource(authorizer, streamcatalogService, topologyActionsService, environmentService));
        result.add(new SearchCatalogResource(authorizer, streamcatalogService, environmentService,
                topologyActionsService, topologyMetricsService));
        watchFiles(streamcatalogService);
        setupPlaceholderEntities(streamcatalogService);
        return result;
    }

    private SchemaRegistryClient createSchemaRegistryClient() {
        Map<String, ?> conf = Collections.singletonMap(SchemaRegistryClient.Configuration.SCHEMA_REGISTRY_URL.name(),
                                                       config.get("schemaRegistryUrl"));
        return new SchemaRegistryClient(conf);
    }

    private List<Object> getAuthorizerResources(StreamlineAuthorizer authorizer, SecurityCatalogService securityCatalogService) {
        return Collections.singletonList(new SecurityCatalogResource(authorizer, securityCatalogService));
    }

    private List<Object> getTopologyRelatedResources(StreamlineAuthorizer authorizer,
                                                     StreamCatalogService streamcatalogService,
                                                     EnvironmentService environmentService,
                                                     TopologyActionsService actionsService,
                                                     TopologyMetricsService metricsService,
                                                     SecurityCatalogService securityCatalogService,
                                                     Subject subject) {
        return Arrays.asList(
                new TopologyCatalogResource(authorizer, streamcatalogService, environmentService, actionsService, metricsService, transactionManager),
                new TopologyComponentBundleResource(authorizer, streamcatalogService, environmentService, subject),
                new TopologyStreamCatalogResource(authorizer, streamcatalogService),
                new TopologyEditorMetadataResource(authorizer, streamcatalogService),
                new TopologySourceCatalogResource(authorizer, streamcatalogService),
                new TopologySinkCatalogResource(authorizer, streamcatalogService),
                new TopologyProcessorCatalogResource(authorizer, streamcatalogService),
                new TopologyEdgeCatalogResource(authorizer, streamcatalogService),
                new RuleCatalogResource(authorizer, streamcatalogService),
                new BranchRuleCatalogResource(authorizer, streamcatalogService),
                new WindowCatalogResource(authorizer, streamcatalogService),
                new TopologyEditorToolbarResource(authorizer, streamcatalogService, securityCatalogService),
                new TopologyTestRunResource(streamcatalogService, actionsService)
        );
    }

    private List<Object> getClusterRelatedResources(StreamlineAuthorizer authorizer, EnvironmentService environmentService) {
        return Arrays.asList(
                new ClusterCatalogResource(authorizer, environmentService),
                new ServiceCatalogResource(authorizer, environmentService),
                new ServiceConfigurationCatalogResource(authorizer, environmentService),
                new ComponentCatalogResource(authorizer, environmentService),
                new ServiceBundleResource(environmentService)
        );
    }

    private List<Object> getServiceMetadataResources(StreamlineAuthorizer authorizer, EnvironmentService environmentService, Subject subject) {
        return Arrays.asList(
                new KafkaMetadataResource(authorizer, environmentService),
                new StormMetadataResource(authorizer, environmentService, subject),
                new HiveMetadataResource(authorizer, environmentService, subject),
                new HBaseMetadataResource(authorizer, environmentService, subject)
        );
    }

    private List<Object> getNotificationsRelatedResources(StreamlineAuthorizer authorizer, StreamCatalogService streamcatalogService) {
        return Arrays.asList(
                new NotifierInfoCatalogResource(authorizer, streamcatalogService, fileStorage),
                new NotificationsResource(authorizer, new NotificationServiceImpl())
        );
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
        if (transactionManager == null)
            throw new RuntimeException("TransactionManager is not initialized");
        try {
            transactionManager.beginTransaction(TransactionIsolation.DEFAULT);
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
            transactionManager.commitTransaction();
        } catch (Exception e) {
            transactionManager.rollbackTransaction();
            throw e;
        }
    }

    @Override
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }
}
