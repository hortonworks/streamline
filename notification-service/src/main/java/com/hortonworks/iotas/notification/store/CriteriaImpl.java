package com.hortonworks.iotas.notification.store;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation for representing user defined
 * query criteria.
 */
public class CriteriaImpl<T> implements Criteria<T> {
    private final Class<T> clazz;
    private Map<String, String> fieldRestrictions = new HashMap<>();
    private int numRows;

    public CriteriaImpl(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<T> clazz() {
        return clazz;
    }

    public CriteriaImpl<T> addFieldRestriction(String fieldName, String fieldValue) {
        this.fieldRestrictions.put(fieldName, fieldValue);
        return this;
    }

    public CriteriaImpl<T> setNumRows(int numRows) {
        this.numRows = numRows;
        return this;
    }

    @Override
    public Map<String, String> fieldRestrictions() {
        return fieldRestrictions;
    }

    @Override
    public int numRows() {
        return numRows;
    }

    @Override
    public String toString() {
        return "CriteriaImpl{" +
                "clazz=" + clazz +
                ", fieldRestrictions=" + fieldRestrictions +
                ", numRows=" + numRows +
                '}';
    }
}
