package com.hortonworks.iotas.streams.layout.storm;

import java.util.Map;

public class GlobalGroupingLinkFluxComponent extends LinkFluxComponent {
    @Override
    protected void generateComponent() {
        super.generateComponent();
        Map grouping = getGroupingYamlForType(StormTopologyLayoutConstants
                .YAML_KEY_GLOBAL_GROUPING);
        updateLinkComponentWithGrouping(grouping);
    }
}
