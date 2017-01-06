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

package com.hortonworks.streamline.streams.notification.store;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for representing user defined
 * query criteria.
 */
public class CriteriaImpl<T> implements Criteria<T> {
    private final Class<T> clazz;
    private final List<Field> fieldRestrictions = new ArrayList<>();
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
