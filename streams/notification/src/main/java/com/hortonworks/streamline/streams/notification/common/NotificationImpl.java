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

package com.hortonworks.streamline.streams.notification.common;

import com.hortonworks.streamline.streams.notification.Notification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A notification object that carries the necessary info from rule engine to the notifier
 * for sending out notifications to external systems.
 */
public class NotificationImpl implements Notification {

    private final String id;
    private final Map<String, Object> fieldsAndValues;
    private final List<String> eventIds;
    private final List<String> dataSourceIds;
    private final String ruleId;
    private final Status status;
    private final String notifierName;
    private final long timestamp;

    /**
     * Notification builder
     */
    public static class Builder {
        private String id;
        private final Map<String, Object> fieldsAndValues;
        private List<String> eventIds;
        private List<String> dataSourceIds;
        private String ruleId;
        private Status status;
        private String notifierName;
        private long timestamp;

        public Builder(Map<String, Object> fieldsAndValues) {
            this.fieldsAndValues = fieldsAndValues;
        }

        /**
         * copy ctor
         */
        public Builder(Notification notification) {
            this.id = notification.getId();
            this.fieldsAndValues = notification.getFieldsAndValues();
            this.eventIds = notification.getEventIds();
            this.dataSourceIds = notification.getDataSourceIds();
            this.ruleId = notification.getRuleId();
            this.status = notification.getStatus();
            this.notifierName = notification.getNotifierName();
            this.timestamp = notification.getTs();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder eventIds(List<String> eventIds) {
            this.eventIds = eventIds;
            return this;
        }

        public Builder dataSourceIds(List<String> dataSourceIds) {
            this.dataSourceIds = dataSourceIds;
            return this;
        }

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder notifierName(String notifierName) {
            this.notifierName = notifierName;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public NotificationImpl build() {
            return new NotificationImpl(this);
        }
    }

    private NotificationImpl(Builder builder) {
        this.id = (builder.id != null) ? builder.id : UUID.randomUUID().toString();
        this.fieldsAndValues = builder.fieldsAndValues;
        this.eventIds = builder.eventIds;
        this.dataSourceIds = builder.dataSourceIds;
        this.ruleId = builder.ruleId;
        this.status = (builder.status != null) ? builder.status : Status.NEW;
        this.notifierName = builder.notifierName;
        this.timestamp = (builder.timestamp == 0) ? System.currentTimeMillis() : builder.timestamp;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<String> getEventIds() {
        return eventIds;
    }

    @Override
    public List<String> getDataSourceIds() {
        return dataSourceIds;
    }

    @Override
    public String getRuleId() {
        return ruleId;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public Map<String, Object> getFieldsAndValues() {
        return fieldsAndValues;
    }

    @Override
    public String getNotifierName() {
        return notifierName;
    }

    @Override
    public long getTs() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "NotificationImpl{" +
                "id='" + id + '\'' +
                ", fieldsAndValues=" + fieldsAndValues +
                ", eventIds=" + eventIds +
                ", dataSourceIds=" + dataSourceIds +
                ", ruleId='" + ruleId + '\'' +
                ", status=" + status +
                ", notifierName='" + notifierName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationImpl)) return false;

        NotificationImpl that = (NotificationImpl) o;

        if (timestamp != that.timestamp) return false;
        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getFieldsAndValues() != null ? !getFieldsAndValues().equals(that.getFieldsAndValues()) : that.getFieldsAndValues() != null)
            return false;
        if (getEventIds() != null ? !getEventIds().equals(that.getEventIds()) : that.getEventIds() != null)
            return false;
        if (getDataSourceIds() != null ? !getDataSourceIds().equals(that.getDataSourceIds()) : that.getDataSourceIds() != null)
            return false;
        if (getRuleId() != null ? !getRuleId().equals(that.getRuleId()) : that.getRuleId() != null) return false;
        if (getStatus() != that.getStatus()) return false;
        return getNotifierName() != null ? getNotifierName().equals(that.getNotifierName()) : that.getNotifierName() == null;

    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getFieldsAndValues() != null ? getFieldsAndValues().hashCode() : 0);
        result = 31 * result + (getEventIds() != null ? getEventIds().hashCode() : 0);
        result = 31 * result + (getDataSourceIds() != null ? getDataSourceIds().hashCode() : 0);
        result = 31 * result + (getRuleId() != null ? getRuleId().hashCode() : 0);
        result = 31 * result + (getStatus() != null ? getStatus().hashCode() : 0);
        result = 31 * result + (getNotifierName() != null ? getNotifierName().hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
