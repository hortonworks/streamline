package org.apache.streamline.streams.service;

import org.apache.streamline.common.Constants;
import org.apache.streamline.common.FileEventHandler;
import org.apache.streamline.common.FileWatcher;
import org.apache.streamline.common.ModuleRegistration;
import org.apache.streamline.common.TimeSeriesDBConfiguration;
import org.apache.streamline.common.util.FileStorage;
import org.apache.streamline.common.util.ReflectionHelper;
import org.apache.streamline.registries.parser.client.ParserClient;
import org.apache.streamline.registries.tag.client.TagClient;
import org.apache.streamline.storage.StorageManager;
import org.apache.streamline.storage.StorageManagerAware;
import org.apache.streamline.streams.catalog.service.CatalogService;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.exception.ConfigException;
import org.apache.streamline.streams.layout.TopologyLayoutConstants;
import org.apache.streamline.streams.layout.component.TopologyActions;
import org.apache.streamline.streams.layout.storm.StormTopologyLayoutConstants;
import org.apache.streamline.streams.metrics.TimeSeriesQuerier;
import org.apache.streamline.streams.metrics.topology.TopologyMetrics;
import org.apache.streamline.streams.notification.service.NotificationServiceImpl;
import org.apache.streamline.streams.notification.service.NotificationsResource;
import org.apache.registries.schemaregistry.client.SchemaRegistryClient;
import org.apache.streamline.streams.service.metadata.HBaseMetadataResource;
import org.apache.streamline.streams.service.metadata.HiveMetadataResource;
import org.apache.streamline.streams.service.metadata.KafkaMetadataResource;
import org.apache.streamline.streams.service.metadata.StormMetadataResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        final StreamCatalogService streamcatalogService;
        try {
            streamcatalogService = new StreamCatalogService(storageManager, getTopologyActionsImpl(), getTopologyMetricsImpl(), fileStorage);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
        String catalogRootUrl = (String) config.get(Constants.CONFIG_CATALOG_ROOT_URL);
        TagClient tagClient = new TagClient(catalogRootUrl);
        ParserClient parserClient = new ParserClient(catalogRootUrl);
        final CatalogService catalogService = new CatalogService(storageManager, fileStorage, tagClient, parserClient);
        result.add(new MetricsResource(streamcatalogService));
        result.addAll(getClusterRelatedResources(streamcatalogService));
        result.add(new FileCatalogResource(catalogService));
        result.addAll(getTopologyRelatedResources(streamcatalogService));
        result.add(new RuleCatalogResource(streamcatalogService));
        result.add(new UDFCatalogResource(streamcatalogService, fileStorage));
        result.addAll(getNotificationsRelatedResources(streamcatalogService));
        result.add(new WindowCatalogResource(streamcatalogService));
        result.add(new SchemaResource(createSchemaRegistryClient()));
        result.addAll(getServiceMetadataResources(streamcatalogService));
        watchFiles(streamcatalogService);
        return result;
    }

    private SchemaRegistryClient createSchemaRegistryClient() {
        Map<String, ?> conf = Collections.singletonMap(SchemaRegistryClient.Options.SCHEMA_REGISTRY_URL, config.get("schemaRegistryUrl"));
        return new SchemaRegistryClient(conf);
    }

    @Override
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    private List<Object> getTopologyRelatedResources(StreamCatalogService streamcatalogService) {
        List<Object> result = new ArrayList<>();
        final TopologyCatalogResource topologyCatalogResource = new TopologyCatalogResource(streamcatalogService);
        result.add(topologyCatalogResource);
        final TopologyComponentBundleResource topologyComponentResource = new TopologyComponentBundleResource(streamcatalogService);
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

    private List<Object> getClusterRelatedResources(StreamCatalogService streamcatalogService) {
        List<Object> result = new ArrayList<>();
        final ClusterCatalogResource clusterCatalogResource = new ClusterCatalogResource(streamcatalogService);
        result.add(clusterCatalogResource);
        final ServiceCatalogResource serviceCatalogResource = new ServiceCatalogResource(streamcatalogService);
        result.add(serviceCatalogResource);
        final ServiceConfigurationCatalogResource serviceConfigurationCatalogResource = new ServiceConfigurationCatalogResource(streamcatalogService);
        result.add(serviceConfigurationCatalogResource);
        final ComponentCatalogResource componentCatalogResource = new ComponentCatalogResource(streamcatalogService);
        result.add(componentCatalogResource);
        return result;
    }

    private List<Object> getServiceMetadataResources(StreamCatalogService streamcatalogService) {
        final List<Object> result = new ArrayList<>();
        final KafkaMetadataResource kafkaMetadataResource = new KafkaMetadataResource(streamcatalogService);
        result.add(kafkaMetadataResource);
        final StormMetadataResource stormMetadataResource = new StormMetadataResource(streamcatalogService);
        result.add(stormMetadataResource);
        final HiveMetadataResource hiveMetadataResource = new HiveMetadataResource(streamcatalogService);
        result.add(hiveMetadataResource);
        final HBaseMetadataResource hbaseMetadataResource = new HBaseMetadataResource(streamcatalogService);
        result.add(hbaseMetadataResource);
        return result;
    }

    private List<Object> getNotificationsRelatedResources(StreamCatalogService streamcatalogService) {
        List<Object> result = new ArrayList<>();
        Boolean isNotificationsRestDisabled = (Boolean) config.get(org.apache.streamline.streams.common.Constants.CONFIG_NOTIFICATIONS_REST_FLAG);
        result.add(new NotifierInfoCatalogResource(streamcatalogService, fileStorage));
        if (isNotificationsRestDisabled == null || !isNotificationsRestDisabled) {
            result.add(new NotificationsResource(new NotificationServiceImpl()));
        }
        return result;
    }

    private TopologyActions getTopologyActionsImpl() {
        String className = (String) config.get(org.apache.streamline.streams.common.Constants.CONFIG_TOPOLOGY_ACTIONS_IMPL);
        // Note that streamlineStormJar value needs to be changed in streamline.yaml
        // based on the location of the storm module jar of Streamline project.
        // Reason for doing it this way is storm ui right now does not
        // support submitting a jar because of security vulnerability. Hence
        // for now, we just run the storm jar command in a shell on machine
        // where Streamline is deployed. It is run in StormTopologyActionsImpl
        // class. This also adds a security vulnerability. We will change
        // this later on using our cluster entity when its handled right in
        // storm.
        String jar = (String) config.get(StormTopologyLayoutConstants.STORM_JAR_LOCATION_KEY);
        TopologyActions topologyActions;
        try {
            topologyActions = ReflectionHelper.newInstance(className);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        //pass any config info that might be needed in the constructor as a map
        Map<String, String> conf = new HashMap<>();
        conf.put(StormTopologyLayoutConstants.STORM_JAR_LOCATION_KEY, jar);
        conf.put(StormTopologyLayoutConstants.YAML_KEY_CATALOG_ROOT_URL, (String) config.get(Constants.CONFIG_CATALOG_ROOT_URL));
        conf.put(StormTopologyLayoutConstants.STORM_HOME_DIR, (String) config.get(StormTopologyLayoutConstants.STORM_HOME_DIR));
        conf.put(TopologyLayoutConstants.JAVA_JAR_COMMAND, (String) config.get(TopologyLayoutConstants.JAVA_JAR_COMMAND));
        conf.put(TopologyLayoutConstants.SCHEMA_REGISTRY_URL, (String) config.get(TopologyLayoutConstants.SCHEMA_REGISTRY_URL));
        topologyActions.init(conf);
        return topologyActions;
    }

    private TopologyMetrics getTopologyMetricsImpl() throws ConfigException {
        String className = (String) config.get(org.apache.streamline.streams.common.Constants.CONFIG_TOPOLOGY_METRICS_IMPL);
        TopologyMetrics topologyMetrics;
        try {
            topologyMetrics = ReflectionHelper.newInstance(className);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        //pass any config info that might be needed in the constructor as a map
        Map<String, String> conf = new HashMap<>();
        conf.put(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY, (String) config.get(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY));
        topologyMetrics.init(conf);

        TimeSeriesQuerier timeSeriesQuerier = getTimeSeriesQuerier();
        if (timeSeriesQuerier != null) {
            topologyMetrics.setTimeSeriesQuerier(timeSeriesQuerier);
        }

        return topologyMetrics;
    }

    private TimeSeriesQuerier getTimeSeriesQuerier() {
        if (config.get(Constants.CONFIG_TIME_SERIES_DB) == null) {
            return null;
        }

        try {
            TimeSeriesDBConfiguration timeSeriesDBConfiguration = (TimeSeriesDBConfiguration) config.get(Constants.CONFIG_TIME_SERIES_DB);
            TimeSeriesQuerier timeSeriesQuerier = ReflectionHelper.newInstance(timeSeriesDBConfiguration.getClassName());
            timeSeriesQuerier.init(timeSeriesDBConfiguration.getProperties());
            return timeSeriesQuerier;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void watchFiles(StreamCatalogService catalogService) {
        String customProcessorWatchPath = (String) config.get(org.apache.streamline.streams.common.Constants.CONFIG_CP_WATCH_PATH);
        String customProcessorUploadFailPath = (String) config.get(org.apache.streamline.streams.common.Constants.CONFIG_CP_UPLOAD_FAIL_PATH);
        String customProcessorUploadSuccessPath = (String) config.get(org.apache.streamline.streams.common.Constants.CONFIG_CP_UPLOAD_SUCCESS_PATH);
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
}
