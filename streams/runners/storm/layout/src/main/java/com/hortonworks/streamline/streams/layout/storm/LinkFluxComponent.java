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
package com.hortonworks.streamline.streams.layout.storm;

import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.common.exception.ComponentConfigException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation for flux streams
 */
public class LinkFluxComponent extends AbstractFluxComponent {

    @Override
    protected void generateComponent () {
        String linkName = "link" + UUID_FOR_COMPONENTS;
        component.put(StormTopologyLayoutConstants.YAML_KEY_NAME, linkName);
        component.put(StormTopologyLayoutConstants.YAML_KEY_FROM, conf.get
                (TopologyLayoutConstants.JSON_KEY_FROM));
        component.put(StormTopologyLayoutConstants.YAML_KEY_TO, conf.get
                (TopologyLayoutConstants.JSON_KEY_TO));
    }

    protected void updateLinkComponentWithGrouping (Map<String, Object> groupingInfo) {
        if (groupingInfo != null && !groupingInfo.isEmpty()) {
            component.put(StormTopologyLayoutConstants.YAML_KEY_GROUPING, groupingInfo);
        }
    }

    protected Map<String, Object> getGroupingYamlForType (String groupingType) {
        Map<String, Object> grouping = new LinkedHashMap<>();
        grouping.put(StormTopologyLayoutConstants.YAML_KEY_TYPE, groupingType);
        if (conf.get(TopologyLayoutConstants.JSON_KEY_STREAM_ID) != null) {
            grouping.put(StormTopologyLayoutConstants.YAML_KEY_STREAM_ID, conf.get(TopologyLayoutConstants.JSON_KEY_STREAM_ID));
        }
        return grouping;
    }

    @Override
    public void validateConfig () throws ComponentConfigException {
        validateStringFields();
    }

    private void validateStringFields () throws ComponentConfigException {
        String[] requiredStringFields = {
            TopologyLayoutConstants.JSON_KEY_FROM,
            TopologyLayoutConstants.JSON_KEY_TO
        };
        validateStringFields(requiredStringFields, true);
        String[] optionalStringFields = {
            TopologyLayoutConstants.JSON_KEY_STREAM_ID
        };
        validateStringFields(optionalStringFields, false);
    }

}
