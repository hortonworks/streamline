package com.hortonworks.streamline.streams.storm.common;

import java.util.List;
import java.util.Map;

public class StormTopologyUtil {
    private StormTopologyUtil() {
    }

    public static String generateStormTopologyName(Long topologyId, String topologyName) {
        return "streamline-" + topologyId + "-" + topologyName;
    }

    public static String generateUniqueStormTopologyNamePrefix(Long topologyId) {
        return "streamline-" + topologyId + "-";
    }

    public static String findStormTopologyId(StormRestAPIClient client, Long topologyId) {
        String topologyNamePrefix = generateUniqueStormTopologyNamePrefix(topologyId);
        Map<?, ?> summaryMap = client.getTopologySummary();
        List<Map<?, ?>> topologies = (List<Map<?, ?>>) summaryMap.get(StormRestAPIConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGIES);
        String stormTopologyId = null;
        for (Map<?, ?> topologyMap : topologies) {
            String topologyNameForStorm = (String) topologyMap.get(StormRestAPIConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGY_NAME);

            if (topologyNameForStorm.startsWith(topologyNamePrefix)) {
                stormTopologyId = (String) topologyMap.get(StormRestAPIConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGY_ID_ENCODED);
                break;
            }
        }
        return stormTopologyId;
    }

    public static String findStormCompleteTopologyName(StormRestAPIClient client, Long topologyId) {
        String topologyNamePrefix = generateUniqueStormTopologyNamePrefix(topologyId);
        Map<?, ?> summaryMap = client.getTopologySummary();
        List<Map<?, ?>> topologies = (List<Map<?, ?>>) summaryMap.get(StormRestAPIConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGIES);
        String topologyName = null;
        for (Map<?, ?> topologyMap : topologies) {
            String topologyNameForStorm = (String) topologyMap.get(StormRestAPIConstant.TOPOLOGY_SUMMARY_JSON_TOPOLOGY_NAME);

            if (topologyNameForStorm.startsWith(topologyNamePrefix)) {
                topologyName = topologyNameForStorm;
                break;
            }
        }
        return topologyName;
    }

    public static String findOrGenerateTopologyName(StormRestAPIClient client, Long topologyId, String topologyName) {
        String actualTopologyName = findStormCompleteTopologyName(client, topologyId);
        if (actualTopologyName == null) {
            actualTopologyName = generateStormTopologyName(topologyId, topologyName);
        }
        return actualTopologyName;
    }
}
