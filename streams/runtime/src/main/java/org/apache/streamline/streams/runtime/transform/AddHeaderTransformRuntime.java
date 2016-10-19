package org.apache.streamline.streams.runtime.transform;


import org.apache.streamline.streams.IotasEvent;
import org.apache.streamline.streams.common.IotasEventImpl;
import org.apache.streamline.streams.layout.Transform;
import org.apache.streamline.streams.layout.component.rule.action.transform.AddHeaderTransform;
import org.apache.streamline.streams.runtime.RuntimeService;
import org.apache.streamline.streams.runtime.TransformRuntime;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds a fixed header to the input event
 */
public class AddHeaderTransformRuntime implements TransformRuntime {
    public static final String HEADER_FIELD_NOTIFIER_NAME = "notifierName";
    public static final String HEADER_FIELD_RULE_ID = "ruleId";
    public static final String HEADER_FIELD_DATASOURCE_IDS = "dataSourceIds";
    public static final String HEADER_FIELD_EVENT_IDS = "eventIds";
    public static final String HEADER_FIELD_TIMESTAMP = "ts";

    private final AddHeaderTransform addHeaderTransform;


    public AddHeaderTransformRuntime(AddHeaderTransform addHeaderTransform) {
        this.addHeaderTransform = addHeaderTransform;
    }

    @Override
    public List<IotasEvent> execute(IotasEvent input) {
        Map<String, Object> header = new HashMap<>();
        if(addHeaderTransform.getFixedHeader() != null) {
            header.putAll(addHeaderTransform.getFixedHeader());
        }
        header.put(HEADER_FIELD_DATASOURCE_IDS, Collections.singletonList(input.getDataSourceId()));
        header.put(HEADER_FIELD_EVENT_IDS, Collections.singletonList(input.getId()));
        header.put(HEADER_FIELD_TIMESTAMP, System.currentTimeMillis());
        return Collections.<IotasEvent>singletonList(
                new IotasEventImpl(input.getFieldsAndValues(), input.getDataSourceId(), header));
    }

    @Override
    public String toString() {
        return "AddHeaderTransformRuntime{" +
                "addHeaderTransform=" + addHeaderTransform +
                '}';
    }

    public static class Factory implements RuntimeService.Factory<TransformRuntime, Transform> {

        @Override
        public TransformRuntime create(Transform transform) {
            return new AddHeaderTransformRuntime((AddHeaderTransform) transform);
        }
    }
}
