package com.hortonworks.iotas.notification.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation for representing user defined
 * query criteria.
 */
public class CriteriaImpl<T> implements Criteria<T> {
    private final Class<T> clazz;
    private List<Field> fieldRestrictions = new ArrayList<>();
    private int numRows;
    private long startTs;
    private long endTs;
    private boolean descending;

    public static class FieldImpl implements Criteria.Field {
        private final String name;
        private final String value;

        public FieldImpl(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "FieldImpl{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

    public CriteriaImpl(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<T> clazz() {
        return clazz;
    }

    public CriteriaImpl<T> addFieldRestriction(String fieldName, String fieldValue) {
        this.fieldRestrictions.add(new FieldImpl(fieldName, fieldValue));
        return this;
    }

    public CriteriaImpl<T> setNumRows(Integer numRows) {
        this.numRows = numRows;
        return this;
    }

    public CriteriaImpl<T> setStartTs(Long ts) {
        this.startTs = ts;
        return this;
    }

    public CriteriaImpl<T> setEndTs(Long ts) {
        this.endTs = ts;
        return this;
    }

    public CriteriaImpl<T> setDescending(boolean flag) {
        this.descending = flag;
        return this;
    }

    @Override
    public List<Field> fieldRestrictions() {
        return fieldRestrictions;
    }

    @Override
    public int numRows() {
        return numRows;
    }

    @Override
    public long startTs() {
        return startTs;
    }

    @Override
    public long endTs() {
        return endTs;
    }

    @Override
    public boolean isDescending() {
        return descending;
    }

    @Override
    public String toString() {
        return "CriteriaImpl{" +
                "clazz=" + clazz +
                ", fieldRestrictions=" + fieldRestrictions +
                ", numRows=" + numRows +
                ", startTs=" + startTs +
                ", endTs=" + endTs +
                '}';
    }
}
