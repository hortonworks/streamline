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

package org.apache.streamline.streams.notification.store.hbase;

import org.apache.streamline.streams.notification.store.hbase.mappers.IndexMapper;
import org.apache.streamline.streams.notification.store.hbase.mappers.Mapper;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.PageFilter;

import java.nio.charset.StandardCharsets;

/**
 * A wrapper class used to hold the HBase table scan config params.
 */
public class HBaseScanConfig<T> {
    private static final int DEFAULT_NUM_ROWS = 10;

    private IndexMapper<T> mapper;
    private String indexedFieldValue;
    private final FilterList filterList = new FilterList();
    private long startTs;
    private long endTs = Long.MAX_VALUE;

    public void setMapper(IndexMapper<T> mapper) {
        this.mapper = mapper;
    }

    public void setIndexedFieldValue(String value) {
        this.indexedFieldValue = value;
    }

    public void setNumRows(int n) {
        this.filterList.addFilter(new PageFilter(n == 0 ? DEFAULT_NUM_ROWS : n));
    }

    public void setStartTs(long startTsMillis) {
        this.startTs = startTsMillis;
    }

    public void setEndTs(long endTsMillis) {
        if (endTsMillis != 0) {
            this.endTs = endTsMillis;
        }
    }

    public void addFilter(Filter filter) {
        this.filterList.addFilter(filter);
    }

    // assumes that index table row key always has ts as suffix.
    public byte[] getStartRow() {
        StringBuilder sb = new StringBuilder();
        if (indexedFieldValue != null) {
            sb.append(indexedFieldValue).append(Mapper.ROWKEY_SEP);
        }
        sb.append(startTs);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // assumes that index table row key always has ts as suffix.
    // TODO: currently stop row excludes end Ts.
    public byte[] getStopRow() {
        StringBuilder sb = new StringBuilder();
        if (indexedFieldValue != null) {
            sb.append(indexedFieldValue).append(Mapper.ROWKEY_SEP);
        }
        sb.append(endTs);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public FilterList filterList() {
        return filterList;
    }

    public IndexMapper<T> getMapper() {
        return mapper;
    }

    @Override
    public String toString() {
        return "HBaseScanConfig{" +
                "mapper=" + mapper +
                ", indexedFieldValue='" + indexedFieldValue + '\'' +
                ", filterList=" + filterList +
                ", startTsMillis=" + startTs +
                ", endTsMillis=" + endTs +
                '}';
    }
}
