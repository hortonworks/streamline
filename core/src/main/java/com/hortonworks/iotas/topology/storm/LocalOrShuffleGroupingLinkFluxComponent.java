package com.hortonworks.iotas.topology.storm;

import com.hortonworks.iotas.topology.TopologyLayoutConstants;

import java.util.Map;

public class LocalOrShuffleGroupingLinkFluxComponent extends LinkFluxComponent {

    @Override
    protected void generateComponent() {
        super.generateComponent();
        Map grouping = getGroupingYamlForType(TopologyLayoutConstants.YAML_KEY_LOCAL_OR_SHUFFLE_GROUPING);
        updateLinkComponentWithGrouping(grouping);
    }
}
