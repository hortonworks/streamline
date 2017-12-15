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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.hortonworks.streamline.streams.StreamlineEvent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A default implementation of StreamlineEvent.
 */
@JsonSerialize(as=StreamlineEventImpl.class)
public final class StreamlineEventImpl extends ForwardingMap<String, Object> implements StreamlineEvent {
    // Default value chosen to be blank and not the default used in storm since wanted to keep it independent of storm.
    public final static String DEFAULT_SOURCE_STREAM = "default";
    // special event to trigger evaluation of group by
    public static final StreamlineEvent GROUP_BY_TRIGGER_EVENT = StreamlineEventImpl.builder().build();

    public static final String TO_STRING_PREFIX = "StreamlineEvent";

    private final ImmutableMap<String, Object> header;
    private final String sourceStream;
    private final ImmutableMap<String, Object> auxiliaryFieldsAndValues;
    private final String dataSourceId;
    private final String id = UUID.randomUUID().toString();
    private final ImmutableMap<String, Object> delegate;

    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }

    private StreamlineEventImpl() {
        header = null;
        sourceStream = null;
        auxiliaryFieldsAndValues = null;
        dataSourceId = null;
        delegate = null;
    }

    /**
     * Creates an StreamlineEvent with given keyValues, dataSourceId, id, header and sourceStream.
     */
    private StreamlineEventImpl(Map<String, Object> keyValues, String dataSourceId, Map<String, Object> header,
                                String sourceStream, Map<String, Object> auxiliaryFieldsAndValues) {
        if (keyValues instanceof StreamlineEventImpl) {
            this.delegate = ImmutableMap.copyOf(((StreamlineEventImpl) keyValues).delegate());
        } else {
            this.delegate = ImmutableMap.copyOf(keyValues);
        }
        this.dataSourceId = dataSourceId;
        this.sourceStream = sourceStream;
        this.header = header != null ? ImmutableMap.copyOf(header) : ImmutableMap.of();
        this.auxiliaryFieldsAndValues = auxiliaryFieldsAndValues != null ? ImmutableMap.copyOf(auxiliaryFieldsAndValues) : ImmutableMap.of();
    }

    public static class Builder {
        private ImmutableMap.Builder<String, Object> kvBuilder;
        private Map<String, Object> kv;
        private Map<String, Object> header;
        private Map<String, Object> auxiliaryFieldsAndValues;
        private String sourceStream = DEFAULT_SOURCE_STREAM;
        private String dataSourceId = "";

        private Builder() {}

        public Builder from(StreamlineEvent other) {
            return this.header(other.getHeader())
                    .sourceStream(other.getSourceStream())
                    .dataSourceId(other.getDataSourceId())
                    .auxiliaryFieldsAndValues(other.getAuxiliaryFieldsAndValues())
                    .putAll(other);
        }

        public Builder header(Map<String, Object> header) {
            this.header = header;
            return this;
        }

        public Builder auxiliaryFieldsAndValues(Map<String, Object> auxiliaryFieldsAndValues) {
            this.auxiliaryFieldsAndValues = auxiliaryFieldsAndValues;
            return this;
        }

        public Builder sourceStream(String sourceStream) {
            this.sourceStream = sourceStream;
            return this;
        }

        public Builder dataSourceId(String dataSourceId) {
            this.dataSourceId = dataSourceId;
            return this;
        }

        public Builder put(String key, Object value) {
            if (kvBuilder == null) {
                kvBuilder = ImmutableMap.builder();
                if (kv != null) {
                    kvBuilder.putAll(kv);
                }
            }
            kvBuilder.put(key, value);
            return this;
        }

        public Builder putAll(Map<String, Object> fieldsAndValues) {
            if (kvBuilder == null) {
                // avoids unnecessary copy if the fieldsAndValues is already immutable.
                if (kv == null) {
                    kv = fieldsAndValues;
                } else {
                    kvBuilder = ImmutableMap.builder();
                    kvBuilder.putAll(kv).putAll(fieldsAndValues);
                }
            } else {
                kvBuilder.putAll(fieldsAndValues);
            }
            return this;
        }

        public Builder fieldsAndValues(Map<String, Object> fieldsAndValues) {
            if (kvBuilder == null) {
                // avoids unnecessary copy if the fieldsAndValues is already immutable.
                if (kv == null) {
                    kv = fieldsAndValues;
                } else {
                    kvBuilder = ImmutableMap.builder();
                    kvBuilder.putAll(fieldsAndValues);
                }
            } else {
                kvBuilder = ImmutableMap.builder();
                kvBuilder.putAll(fieldsAndValues);
            }
            return this;
        }

        public StreamlineEventImpl build() {
            Map<String, Object> fieldsAndValues;
            if (kvBuilder != null) {
                fieldsAndValues = kvBuilder.build();
            } else if (kv != null) {
                fieldsAndValues = kv;
            } else {
                fieldsAndValues = Collections.emptyMap();
            }
            Map<String, Object> header = this.header != null ? ImmutableMap.copyOf(this.header) : ImmutableMap.of();
            Map<String, Object> aux = this.auxiliaryFieldsAndValues != null ?
                    ImmutableMap.copyOf(this.auxiliaryFieldsAndValues) : ImmutableMap.of();

            return new StreamlineEventImpl(
                    fieldsAndValues,
                    this.dataSourceId,
                    header,
                    this.sourceStream,
                    aux);
        }

    }

    public static StreamlineEventImpl.Builder builder() {
        return new StreamlineEventImpl.Builder();
    }

    @Override
    public Map<String, Object> getAuxiliaryFieldsAndValues() {
        return auxiliaryFieldsAndValues;
    }

    @Override
    public StreamlineEvent addAuxiliaryFieldAndValue(String field, Object value) {
        Map<String, Object> aux = new HashMap<>();
        aux.putAll(this.getAuxiliaryFieldsAndValues());
        aux.put(field, value);

        return StreamlineEventImpl.builder().from(this)
                .auxiliaryFieldsAndValues(aux)
                .build();
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
        return builder().from(this).putAll(fieldsAndValues).build();
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
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.putAll(this.getHeader());
        headerMap.putAll(headers);

        return StreamlineEventImpl.builder().from(this)
                .header(headerMap)
                .build();
    }

    @Override
    public Map<String, Object> getHeader() {
        return header;
    }

    @Override
    public byte[] getBytes() {
        try {
            return this.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
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
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public final Object remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public final void putAll(Map<? extends String, ? extends Object> map) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    public static StreamlineEvent fromString(String s) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> event = mapper.readValue(
                    s.substring(s.indexOf(TO_STRING_PREFIX) + TO_STRING_PREFIX.length()),
                    new TypeReference<Map<String, Object>>() {});
            return StreamlineEventImpl.builder()
                    .header((Map<String, Object>) event.get("header"))
                    .sourceStream((String) event.get("sourceStream"))
                    .auxiliaryFieldsAndValues((Map<String, Object>) event.get("auxiliaryFieldsAndValues"))
                    .dataSourceId((String) event.get("dataSourceId"))
                    .putAll((Map<String, Object>) event.get("fieldsAndValues"))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("header", header);
            event.put("sourceStream", sourceStream);
            event.put("auxiliaryFieldsAndValues", auxiliaryFieldsAndValues);
            event.put("dataSourceId", dataSourceId);
            event.put("id", id);
            event.put("fieldsAndValues", delegate);
            return  TO_STRING_PREFIX + mapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
