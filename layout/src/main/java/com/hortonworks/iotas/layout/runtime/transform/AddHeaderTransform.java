package com.hortonworks.iotas.layout.runtime.transform;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Adds a fixed header to the input event
 */
public class AddHeaderTransform implements Transform {
    private final Map<String, Object> header;

    public AddHeaderTransform(Map<String, Object> header) {
        this.header = header;
    }

    @Override
    public List<IotasEvent> execute(IotasEvent input) {
        return Collections.<IotasEvent>singletonList(
                new IotasEventImpl(input.getFieldsAndValues(), input.getDataSourceId(), header));
    }

    @Override
    public String toString() {
        return "AddHeaderTransform{" +
                "header=" + header +
                '}';
    }
}
