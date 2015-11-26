package com.hortonworks.iotas.common;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * A default implementation of IotasEvent.
 */
public class IotasEventImpl implements IotasEvent {

    private final Map<String, Object> fieldsAndValues;
    private final String dataSourceId;
    private final String id;

    /**
     * Creates an IotasEvent with given keyValues, dataSourceId
     * and a random UUID as the id.
     */
    public IotasEventImpl(Map<String, Object> keyValues, String dataSourceId) {
        this(keyValues, dataSourceId, UUID.randomUUID().toString());
    }

    public IotasEventImpl(Map<String, Object> keyValues, String dataSourceId, String id) {
        this.fieldsAndValues = keyValues;
        this.dataSourceId = dataSourceId;
        this.id = id;
    }

    @Override
    public Map<String, Object> getFieldsAndValues() {
        return Collections.unmodifiableMap(fieldsAndValues);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDataSourceId() {
        return dataSourceId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IotasEventImpl that = (IotasEventImpl) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "IotasEventImpl{" +
                "fieldsAndValues=" + fieldsAndValues +
                ", id='" + id + '\'' +
                ", dataSourceId='" + dataSourceId + '\'' +
                '}';
    }
}
