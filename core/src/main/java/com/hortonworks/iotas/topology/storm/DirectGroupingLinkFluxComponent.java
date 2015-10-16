package com.hortonworks.iotas.topology.storm;

import com.hortonworks.iotas.topology.TopologyLayoutConstants;

import java.util.Map;

/*
Direct grouping is when the producer decides which task of the downstream bolt will get the tuple
 */
public class DirectGroupingLinkFluxComponent extends LinkFluxComponent {
    @Override
    protected void generateComponent() {
        super.generateComponent();
        Map grouping = getGroupingYamlForType(TopologyLayoutConstants
                .YAML_KEY_DIRECT_GROUPING);
        updateLinkComponentWithGrouping(grouping);
    }
}
