package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;

/**
 *
 */
public abstract class AbstractDataResource implements Storable {
    public static final String DESCRIPTION = "description";
    public static final String TAGS = "tags";
    public static final String TIMESTAMP = "timestamp";
    public static final String TYPE = "type";
    public static final String TYPE_CONFIG = "typeConfig";

    /**
     * Human readable description.
     */
    protected String description;

    /**
     * Free form tags.
     */
    protected String tags;

    /**
     * Time when this entry was created/updated.
     */
    protected Long timestamp;

    /**
     * The config string specific to the type. e.g. Device Config, HDFS sink config etc.
     */
    protected String typeConfig;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTypeConfig() {
        return typeConfig;
    }

    public void setTypeConfig(String typeConfig) {
        this.typeConfig = typeConfig;
    }


    @JsonIgnore
    public StorableKey getStorableKey() {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }
}
