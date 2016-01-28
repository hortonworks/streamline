package com.hortonworks.iotas.layout.runtime.transform;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts some fields from the input IotasEvent.
 */
public class ProjectionTransform implements Transform {
    private final Map<String, Object> fieldsAndDefaults;

    /**
     * Selects the fields matching the keys from the given map
     * with the given defaults if the entry is not available in the IotasEvent
     *
     * @param fieldsAndDefaults the fields to select and their default values (or null)
     */
    public ProjectionTransform(Map<String, Object> fieldsAndDefaults) {
        this.fieldsAndDefaults = fieldsAndDefaults;
    }

    @Override
    public List<IotasEvent> execute(IotasEvent input) {
        return doTransform(input);
    }

    private List<IotasEvent> doTransform(IotasEvent input) {
        Map<String, Object> merged = new HashMap<>();
        for(Map.Entry<String, Object> entry: fieldsAndDefaults.entrySet()) {
            Object value = input.getFieldsAndValues().get(entry.getKey());
            if(value == null) {
                value = entry.getValue();
            }
            merged.put(entry.getKey(), value);
        }
        return Collections.<IotasEvent>singletonList(new IotasEventImpl(merged, input.getDataSourceId()));
    }

    @Override
    public String toString() {
        return "ProjectionTransform{" +
                "fieldsAndDefaults=" + fieldsAndDefaults +
                '}';
    }
}
