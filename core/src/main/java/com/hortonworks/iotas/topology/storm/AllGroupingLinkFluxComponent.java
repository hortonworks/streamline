package com.hortonworks.iotas.topology.storm;

import com.hortonworks.iotas.topology.TopologyLayoutConstants;

import java.util.Map;

/*
All grouping link is when a tuple is sent to every instance of the downstream bolt
 */
public class AllGroupingLinkFluxComponent extends LinkFluxComponent {
    @Override
    protected void generateComponent() {
        super.generateComponent();
        Map grouping = getGroupingYamlForType(TopologyLayoutConstants
                .YAML_KEY_ALL_GROUPING);
        updateLinkComponentWithGrouping(grouping);
    }
}
