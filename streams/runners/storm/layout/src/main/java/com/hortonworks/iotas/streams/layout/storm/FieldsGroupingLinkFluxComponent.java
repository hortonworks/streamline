package com.hortonworks.iotas.streams.layout.storm;

import com.hortonworks.iotas.streams.layout.ConfigFieldValidation;
import com.hortonworks.iotas.streams.layout.TopologyLayoutConstants;
import com.hortonworks.iotas.streams.layout.exception.BadTopologyLayoutException;

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
    public void validateConfig () throws BadTopologyLayoutException {
        super.validateConfig();
        String fieldName = TopologyLayoutConstants.JSON_KEY_GROUPING_FIELDS;
        Object value = conf.get(fieldName);
        if (!ConfigFieldValidation.isList(value)) {
            throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
        }
        List groupingFields = (List) value;
        int listLength = groupingFields.size();
        if (listLength == 0) {
            throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
        }
        for (Object groupingField : groupingFields) {
            if (!ConfigFieldValidation.isStringAndNotEmpty(groupingField)) {
                throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
            }
        }
    }
}
