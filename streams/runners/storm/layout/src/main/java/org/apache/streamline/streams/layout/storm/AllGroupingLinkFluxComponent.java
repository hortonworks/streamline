package org.apache.streamline.streams.layout.storm;

import java.util.Map;

/*
All grouping link is when a tuple is sent to every instance of the downstream bolt
 */
public class AllGroupingLinkFluxComponent extends LinkFluxComponent {
    @Override
    protected void generateComponent() {
        super.generateComponent();
        Map grouping = getGroupingYamlForType(StormTopologyLayoutConstants
                .YAML_KEY_ALL_GROUPING);
        updateLinkComponentWithGrouping(grouping);
    }
}
