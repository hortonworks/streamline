package com.hortonworks.iotas.streams.layout.storm;

import java.util.Map;

public class ShuffleGroupingLinkFluxComponent extends LinkFluxComponent {
    @Override
    protected void generateComponent() {
        super.generateComponent();
        Map grouping = getGroupingYamlForType(StormTopologyLayoutConstants
                .YAML_KEY_SHUFFLE_GROUPING);
        updateLinkComponentWithGrouping(grouping);
    }
}
