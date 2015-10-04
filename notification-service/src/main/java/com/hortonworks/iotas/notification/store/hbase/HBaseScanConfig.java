package com.hortonworks.iotas.notification.store.hbase;

import com.hortonworks.iotas.notification.store.hbase.mappers.IndexMapper;
import com.hortonworks.iotas.notification.store.hbase.mappers.Mapper;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.PageFilter;

import java.nio.charset.StandardCharsets;

/**
 * A wrapper class used to hold the HBase table scan config params.
 */
public class HBaseScanConfig<T> {
    private IndexMapper<T> mapper;
    private String indexedFieldValue;
    private final FilterList filterList = new FilterList();

    public void setMapper(IndexMapper<T> mapper) {
        this.mapper = mapper;
    }

    public void setIndexedFieldValue(String value) {
        this.indexedFieldValue = value;
    }

    public void setNumRows(int n) {
        this.filterList.addFilter(new PageFilter(n));
    }

    public void addFilter(Filter filter) {
        this.filterList.addFilter(filter);
    }

    public byte[] getStartRow() {
        return indexedFieldValue == null ? null : indexedFieldValue.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] getStopRow() {
        return indexedFieldValue == null ? null : (indexedFieldValue + Mapper.ROWKEY_SEP + "z").getBytes(StandardCharsets.UTF_8);
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
                "mapper=" + mapper.getClass() +
                ", indexedFieldValue='" + indexedFieldValue + '\'' +
                ", filterList=" + filterList +
                '}';
    }
}
