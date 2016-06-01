package com.hortonworks.iotas.bolt.notification;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.notification.common.Notification;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_EVENT_IDS;
import static com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_DATASOURCE_IDS;
import static com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_RULE_ID;
import static com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_NOTIFIER_NAME;
import static com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_TIMESTAMP;

/**
 * Adapts {@link com.hortonworks.iotas.common.IotasEvent} to {@link com.hortonworks.iotas.notification.common.Notification}
 */
public class IotasEventAdapter implements Notification {
    private final IotasEvent iotasEvent;

    public IotasEventAdapter(IotasEvent iotasEvent) {
        this.iotasEvent = iotasEvent;
    }

    @Override
    public String getId() {
        return iotasEvent.getId();
    }

    @Override
    public List<String> getEventIds() {
        List<String> header = getHeader(HEADER_FIELD_EVENT_IDS);
        return header != null ? header : Collections.<String>emptyList();
    }

    @Override
    public List<String> getDataSourceIds() {
        List<String> header = getHeader(HEADER_FIELD_DATASOURCE_IDS);
        return header != null ? header : Collections.<String>emptyList();
    }

    @Override
    public String getRuleId() {
        Long header = getHeader(HEADER_FIELD_RULE_ID);
        return header != null ? header.toString() : "";
    }

    @Override
    public Status getStatus() {
        return Status.NEW;
    }

    @Override
    public Map<String, Object> getFieldsAndValues() {
        return iotasEvent.getFieldsAndValues();
    }

    @Override
    public String getNotifierName() {
        String header = getHeader(HEADER_FIELD_NOTIFIER_NAME);
        return header != null ? header : "";
    }

    @Override
    public long getTs() {
        Long header = getHeader(HEADER_FIELD_TIMESTAMP);
        return header != null ? header : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IotasEventAdapter that = (IotasEventAdapter) o;

        return iotasEvent != null ? iotasEvent.equals(that.iotasEvent) : that.iotasEvent == null;

    }

    @Override
    public int hashCode() {
        return iotasEvent != null ? iotasEvent.hashCode() : 0;
    }

    private <T> T getHeader(String key) {
        return (T) iotasEvent.getHeader().get(key);
    }
}
