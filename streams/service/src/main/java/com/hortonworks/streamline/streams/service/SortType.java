package com.hortonworks.streamline.streams.service;

public enum SortType {
    NAME("name"),
    TIMESTAMP("timestamp");

    private final String fieldName;

    SortType(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
