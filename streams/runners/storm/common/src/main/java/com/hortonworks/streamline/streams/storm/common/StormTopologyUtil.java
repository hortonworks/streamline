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

    public static String findStormTopologyId(StormRestAPIClient client, Long topologyId, String asUser) {
        String topologyNamePrefix = generateUniqueStormTopologyNamePrefix(topologyId);
        Map<?, ?> summaryMap = client.getTopologySummary(asUser);
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

    public static String findStormCompleteTopologyName(StormRestAPIClient client, Long topologyId, String asUser) {
        String topologyNamePrefix = generateUniqueStormTopologyNamePrefix(topologyId);
        Map<?, ?> summaryMap = client.getTopologySummary(asUser);
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

    public static String findOrGenerateTopologyName(StormRestAPIClient client, Long topologyId, String topologyName, String asUser) {
        String actualTopologyName = findStormCompleteTopologyName(client, topologyId, asUser);
        if (actualTopologyName == null) {
            actualTopologyName = generateStormTopologyName(topologyId, topologyName);
        }
        return actualTopologyName;
    }
}
