package com.hortonworks.iotas.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A default implementation of IotasEvent.
 */
public class IotasEventImpl implements IotasEvent {
    // Default value chosen to be blank and not the default used in storm since wanted to keep it independent of storm.
    public final static String DEFAULT_SOURCE_STREAM = "default";
    // special event to trigger evaluation of group by
    public static final IotasEvent GROUP_BY_TRIGGER_EVENT = new IotasEventImpl(null, null);

    private final Map<String, Object> header;
    private final String sourceStream;
    private final Map<String, Object> fieldsAndValues;
    private final Map<String, Object> auxiliaryFieldsAndValues;
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
        this(keyValues, dataSourceId, id, Collections.<String, Object>emptyMap(), DEFAULT_SOURCE_STREAM);
    }

    /**
     * Creates an IotasEvent with given keyValues, dataSourceId and header.
     */
    public IotasEventImpl(Map<String, Object> keyValues, String dataSourceId, Map<String, Object> header) {
        this(keyValues, dataSourceId, UUID.randomUUID().toString(), header, DEFAULT_SOURCE_STREAM);
    }

    /**
     * Creates an IotasEvent with given keyValues, dataSourceId, id and header.
     */
    public IotasEventImpl(Map<String, Object> keyValues, String dataSourceId, String id, Map<String, Object> header) {
        this(keyValues, dataSourceId, id, header, DEFAULT_SOURCE_STREAM);
    }


    /**
     * Creates an IotasEvent with given keyValues, dataSourceId, id and header and sourceStream.
     */
    public IotasEventImpl(Map<String, Object> fieldsAndValues, String dataSourceId, String id, Map<String, Object> header, String sourceStream) {
        this(fieldsAndValues, dataSourceId, id, header, sourceStream, null);
    }

    /**
     * Creates an IotasEvent with given keyValues, dataSourceId, id and header, sourceStream and auxiliary fields.
     * Creates an IotasEvent with given keyValues, dataSourceId, header and sourceStream.
     */
    public IotasEventImpl(Map<String, Object> keyValues, String dataSourceId, Map<String, Object> header, String sourceStream) {
        this(keyValues, dataSourceId, UUID.randomUUID().toString(), header, sourceStream);
    }

    /**
     * Creates an IotasEvent with given keyValues, dataSourceId, id, header and sourceStream.
     */
    public IotasEventImpl(Map<String, Object> keyValues, String dataSourceId, String id, Map<String, Object> header, String sourceStream, Map<String, Object> auxiliaryFieldsAndValues) {
        this.fieldsAndValues = keyValues;
        this.dataSourceId = dataSourceId;
        this.id = id;
        this.header = header;
        this.sourceStream = sourceStream;
        this.auxiliaryFieldsAndValues = (auxiliaryFieldsAndValues != null ? new HashMap<>(auxiliaryFieldsAndValues) : new HashMap<String, Object>());
    }


    @Override
    public Map<String, Object> getFieldsAndValues() {
        return Collections.unmodifiableMap(fieldsAndValues);
    }

    @Override
    public Map<String, Object> getAuxiliaryFieldsAndValues() {
        return Collections.unmodifiableMap(auxiliaryFieldsAndValues);
    }

    public void addAuxiliaryFieldAndValue(String field, Object value) {
        auxiliaryFieldsAndValues.put(field, value);
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
    public String getSourceStream() {
        return sourceStream;
    }

    /**
     * Returns a new Iotas event with the given fieldsAndValues added to the existing fieldsAndValues
     *
     * @param fieldsAndValues the map of fieldsAndValues to add
     * @return the new IotasEvent
     */
    @Override
    public IotasEvent addFieldsAndValues(Map<String, Object> fieldsAndValues) {
        Map<String, Object> kv = new HashMap<>();
        kv.putAll(getFieldsAndValues());
        kv.putAll(fieldsAndValues);
        return new IotasEventImpl(kv, this.getDataSourceId(), this.getHeader(), this.getSourceStream());
    }

    /**
     * Returns a new Iotas event with the given headers added to the existing headers.
     * All the other fields are copied from this event.
     *
     * @param headers the map of fieldsAndValues to add or overwrite
     * @return the new IotasEvent
     */
    @Override
    public IotasEvent addHeaders(Map<String, Object> headers) {
        Map<String, Object> kv = new HashMap<>();
        kv.putAll(getHeader());
        kv.putAll(headers);
        return new IotasEventImpl(this.getFieldsAndValues(), this.getDataSourceId(), kv, this.getSourceStream());
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
                "header=" + header +
                ", sourceStream='" + sourceStream + '\'' +
                ", fieldsAndValues=" + fieldsAndValues +
                ", auxiliaryFieldsAndValues=" + auxiliaryFieldsAndValues +
                ", dataSourceId='" + dataSourceId + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
