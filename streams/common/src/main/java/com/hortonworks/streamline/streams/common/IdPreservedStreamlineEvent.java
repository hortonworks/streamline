/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.streamline.streams.common;

import com.hortonworks.streamline.streams.StreamlineEvent;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * The implementation of StreamlineEvent which preserves event ID.
 *
 * Note that this class doesn't guarantee preserving event ID while modifying the event.
 * So if you would want to preserve event ID again, please wrap the returned StreamlineEvent with this class again.
 */
public class IdPreservedStreamlineEvent implements StreamlineEvent {
    private StreamlineEvent underlyingEvent;
    private String id;

    public IdPreservedStreamlineEvent(StreamlineEvent underlyingEvent, String id) {
        this.underlyingEvent = underlyingEvent;
        this.id = id;
    }

    @Override
    public Map<String, Object> getAuxiliaryFieldsAndValues() {
        return underlyingEvent.getAuxiliaryFieldsAndValues();
    }

    @Override
    public StreamlineEvent addAuxiliaryFieldAndValue(String field, Object value) {
        // note that returning event doesn't preserve ID
        return underlyingEvent.addAuxiliaryFieldAndValue(field, value);
    }

    @Override
    public Map<String, Object> getHeader() {
        return underlyingEvent.getHeader();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDataSourceId() {
        return underlyingEvent.getDataSourceId();
    }

    @Override
    public String getSourceStream() {
        return underlyingEvent.getSourceStream();
    }

    @Override
    public StreamlineEvent addFieldsAndValues(Map<String, Object> fieldsAndValues) {
        return underlyingEvent.addFieldsAndValues(fieldsAndValues);
    }

    @Override
    public StreamlineEvent addFieldAndValue(String key, Object value) {
        return underlyingEvent.addFieldAndValue(key, value);
    }

    @Override
    public StreamlineEvent addHeaders(Map<String, Object> headers) {
        return underlyingEvent.addHeaders(headers);
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
    public int size() {
        return underlyingEvent.size();
    }

    @Override
    public boolean isEmpty() {
        return underlyingEvent.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return underlyingEvent.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return underlyingEvent.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return underlyingEvent.get(key);
    }

    @Override
    public Set<String> keySet() {
        return underlyingEvent.keySet();
    }

    @Override
    public Collection<Object> values() {
        return underlyingEvent.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return underlyingEvent.entrySet();
    }

    @Override
    public String toString() {
        return "IdPreservedStreamlineEvent{" +
                "underlyingEvent=" + underlyingEvent +
                ", id='" + id + '\'' +
                '}';
    }
}
