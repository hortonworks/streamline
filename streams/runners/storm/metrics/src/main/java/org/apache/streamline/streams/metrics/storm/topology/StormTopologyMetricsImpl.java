package org.apache.streamline.streams.metrics.storm.topology;

import org.apache.streamline.streams.exception.ConfigException;
import org.apache.streamline.streams.layout.TopologyLayoutConstants;
import org.apache.streamline.streams.layout.component.TopologyLayout;
import org.apache.streamline.streams.metrics.TimeSeriesQuerier;
import org.apache.streamline.streams.metrics.topology.TopologyMetrics;
import org.apache.streamline.streams.metrics.topology.TopologyTimeSeriesMetrics;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.STATS_JSON_ACKED_TUPLES;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.STATS_JSON_COMPLETE_LATENCY;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.STATS_JSON_EMITTED_TUPLES;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.STATS_JSON_FAILED_TUPLES;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.STATS_JSON_PROCESS_LATENCY;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.STATS_JSON_TRANSFERRED_TUPLES;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.TOPOLOGY_JSON_BOLTS;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.TOPOLOGY_JSON_BOLT_ID;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.TOPOLOGY_JSON_EXECUTORS_TOTAL;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.TOPOLOGY_JSON_SPOUTS;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.TOPOLOGY_JSON_SPOUT_ID;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.TOPOLOGY_JSON_STATS;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.TOPOLOGY_JSON_STATUS;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.TOPOLOGY_JSON_UPTIME_SECS;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.TOPOLOGY_JSON_WINDOW;
import static org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant.TOPOLOGY_JSON_WORKERS_TOTAL;

/**
 * Storm implementation of the TopologyMetrics interface
 */
public class StormTopologyMetricsImpl implements TopologyMetrics {
    public static final String FRAMEWORK = "STORM";

    private Client client;
    private String stormApiRootUrl;
    private TopologyTimeSeriesMetrics timeSeriesMetrics;

