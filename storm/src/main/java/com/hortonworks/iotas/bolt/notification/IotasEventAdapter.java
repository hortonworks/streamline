/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.hortonworks.iotas.bolt.notification;

import com.hortonworks.iotas.streams.IotasEvent;
import com.hortonworks.iotas.streams.notification.Notification;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_EVENT_IDS;
import static com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_DATASOURCE_IDS;
import static com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_RULE_ID;
import static com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_NOTIFIER_NAME;
import static com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransformRuntime.HEADER_FIELD_TIMESTAMP;

/**
 * Adapts {@link IotasEvent} to {@link com.hortonworks.iotas.streams.notification.Notification}
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
