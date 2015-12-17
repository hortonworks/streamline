package com.hortonworks.iotas.topology.storm;

import com.hortonworks.iotas.topology.ConfigFieldValidation;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.util.exception.BadTopologyLayoutException;

import java.util.List;
import java.util.Map;

public class FieldsGroupingLinkFluxComponent extends LinkFluxComponent {

    @Override
    protected void generateComponent() {
        super.generateComponent();
        Map grouping = getGroupingYamlForType(TopologyLayoutConstants
                .YAML_KEY_FIELDS_GROUPING);
        if (conf.get(TopologyLayoutConstants.JSON_KEY_GROUPING_FIELDS) != null) {
            grouping.put(TopologyLayoutConstants.YAML_KEY_ARGS, conf.get
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
        for (int i = 0; i < listLength; ++i) {
            if (!ConfigFieldValidation.isStringAndNotEmpty(groupingFields.get(i))) {
                throw new BadTopologyLayoutException(String.format(TopologyLayoutConstants.ERR_MSG_MISSING_INVALID_CONFIG, fieldName));
            }
        }
    }
}
