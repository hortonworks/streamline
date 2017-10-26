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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StormTopologyUtil {
    public static final String COMPONENT_NAME_STORM_AUXILIARY_PART_DELIMITER = "$$";
    public static final String REGEX_COMPONENT_NAME_STORM_AUXILIARY_PART_DELIMITER = "\\$\\$";
    public static final String REGEX_COMPONENT_ID_STORM_AUXILIARY_PART_DELIMITER = "\\.";

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
    
    public static String createComponentNameWithAuxPart(String streamlineComponentName, String auxiliaryPart) {
        return streamlineComponentName + COMPONENT_NAME_STORM_AUXILIARY_PART_DELIMITER + auxiliaryPart;
    }

    public static String extractStreamlineComponentName(String stormComponentId) {
        String[] splitted = stormComponentId.split("-");
        if (splitted.length <= 1) {
            throw new IllegalArgumentException("Invalid Storm component ID for Streamline: " + stormComponentId);
        }

        List<String> splittedList = Arrays.asList(splitted);
        String componentName = String.join("-", splittedList.subList(1, splittedList.size()));
        return componentName.split(REGEX_COMPONENT_NAME_STORM_AUXILIARY_PART_DELIMITER)[0];
    }

    public static String extractStreamlineComponentId(String stormComponentId) {
        // removes all starting from first '-'
        String componentId = stormComponentId.substring(0, stormComponentId.indexOf('-'));
        return componentId.split(REGEX_COMPONENT_ID_STORM_AUXILIARY_PART_DELIMITER)[0];
    }
}
