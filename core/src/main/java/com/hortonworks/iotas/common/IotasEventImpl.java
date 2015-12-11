package com.hortonworks.iotas.common;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * A default implementation of IotasEvent.
 */
public class IotasEventImpl implements IotasEvent {
    private final Map<String, Object> header;
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

    /**
     * Creates an IotasEvent with given keyValues, dataSourceId and id.
     */
    public IotasEventImpl(Map<String, Object> keyValues, String dataSourceId, String id) {
        this(keyValues, dataSourceId, id, Collections.<String, Object>emptyMap());
    }

    /**
     * Creates an IotasEvent with given keyValues, dataSourceId and header.
     */
    public IotasEventImpl(Map<String, Object> keyValues, String dataSourceId, Map<String, Object> header) {
        this(keyValues, dataSourceId, UUID.randomUUID().toString(), header);
    }

    /**
     * Creates an IotasEvent with given keyValues, dataSourceId, id and header.
     */
    public IotasEventImpl(Map<String, Object> keyValues, String dataSourceId, String id, Map<String, Object> header) {
        this.fieldsAndValues = keyValues;
        this.dataSourceId = dataSourceId;
        this.id = id;
        this.header = header;
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
    public Map<String, Object> getHeader() {
        return Collections.unmodifiableMap(header);
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
