package com.hortonworks.iotas.common;

import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.UUID;

/**
 * A default implementation of IotasEvent.
 */
public class IotasEventImpl implements IotasEvent {

    private final Map<String, Object> map;
    private final String id;
    private String dataSourceId = StringUtils.EMPTY;

    public IotasEventImpl(Map<String, Object> keyValues) {
        this(keyValues, UUID.randomUUID().toString());
    }

    public IotasEventImpl(Map<String, Object> keyValues, String id) {
        this.map = keyValues;
        this.id = id;
    }

    public IotasEventImpl withDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
        return this;
    }

    @Override
    public Map<String, Object> getFieldsAndValues() {
        return map;
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
                "map=" + map +
                ", id='" + id + '\'' +
                ", dataSourceId='" + dataSourceId + '\'' +
                '}';
    }
}
