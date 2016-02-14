package com.hortonworks.iotas.notification.common;

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
        private Map<String, Object> fieldsAndValues;
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
}