    public StormTopologyMetricsImpl() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Map<String, String> conf) throws ConfigException {
        if (conf != null) {
            stormApiRootUrl = conf.get(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY);
        }
        client = ClientBuilder.newClient(new ClientConfig());
        timeSeriesMetrics = new StormTopologyTimeSeriesMetricsImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopologyMetric getTopologyMetric(TopologyLayout topology) {
        String stormTopologyName = getTopologyName(topology);

        String topologyId = findTopologyId(stormTopologyName);
        if (topologyId == null) {
            throw new TopologyNotAliveException("Topology not found in Storm Cluster - topology name in Storm: " + stormTopologyName);
        }

        Map<String, ?> responseMap = doGetRequest(getTopologyUrl(topologyId));

        Long uptimeSeconds = ((Number) responseMap.get(TOPOLOGY_JSON_UPTIME_SECS)).longValue();
        String status = (String) responseMap.get(TOPOLOGY_JSON_STATUS);
        Long workerTotal = ((Number) responseMap.get(TOPOLOGY_JSON_WORKERS_TOTAL)).longValue();
        Long executorTotal = ((Number) responseMap.get(TOPOLOGY_JSON_EXECUTORS_TOTAL)).longValue();

        List<Map<String, ?>> topologyStatsList = (List<Map<String, ?>>) responseMap.get(TOPOLOGY_JSON_STATS);

        // pick smallest time window
        Map<String, ?> topologyStatsMap = null;
        Long smallestWindow = Long.MAX_VALUE;
        for (Map<String, ?> topoStats : topologyStatsList) {
            String windowStr = (String) topoStats.get(TOPOLOGY_JSON_WINDOW);
            Long window = convertWindowString(windowStr, uptimeSeconds);
            if (smallestWindow > window) {
                smallestWindow = window;
                topologyStatsMap = topoStats;
            }
        }

        // extract metrics from smallest time window
        Long window = smallestWindow;
        Long acked = getLongValueOrDefault(topologyStatsMap, STATS_JSON_ACKED_TUPLES, 0L);
        Long failedRecords = getLongValueOrDefault(topologyStatsMap, STATS_JSON_FAILED_TUPLES, 0L);
        Double completeLatency = getDoubleValueFromStringOrDefault(topologyStatsMap, STATS_JSON_COMPLETE_LATENCY, 0.0d);

        // Storm specific metrics
        Long emittedTotal = getLongValueOrDefault(topologyStatsMap, STATS_JSON_EMITTED_TUPLES, 0L);
        Long transferred = getLongValueOrDefault(topologyStatsMap, STATS_JSON_TRANSFERRED_TUPLES, 0L);

        Map<String, Number> miscMetrics = new HashMap<>();
        miscMetrics.put(TOPOLOGY_JSON_WORKERS_TOTAL, workerTotal);
        miscMetrics.put(TOPOLOGY_JSON_EXECUTORS_TOTAL, executorTotal);
        miscMetrics.put(STATS_JSON_EMITTED_TUPLES, emittedTotal);
        miscMetrics.put(STATS_JSON_TRANSFERRED_TUPLES, transferred);

        return new TopologyMetric(FRAMEWORK, topology.getName(), status, uptimeSeconds, window,
            acked * 1.0 / window, completeLatency, failedRecords, miscMetrics);
    }

    private Map doGetRequest(String requestUrl) {
        try {
            return client.target(requestUrl).request(MediaType.APPLICATION_JSON_TYPE).get(Map.class);
        } catch (javax.ws.rs.ProcessingException e) {
            if (e.getCause() instanceof IOException) {
                throw new StormNotReachableException("Exception while requesting " + requestUrl, e);
            }

            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ComponentMetric> getMetricsForTopology(TopologyLayout topology) {
        String stormTopologyName = getTopologyName(topology);

        Map<String, ComponentMetric> metricMap = new HashMap<>();

        String topologyId = findTopologyId(stormTopologyName);
        if (topologyId == null) {
            throw new TopologyNotAliveException("Topology not found in Storm Cluster - topology name in Storm: " + stormTopologyName);
        }

        Map<String, ?> responseMap = doGetRequest(getTopologyUrl(topologyId));

        List<Map<String, ?>> spouts = (List<Map<String, ?>>) responseMap.get(TOPOLOGY_JSON_SPOUTS);
        for (Map<String, ?> spout : spouts) {
            String spoutName = (String) spout.get(TOPOLOGY_JSON_SPOUT_ID);
            ComponentMetric metric = extractMetric(spoutName, spout);
            metricMap.put(metric.getComponentName(), metric);
        }

        List<Map<String, ?>> bolts = (List<Map<String, ?>>) responseMap.get(TOPOLOGY_JSON_BOLTS);
        for (Map<String, ?> bolt : bolts) {
            String boltName = (String) bolt.get(TOPOLOGY_JSON_BOLT_ID);
            ComponentMetric metric = extractMetric(boltName, bolt);
            metricMap.put(metric.getComponentName(), metric);
        }

        return metricMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeSeriesQuerier(TimeSeriesQuerier timeSeriesQuerier) {
        timeSeriesMetrics.setTimeSeriesQuerier(timeSeriesQuerier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimeSeriesQuerier getTimeSeriesQuerier() {
        return timeSeriesMetrics.getTimeSeriesQuerier();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Double> getCompleteLatency(TopologyLayout topology, String sourceId, long from, long to) {
        return timeSeriesMetrics.getCompleteLatency(topology, sourceId, from, to);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Map<Long, Double>> getkafkaTopicOffsets(TopologyLayout topology, String sourceId, long from, long to) {
        return timeSeriesMetrics.getkafkaTopicOffsets(topology, sourceId, from, to);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Map<Long, Double>> getComponentStats(TopologyLayout topology, String componentId, long from, long to) {
        return timeSeriesMetrics.getComponentStats(topology, componentId, from, to);
    }

    private ComponentMetric extractMetric(String componentName, Map<String, ?> componentMap) {
        Long inputRecords = getLongValueOrDefault(componentMap, StormMetricsConstant.STATS_JSON_EXECUTED_TUPLES, 0L);
        Long outputRecords = getLongValueOrDefault(componentMap, STATS_JSON_EMITTED_TUPLES, 0L);
        Long failedRecords = getLongValueOrDefault(componentMap, StormMetricsConstant.STATS_JSON_FAILED_TUPLES, 0L);
        Double processedTime = getDoubleValueFromStringOrDefault(componentMap, STATS_JSON_PROCESS_LATENCY, 0.0d);

        return new ComponentMetric(componentName, inputRecords, outputRecords, failedRecords, processedTime);
    }

    private String findTopologyId(String topologyName) {
        Map<?, ?> summaryMap = doGetRequest(getTopologySummaryUrl());
        List<Map<?, ?>> topologies = (List<Map<?, ?>>) summaryMap.get(StormMetricsConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGIES);
        String topologyId = null;
        for (Map<?, ?> topology : topologies) {
            String topologyNameForStorm = (String) topology.get(StormMetricsConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGY_NAME);

            if (topologyNameForStorm.equals(topologyName)) {
                topologyId = (String) topology.get(StormMetricsConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGY_ID_ENCODED);
                break;
            }
        }
        return topologyId;
    }

    private String getTopologySummaryUrl() {
        return stormApiRootUrl + "/topology/summary";
    }

    private String getTopologyUrl(String topologyId) {
        return stormApiRootUrl + "/topology/" + topologyId;
    }

    private String getTopologyName(TopologyLayout topology) {
        return "streamline-" + topology.getId() + "-" + topology.getName();
    }

    private Long convertWindowString(String windowStr, Long uptime) {
        if (windowStr.equals(":all-time")) {
            return uptime;
        } else {
            return Long.valueOf(windowStr);
        }
    }

    private Long getLongValueOrDefault(Map<String, ?> map, String key, Long defaultValue) {
        if (map.containsKey(key)) {
            Number number = (Number) map.get(key);
            if (number != null) {
                return number.longValue();
            }
        }
        return defaultValue;
    }

    private Double getDoubleValueFromStringOrDefault(Map<String, ?> map, String key,
        Double defaultValue) {
        if (map.containsKey(key)) {
            String valueStr = (String) map.get(key);
            if (valueStr != null) {
                try {
                    return Double.parseDouble(valueStr);
                } catch (NumberFormatException e) {
                    // noop
                }
            }
        }
        return defaultValue;
    }
}