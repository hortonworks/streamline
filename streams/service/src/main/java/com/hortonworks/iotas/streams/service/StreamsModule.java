package com.hortonworks.iotas.streams.service;

import com.hortonworks.iotas.common.FileEventHandler;
import com.hortonworks.iotas.common.FileWatcher;
import com.hortonworks.iotas.common.ModuleRegistration;
import com.hortonworks.iotas.common.TimeSeriesDBConfiguration;
import com.hortonworks.iotas.common.util.FileStorage;
import com.hortonworks.iotas.common.util.ReflectionHelper;
import com.hortonworks.iotas.registries.parser.client.ParserClient;
import com.hortonworks.iotas.registries.tag.client.TagClient;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.StorageManagerAware;
import com.hortonworks.iotas.streams.catalog.DataSourceFacade;
import com.hortonworks.iotas.streams.catalog.service.CatalogService;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;
import com.hortonworks.iotas.streams.exception.ConfigException;
import com.hortonworks.iotas.streams.layout.TopologyLayoutConstants;
import com.hortonworks.iotas.streams.layout.component.TopologyActions;
import com.hortonworks.iotas.streams.layout.storm.StormTopologyLayoutConstants;
import com.hortonworks.iotas.streams.metrics.TimeSeriesQuerier;
import com.hortonworks.iotas.streams.metrics.topology.TopologyMetrics;
import com.hortonworks.iotas.streams.notification.service.NotificationServiceImpl;
import com.hortonworks.iotas.streams.notification.service.NotificationsResource;

