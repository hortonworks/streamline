package org.apache.streamline.streams.metrics.storm;

import org.apache.streamline.streams.layout.component.TopologyLayout;
import org.apache.streamline.streams.metrics.storm.topology.StormMetricsConstant;

import java.util.List;
import java.util.Map;

public class StormTopologyUtil {
    private StormTopologyUtil() {
    }

    public static String generateStormTopologyName(TopologyLayout topology) {
        return "streamline-" + topology.getId() + "-" + topology.getName();
    }

    public static String generateUniqueStormTopologyNamePrefix(TopologyLayout topology) {
        return "streamline-" + topology.getId() + "-";
    }

    public static String findStormTopologyId(StormRestAPIClient client, TopologyLayout topology) {
        String topologyNamePrefix = generateUniqueStormTopologyNamePrefix(topology);
        Map<?, ?> summaryMap = client.getTopologySummary();
        List<Map<?, ?>> topologies = (List<Map<?, ?>>) summaryMap.get(StormMetricsConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGIES);
        String topologyId = null;
        for (Map<?, ?> topologyMap : topologies) {
            String topologyNameForStorm = (String) topologyMap.get(StormMetricsConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGY_NAME);

            if (topologyNameForStorm.startsWith(topologyNamePrefix)) {
                topologyId = (String) topologyMap.get(StormMetricsConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGY_ID_ENCODED);
                break;
            }
        }
        return topologyId;
    }

    public static String findStormCompleteTopologyName(StormRestAPIClient client, TopologyLayout topology) {
        String topologyNamePrefix = generateUniqueStormTopologyNamePrefix(topology);
        Map<?, ?> summaryMap = client.getTopologySummary();
        List<Map<?, ?>> topologies = (List<Map<?, ?>>) summaryMap.get(StormMetricsConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGIES);
        String topologyName = null;
        for (Map<?, ?> topologyMap : topologies) {
            String topologyNameForStorm = (String) topologyMap.get(StormMetricsConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGY_NAME);

            if (topologyNameForStorm.startsWith(topologyNamePrefix)) {
                topologyName = topologyNameForStorm;
                break;
            }
        }
        return topologyName;
    }

    public static String findOrGenerateTopologyName(StormRestAPIClient client, TopologyLayout topology) {
        String topologyName = findStormCompleteTopologyName(client, topology);
        if (topologyName == null) {
            topologyName = generateStormTopologyName(topology);
        }
        return topologyName;
    }
}
