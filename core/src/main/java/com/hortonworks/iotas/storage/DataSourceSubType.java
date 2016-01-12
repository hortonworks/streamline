package com.hortonworks.iotas.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.catalog.AbstractStorable;
import com.hortonworks.iotas.common.Schema;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a subtype of DataSource like 'Device'.
 * The dataSourceId is the foreign key reference to the
 * parent entity DataSource.
 */
public abstract class DataSourceSubType extends AbstractStorable {
    public static final String DATA_SOURCE_ID = "dataSourceId";

    /**
     * Primary key that is also a foreign key to referencing to the parent table 'dataSources'.
     */
    protected Long dataSourceId;

    /**
     * The primary key of the device is the datasource id itself which is also a foreign key
     * reference to the parent 'DataSource'.
     */
    @Override
    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(DATA_SOURCE_ID, Schema.Type.LONG), dataSourceId);
        return new PrimaryKey(fieldToObjectMap);
    }

    /**
     * Get the id of its parent data source.
     */
    @JsonIgnore
    public Long getDataSourceId() {
        return dataSourceId;
    }

    /**
     * Set the id of its parent data source.
     */
    public void setDataSourceId(Long dataSourceId) {
        this.dataSourceId = dataSourceId;
    }
}