import java.util.ArrayList;
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
        String catalogRootUrl = (String) config.get("catalogRootUrl");
        TagClient tagClient = new TagClient(catalogRootUrl);
        ParserClient parserClient = new ParserClient(catalogRootUrl);
        final CatalogService catalogService = new CatalogService(storageManager, fileStorage, tagClient, parserClient);

        final FeedCatalogResource feedResource = new FeedCatalogResource(catalogService);
        result.add(feedResource);
        final DataSourceWithDataFeedCatalogResource dataSourceWithDataFeedCatalogResource =
                new DataSourceWithDataFeedCatalogResource(new DataSourceFacade(catalogService, tagClient));
        result.add(dataSourceWithDataFeedCatalogResource);
        final TopologyCatalogResource topologyCatalogResource = new TopologyCatalogResource(streamcatalogService);
        result.add(topologyCatalogResource);
        final MetricsResource metricsResource = new MetricsResource(streamcatalogService);
        result.add(metricsResource);
        final TopologyStreamCatalogResource topologyStreamCatalogResource = new TopologyStreamCatalogResource(streamcatalogService);
        result.add(topologyStreamCatalogResource);
        // cluster related
        final ClusterCatalogResource clusterCatalogResource = new ClusterCatalogResource(streamcatalogService, fileStorage);
        result.add(clusterCatalogResource);
        final ComponentCatalogResource componentCatalogResource = new ComponentCatalogResource(streamcatalogService);
        result.add(componentCatalogResource);
        final TopologyEditorMetadataResource topologyEditorMetadataResource = new TopologyEditorMetadataResource(streamcatalogService);
        result.add(topologyEditorMetadataResource);
        final FileCatalogResource fileCatalogResource = new FileCatalogResource(catalogService);
        result.add(fileCatalogResource);
        // topology related
        final TopologySourceCatalogResource topologySourceCatalogResource = new TopologySourceCatalogResource(streamcatalogService);
        result.add(topologySourceCatalogResource);
        final TopologySinkCatalogResource topologySinkCatalogResource = new TopologySinkCatalogResource(streamcatalogService);
        result.add(topologySinkCatalogResource);
        final TopologyProcessorCatalogResource topologyProcessorCatalogResource = new TopologyProcessorCatalogResource(streamcatalogService);
        result.add(topologyProcessorCatalogResource);
        final TopologyEdgeCatalogResource topologyEdgeCatalogResource = new TopologyEdgeCatalogResource(streamcatalogService);
        result.add(topologyEdgeCatalogResource);
        final RuleCatalogResource ruleCatalogResource = new RuleCatalogResource(streamcatalogService);
        result.add(ruleCatalogResource);
        // UDF catalaog resource
        final UDFCatalogResource udfCatalogResource = new UDFCatalogResource(streamcatalogService, fileStorage);
        result.add(udfCatalogResource);
        Boolean isNotificationsRestDisabled = (Boolean) config.get("notificationsRestDisable");
        if (isNotificationsRestDisabled == null || !isNotificationsRestDisabled) {
            result.add(new NotifierInfoCatalogResource(streamcatalogService));
            result.add(new NotificationsResource(new NotificationServiceImpl()));
        }
        watchFiles(streamcatalogService);
        return result;
    }

    @Override
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    private TopologyActions getTopologyActionsImpl () {
        String className = (String) config.get("topologyActionsImpl");
        // Note that iotasStormJar value needs to be changed in iotas.yaml
        // based on the location of the storm module jar of iotas project.
        // Reason for doing it this way is storm ui right now does not
        // support submitting a jar because of security vulnerability. Hence
        // for now, we just run the storm jar command in a shell on machine
        // where IoTaS is deployed. It is run in StormTopologyActionsImpl
        // class. This also adds a security vulnerability. We will change
        // this later on using our cluster entity when its handled right in
        // storm.
        String jar = (String) config.get("iotasStormJar");
        TopologyActions topologyActions;
        try {
            topologyActions = ReflectionHelper.newInstance(className);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        //pass any config info that might be needed in the constructor as a map
        Map<String, String> conf = new HashMap<>();
        conf.put(StormTopologyLayoutConstants.STORM_JAR_LOCATION_KEY, jar);
        conf.put(StormTopologyLayoutConstants.YAML_KEY_CATALOG_ROOT_URL, (String) config.get("catalogRootUrl"));
        conf.put(StormTopologyLayoutConstants.STORM_HOME_DIR, (String) config.get("stormHomeDir"));
        conf.put(TopologyLayoutConstants.JAVA_JAR_COMMAND, (String) config.get("javaJarCommand"));
        topologyActions.init(conf);
        return topologyActions;
    }

    private TopologyMetrics getTopologyMetricsImpl () throws ConfigException {
        String className = (String) config.get("topologyMetricsImpl");
        TopologyMetrics topologyMetrics;
        try {
            topologyMetrics = ReflectionHelper.newInstance(className);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        //pass any config info that might be needed in the constructor as a map
        Map<String, String> conf = new HashMap<>();
        conf.put(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY, (String) config.get("stormApiRootUrl"));
        topologyMetrics.init(conf);

        TimeSeriesQuerier timeSeriesQuerier = getTimeSeriesQuerier();
        if (timeSeriesQuerier != null) {
            topologyMetrics.setTimeSeriesQuerier(timeSeriesQuerier);
        }

        return topologyMetrics;
    }

    private TimeSeriesQuerier getTimeSeriesQuerier() {
        if (config.get("timeSeriesDBConfiguration") == null) {
            return null;
        }

        try {
            TimeSeriesDBConfiguration timeSeriesDBConfiguration = (TimeSeriesDBConfiguration) config.get("timeSeriesDBConfiguration");
            TimeSeriesQuerier timeSeriesQuerier = ReflectionHelper.newInstance(timeSeriesDBConfiguration.getClassName());
            timeSeriesQuerier.init(timeSeriesDBConfiguration.getProperties());
            return timeSeriesQuerier;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void watchFiles (StreamCatalogService catalogService) {
        String customProcessorWatchPath = (String) config.get("customProcessorWatchPath");
        String customProcessorUploadFailPath = (String) config.get("customProcessorUploadFailPath");
        String customProcessorUploadSuccessPath = (String) config.get("customProcessorUploadSuccessPath");
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
