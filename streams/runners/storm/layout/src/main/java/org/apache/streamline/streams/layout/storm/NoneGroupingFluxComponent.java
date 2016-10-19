package org.apache.streamline.streams.layout.storm;

import java.util.Map;

public class NoneGroupingFluxComponent extends LinkFluxComponent {
    @Override
    protected void generateComponent() {
        super.generateComponent();
        Map grouping = getGroupingYamlForType(StormTopologyLayoutConstants
                .YAML_KEY_NONE_GROUPING);
        updateLinkComponentWithGrouping(grouping);
    }
}
