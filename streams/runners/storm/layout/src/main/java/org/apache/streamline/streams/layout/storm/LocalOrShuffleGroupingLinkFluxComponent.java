package org.apache.streamline.streams.layout.storm;

import java.util.Map;

public class LocalOrShuffleGroupingLinkFluxComponent extends LinkFluxComponent {

    @Override
    protected void generateComponent() {
        super.generateComponent();
        Map grouping = getGroupingYamlForType(StormTopologyLayoutConstants.YAML_KEY_LOCAL_OR_SHUFFLE_GROUPING);
        updateLinkComponentWithGrouping(grouping);
    }
}
