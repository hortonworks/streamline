package com.hortonworks.iotas.topology.storm;

import com.hortonworks.iotas.catalog.Topology;
import com.hortonworks.iotas.common.errors.ConfigException;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.topology.TopologyMetrics;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storm implementation of the TopologyMetrics interface
 */
public class StormTopologyMetricsImpl implements TopologyMetrics {
    private Client client;
    private String stormApiRootUrl;

    public StormTopologyMetricsImpl() {
    }

    @Override
    public void init(Map<String, String> conf) throws ConfigException {
        if (conf != null) {
            stormApiRootUrl = conf.get(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY);
        }
        client = ClientBuilder.newClient(new ClientConfig());
    }

    @Override
    public Map<String, ComponentMetric> getMetricsForTopology(Topology topology) {
        String stormTopologyName = getTopologyName(topology);

        Map<String, ComponentMetric> metricMap = new HashMap<>();

        String topologyId = findTopologyId(stormTopologyName);
        if (topologyId == null) {
            throw new RuntimeException("Topology not found in Storm Cluster - topology name in Storm: " + stormTopologyName);
        }

        Map<String, ?> responseMap = client.target(getTopologyUrl(topologyId)).request(MediaType.APPLICATION_JSON_TYPE).get(Map.class);

        List<Map<String, ?>> spouts = (List<Map<String, ?>>) responseMap.get("spouts");
        for (Map<String, ?> spout : spouts) {
            String spoutName = (String) spout.get("spoutId");
            ComponentMetric metric = extractMetric(spoutName, spout);
            metricMap.put(metric.getComponentName(), metric);
        }

        List<Map<String, ?>> bolts = (List<Map<String, ?>>) responseMap.get("bolts");
        for (Map<String, ?> bolt : bolts) {
            String boltName = (String) bolt.get("boltId");
            ComponentMetric metric = extractMetric(boltName, bolt);
            metricMap.put(metric.getComponentName(), metric);
        }

        return metricMap;
    }

    private ComponentMetric extractMetric(String componentName, Map<String, ?> componentMap) {
        Long inputRecords = 0L;
        Long outputRecords = 0L;
        Double processedTime = 0.0d;
        Long failedRecords = 0L;

        if (componentMap.containsKey(StormMetricsConstant.COMPONENT_EXECUTED_TUPLES)) {
            Number executed = (Number) componentMap.get(StormMetricsConstant.COMPONENT_EXECUTED_TUPLES);
            inputRecords = executed.longValue();
        }
        if (componentMap.containsKey(StormMetricsConstant.COMPONENT_EMITTED_TUPLES)) {
            Number emitted = (Number) componentMap.get(StormMetricsConstant.COMPONENT_EMITTED_TUPLES);
            outputRecords = emitted.longValue();
        }
        if (componentMap.containsKey(StormMetricsConstant.COMPONENT_PROCESS_LATENCY)) {
            String processLatencyStr = (String) componentMap.get(StormMetricsConstant.COMPONENT_PROCESS_LATENCY);
            try {
                processedTime = Double.parseDouble(processLatencyStr);
            } catch (NumberFormatException e) {
                // noop
            }
        }
        if (componentMap.containsKey(StormMetricsConstant.COMPONENT_FAILED_TUPLES)) {
            Number failed = (Number) componentMap.get(StormMetricsConstant.COMPONENT_FAILED_TUPLES);
            failedRecords = failed.longValue();
        }

        return new ComponentMetric(componentName, inputRecords, outputRecords, failedRecords, processedTime);
    }

    private String findTopologyId(String topologyName) {
        Map<?, ?> summaryMap = client.target(getTopologySummaryUrl()).request(MediaType.APPLICATION_JSON_TYPE).get(Map.class);
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

    private String getTopologyName(Topology topology) {
        return "iotas-" + topology.getId() + "-" + topology.getName();
    }

}