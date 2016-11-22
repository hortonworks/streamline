package org.apache.streamline.streams.catalog.container;

import org.apache.streamline.streams.catalog.Component;
import org.apache.streamline.streams.catalog.Namespace;
import org.apache.streamline.streams.catalog.Service;
import org.apache.streamline.streams.catalog.container.mapping.MappedTimeSeriesQuerierImpl;
import org.apache.streamline.streams.catalog.container.mapping.MappedTopologyMetricsImpl;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import org.apache.streamline.streams.exception.ConfigException;
import org.apache.streamline.streams.layout.TopologyLayoutConstants;
import org.apache.streamline.streams.metrics.TimeSeriesQuerier;
import org.apache.streamline.streams.metrics.topology.TopologyMetrics;

import java.util.HashMap;
import java.util.Map;

public class TopologyMetricsContainer extends NamespaceAwareContainer<TopologyMetrics> {

    public static final String COMPONENT_NAME_STORM_UI_SERVER = ComponentPropertyPattern.STORM_UI_SERVER.name();
    public static final String COMPONENT_NAME_METRICS_COLLECTOR = ComponentPropertyPattern.METRICS_COLLECTOR.name();
    public static final String COLLECTOR_API_URL_KEY = "collectorApiUrl";

    public TopologyMetricsContainer(StreamCatalogService catalogService) {
        super(catalogService);
    }

    @Override
    protected TopologyMetrics initializeInstance(Namespace namespace) {
        String streamingEngine = namespace.getStreamingEngine();

        MappedTopologyMetricsImpl metricsImpl;
        // Only Storm is supported as streaming engine
        try {
            metricsImpl = MappedTopologyMetricsImpl.valueOf(streamingEngine);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unsupported streaming engine: " + streamingEngine, e);
        }

        // FIXME: "how to initialize" is up to implementation detail - now we just only consider about Storm implementation
        Map<String, String> conf = buildStormTopologyMetricsConfigMap(namespace, streamingEngine);

        String className = metricsImpl.getClassName();
        TopologyMetrics topologyMetrics = initTopologyMetrics(conf, className);

        String timeSeriesDB = namespace.getTimeSeriesDB();
        if (timeSeriesDB != null && !timeSeriesDB.isEmpty()) {
            String querierKey = MappedTimeSeriesQuerierImpl.getName(streamingEngine, timeSeriesDB);

            MappedTimeSeriesQuerierImpl timeSeriesQuerierImpl;
            try {
                timeSeriesQuerierImpl = MappedTimeSeriesQuerierImpl.valueOf(querierKey);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Unsupported streaming engine and time-series DB combination: " + streamingEngine +
                        " & " + timeSeriesDB, e);
            }

            // FIXME: "how to initialize" is up to implementation detail - now we just only consider about Storm & AMS implementation
            Map<String, String> confTimeSeriesQuerier = buildAMSTimeSeriesQuerierConfigMap(namespace, timeSeriesDB);

            className = timeSeriesQuerierImpl.getClassName();
            TimeSeriesQuerier timeSeriesQuerier = initTimeSeriesQuerier(confTimeSeriesQuerier, className);

            topologyMetrics.setTimeSeriesQuerier(timeSeriesQuerier);
        }

        return topologyMetrics;
    }

    private TopologyMetrics initTopologyMetrics(Map<String, String> conf, String className) {
        try {
            TopologyMetrics topologyMetrics = instantiate(className);
            topologyMetrics.init(conf);
            return topologyMetrics;
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException | ConfigException e) {
            throw new RuntimeException("Can't initialize Topology metrics instance - Class Name: " + className, e);
        }
    }

    private TimeSeriesQuerier initTimeSeriesQuerier(Map<String, String> conf, String className) {
        try {
            Class timeSeriesQuerierImplClass = Class.forName(className);
            TimeSeriesQuerier timeSeriesQuerier = (TimeSeriesQuerier) timeSeriesQuerierImplClass.newInstance();
            timeSeriesQuerier.init(conf);
            return timeSeriesQuerier;
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException | ConfigException e) {
            throw new RuntimeException("Can't initialize Time-series Querier instance - Class Name: " + className, e);
        }
    }

    private Map<String, String> buildStormTopologyMetricsConfigMap(Namespace namespace, String streamingEngine) {
        Service streamingEngineService = getFirstOccurenceServiceForNamespace(namespace, streamingEngine);
        if (streamingEngineService == null) {
            throw new RuntimeException("Streaming Engine " + streamingEngine + " is not associated to the namespace " +
                    namespace.getName() + "(" + namespace.getId() + ")");
        }

        Component uiServer = getComponent(streamingEngineService, COMPONENT_NAME_STORM_UI_SERVER);
        String uiHost = uiServer.getHosts().get(0);
        Integer uiPort = uiServer.getPort();

        assertHostAndPort(uiServer.getName(), uiHost, uiPort);

        Map<String, String> conf = new HashMap<>();
        conf.put(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY, buildStormRestApiRootUrl(uiHost, uiPort));
        return conf;
    }

    private Map<String, String> buildAMSTimeSeriesQuerierConfigMap(Namespace namespace, String timeSeriesDB) {
        Service timeSeriesDBService = getFirstOccurenceServiceForNamespace(namespace, timeSeriesDB);
        if (timeSeriesDBService == null) {
            throw new RuntimeException("Time-series DB " + timeSeriesDB + " is not associated to the namespace " +
                    namespace.getName() + "(" + namespace.getId() + ")");
        }

        Component metricsCollector = getComponent(timeSeriesDBService, COMPONENT_NAME_METRICS_COLLECTOR);
        String metricsCollectorHost = metricsCollector.getHosts().get(0);
        Integer metricsCollectorPort = metricsCollector.getPort();

        assertHostAndPort(COMPONENT_NAME_METRICS_COLLECTOR, metricsCollectorHost, metricsCollectorPort);

        Map<String, String> confForTimeSeriesQuerier = new HashMap<>();
        confForTimeSeriesQuerier.put(COLLECTOR_API_URL_KEY, buildAMSCollectorRestApiRootUrl(metricsCollectorHost, metricsCollectorPort));
        return confForTimeSeriesQuerier;
    }

    private String buildStormRestApiRootUrl(String host, Integer port) {
        return "http://" + host + ":" + port + "/api/v1";
    }

    private String buildAMSCollectorRestApiRootUrl(String host, Integer port) {
        return "http://" + host + ":" + port + "/ws/v1/timeline/metrics";
    }
}