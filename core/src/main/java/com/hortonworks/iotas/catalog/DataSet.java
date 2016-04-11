package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Config;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.DataSourceSubType;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;

import java.util.HashMap;
import java.util.Map;

/**
 * DataSet entity enables support for non-device datasources. E.g. Twitter, weather etc.
 */
public class DataSet extends DataSourceSubType {
    public static final String NAMESPACE = "dataset";

    /**
     * The dataset specific config
     */
    private Config config;

    @JsonIgnore
    @Override
    public Long getDataSourceId() {
        return dataSourceId;
    }

    @Override
    public void setDataSourceId(Long dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @Override
    public StorableKey getStorableKey() {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataSet dataSet = (DataSet) o;

        return config != null ? config.equals(dataSet.config) : dataSet.config == null;

    }

    @Override
    public int hashCode() {
        return config != null ? config.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "DataSet{" +
                "dataSourceId=" + dataSourceId +
                ", config='" + config + '\'' +
                '}';
    }

    @JsonIgnore
    @Override
    public Long getId() {
        return super.getId();
    }
}
