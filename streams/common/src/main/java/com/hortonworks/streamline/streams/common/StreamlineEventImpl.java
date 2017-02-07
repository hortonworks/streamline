/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/
package com.hortonworks.streamline.streams.common;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.hortonworks.streamline.streams.StreamlineEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A default implementation of StreamlineEvent.
 */
public final class StreamlineEventImpl extends ForwardingMap<String, Object> implements StreamlineEvent {
    // Default value chosen to be blank and not the default used in storm since wanted to keep it independent of storm.
    public final static String DEFAULT_SOURCE_STREAM = "default";
    // special event to trigger evaluation of group by
    public static final StreamlineEvent GROUP_BY_TRIGGER_EVENT = new StreamlineEventImpl(Collections.emptyMap(), "");

    private final Map<String, Object> header;
    private final String sourceStream;
    private final Map<String, Object> auxiliaryFieldsAndValues;
    private final String dataSourceId;
    private final String id;
    private final ImmutableMap<String, Object> delegate;

    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }

    /**
     * Creates an StreamlineEvent with given keyValues, dataSourceId
     * and a random UUID as the id.
     */
    public StreamlineEventImpl(Map<String, Object> keyValues, String dataSourceId) {
        this(keyValues, dataSourceId, UUID.randomUUID().toString());
    }

    /**
     * Creates an StreamlineEvent with given keyValues, dataSourceId and id.
     */
    public StreamlineEventImpl(Map<String, Object> keyValues, String dataSourceId, String id) {
        this(keyValues, dataSourceId, id, new HashMap<>(), DEFAULT_SOURCE_STREAM);
    }

    /**
     * Creates an StreamlineEvent with given keyValues, dataSourceId and header.
     */
    public StreamlineEventImpl(Map<String, Object> keyValues, String dataSourceId, Map<String, Object> header) {
        this(keyValues, dataSourceId, UUID.randomUUID().toString(), header, DEFAULT_SOURCE_STREAM);
    }


    /**
     * Creates an StreamlineEvent with given keyValues, dataSourceId, id and header.
     */
    public StreamlineEventImpl(Map<String, Object> keyValues, String dataSourceId, String id, Map<String, Object> header) {
        this(keyValues, dataSourceId, id, header, DEFAULT_SOURCE_STREAM);
    }

    /**
     * Creates an StreamlineEvent with given keyValues, dataSourceId, id and header and sourceStream.
     */
    public StreamlineEventImpl(Map<String, Object> fieldsAndValues, String dataSourceId, String id, Map<String, Object> header, String sourceStream) {
        this(fieldsAndValues, dataSourceId, id, header, sourceStream, null);
    }

    /**
     * Creates an StreamlineEvent with given keyValues, dataSourceId, id and header, sourceStream and auxiliary fields.
     * Creates an StreamlineEvent with given keyValues, dataSourceId, header and sourceStream.
     */
    public StreamlineEventImpl(Map<String, Object> keyValues, String dataSourceId, Map<String, Object> header, String sourceStream) {
        this(keyValues, dataSourceId, UUID.randomUUID().toString(), header, sourceStream);
    }

    /**
     * Creates an StreamlineEvent with given keyValues, dataSourceId, id, header and sourceStream.
     */
    public StreamlineEventImpl(Map<String, Object> keyValues, String dataSourceId, String id, Map<String, Object> header, String sourceStream, Map<String, Object> auxiliaryFieldsAndValues) {
        this.delegate = ImmutableMap.copyOf(keyValues);
        this.dataSourceId = dataSourceId;
        this.id = id;
        this.header = header;
        this.sourceStream = sourceStream;
        this.auxiliaryFieldsAndValues = auxiliaryFieldsAndValues != null ? new HashMap<>(auxiliaryFieldsAndValues) : new HashMap<>();
    }

    public StreamlineEventImpl(StreamlineEventImpl other) {
        this.header = other.header;
        this.sourceStream = other.sourceStream;
        this.auxiliaryFieldsAndValues = new HashMap<>(other.auxiliaryFieldsAndValues);
        this.dataSourceId = other.dataSourceId;
        this.id = other.id;
        this.delegate = ImmutableMap.copyOf(other.delegate);
    }

    /*
     * Creates a copy of 'other' but with the given keyValues.
     */
    private StreamlineEventImpl(StreamlineEventImpl other, ImmutableMap<String, Object> keyValues) {
        this.header = other.header;
        this.sourceStream = other.sourceStream;
        this.auxiliaryFieldsAndValues = new HashMap<>(other.auxiliaryFieldsAndValues);
        this.dataSourceId = other.dataSourceId;
        this.id = other.id;
        this.delegate = ImmutableMap.copyOf(keyValues);
    }

    @Override
    public Map<String, Object> getAuxiliaryFieldsAndValues() {
        return auxiliaryFieldsAndValues;
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
        String res = dataSourceId;
        if (res == null) {
            Object dataSourceIds = header.get("dataSourceIds");
            if (dataSourceIds instanceof List) {
                res = Joiner.on(",").join(Collections2.filter((List) dataSourceIds, new Predicate() {
                    @Override
                    public boolean apply(Object input) {
                        return input != null;
                    }
                }));
            }
        }
        return res;
    }

    @Override
    public String getSourceStream() {
        return sourceStream;
    }


    /**
     * Returns a new Streamline event with the given fieldsAndValues added to the existing fieldsAndValues
     *
     * @param fieldsAndValues the map of fieldsAndValues to add
     * @return the new StreamlineEvent
     */
    @Override
    public StreamlineEvent addFieldsAndValues(Map<String, Object> fieldsAndValues) {
        Objects.requireNonNull(fieldsAndValues, "keyValues is null");
        ImmutableMap<String, Object> kv = ImmutableMap.<String, Object>builder()
                .putAll(delegate).putAll(fieldsAndValues).build();
        return new StreamlineEventImpl(this, kv);
    }

    @Override
    public StreamlineEvent addFieldAndValue(String key, Object value) {
        return addFieldsAndValues(Collections.singletonMap(key, value));
    }

    /**
     * Returns a new Streamline event with the given headers added to the existing headers.
     * All the other fields are copied from this event.
     * @param headers the map of fieldsAndValues to add or overwrite
     * @return the new StreamlineEvent
     */
    @Override
    public StreamlineEvent addHeaders(Map<String, Object> headers) {
        StreamlineEventImpl result = new StreamlineEventImpl(this);
        result.header.putAll(headers);
        return result;
    }

    @Override
    public Map<String, Object> getHeader() {
        return header;
    }

    @Override
    public byte[] getBytes() {
        return this.toString().getBytes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StreamlineEventImpl that = (StreamlineEventImpl) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public final Object put(String k, Object v) {
        return StreamlineEvent.super.put(k, v);
    }

    /**
     * {@inheritDoc}
     */
    public final Object remove(Object o) {
        return StreamlineEvent.super.remove(o);
    }

    /**
     * {@inheritDoc}
     */
    public final void putAll(Map<? extends String, ? extends Object> map) {
        StreamlineEvent.super.putAll(map);
    }

    /**
     * {@inheritDoc}
     */
    public final void clear() {
        StreamlineEvent.super.clear();
    }


    @Override
    public String toString() {
        return "StreamlineEventImpl{" +
                "header=" + header +
                ", sourceStream='" + sourceStream + '\'' +
                ", fieldsAndValues=" + super.toString() +
                ", auxiliaryFieldsAndValues=" + auxiliaryFieldsAndValues +
                ", dataSourceId='" + dataSourceId + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
