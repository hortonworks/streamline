package com.hortonworks.iotas.storage;

/**
 * Represents a subtype of DataSource like 'Device'.
 * The dataSourceId is the foreign key reference to the
 * parent entity DataSource.
 */
public interface DataSourceSubType extends Storable {
    /**
     * Get the id of its parent data source.
     */
    Long getDataSourceId();

    /**
     * Set the id of its parent data source.
     */
    void setDataSourceId(Long dataSourceId);
}
