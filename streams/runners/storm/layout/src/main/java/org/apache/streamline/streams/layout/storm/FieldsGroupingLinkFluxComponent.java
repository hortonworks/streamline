package org.apache.streamline.streams.layout.storm;

import org.apache.streamline.streams.layout.ConfigFieldValidation;
import org.apache.streamline.streams.layout.TopologyLayoutConstants;
import org.apache.streamline.streams.layout.exception.ComponentConfigException;

import java.util.List;
import java.util.Map;

public class FieldsGroupingLinkFluxComponent extends LinkFluxComponent {

    @Override
    protected void generateComponent() {
        super.generateComponent();
        Map grouping = getGroupingYamlForType(StormTopologyLayoutConstants
                .YAML_KEY_FIELDS_GROUPING);
        if (conf.get(TopologyLayoutConstants.JSON_KEY_GROUPING_FIELDS) != null) {
            grouping.put(StormTopologyLayoutConstants.YAML_KEY_ARGS, conf.get
                    (TopologyLayoutConstants.JSON_KEY_GROUPING_FIELDS));
        }
        updateLinkComponentWithGrouping(grouping);

    }

    @Override
    public void validateConfig () throws ComponentConfigException {
        super.validateConfig();
        String fieldName = TopologyLayoutConstants.JSON_KEY_GROUPING_FIELDS;
        Object value = conf.get(fieldName);
        if (!ConfigFieldValidation.isList(value)) {
            throw new ComponentConfigException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
        }
        List groupingFields = (List) value;
        int listLength = groupingFields.size();
        if (listLength == 0) {
            throw new ComponentConfigException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
        }
        for (Object groupingField : groupingFields) {
            if (!ConfigFieldValidation.isStringAndNotEmpty(groupingField)) {
                throw new ComponentConfigException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
            }
        }
    }
}
