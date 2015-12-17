package com.hortonworks.iotas.topology.storm;

import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.util.exception.BadTopologyLayoutException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation for flux streams
 */
public class LinkFluxComponent extends AbstractFluxComponent {

    @Override
    protected void generateComponent () {
        String linkName = "link" + UUID_FOR_COMPONENTS;
        component.put(TopologyLayoutConstants.YAML_KEY_NAME, linkName);
        component.put(TopologyLayoutConstants.YAML_KEY_FROM, conf.get
                (TopologyLayoutConstants.JSON_KEY_FROM));
        component.put(TopologyLayoutConstants.YAML_KEY_TO, conf.get
                (TopologyLayoutConstants.JSON_KEY_TO));
    }

    protected void updateLinkComponentWithGrouping (Map groupingInfo) {
        if (groupingInfo != null && !groupingInfo.isEmpty()) {
            component.put(TopologyLayoutConstants.YAML_KEY_GROUPING, groupingInfo);
        }
    }

    protected Map getGroupingYamlForType (String groupingType) {
        Map grouping = new LinkedHashMap();
        grouping.put(TopologyLayoutConstants.YAML_KEY_TYPE, groupingType);
        if (conf.get(TopologyLayoutConstants.JSON_KEY_STREAM_ID) != null) {
            grouping.put(TopologyLayoutConstants.YAML_KEY_STREAM_ID, conf.get(TopologyLayoutConstants.JSON_KEY_STREAM_ID));
        }
        return grouping;
    }

    @Override
    public void validateConfig () throws BadTopologyLayoutException {
        validateStringFields();
    }

    private void validateStringFields () throws BadTopologyLayoutException {
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
