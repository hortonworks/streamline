package com.hortonworks.iotas.catalog;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;

import java.util.HashMap;
import java.util.Map;

public class DataSource extends AbstractDataResource {
    public static final String NAME_SPACE = "datasources";
    public static final String DATA_SOURCE_ID = "dataSourceId";
    public static final String DATA_SOURCE_NAME = "dataSourceName";

    /**
     * The known types of data sources.
     */
    public enum Type {
        DEVICE,
        UNKNOWN;
    }

    /**
     * Unique identifier. This is the primary key.
     */
    private Long dataSourceId;

    /**
     * Human readable name.
     */
    private String dataSourceName;
    /**
     * The type of the datasource e.g. 'DEVICE'
     */
    private Type type;

    @JsonIgnore
    public String getNameSpace() {
        return NAME_SPACE;
    }

    @JsonIgnore
    public Schema getSchema() {
         return new Schema.SchemaBuilder().fields(
                 new Schema.Field(DATA_SOURCE_ID, Schema.Type.LONG),
                 new Schema.Field(DATA_SOURCE_NAME, Schema.Type.STRING),
                 new Schema.Field(DESCRIPTION, Schema.Type.STRING),
                 new Schema.Field(TAGS, Schema.Type.STRING),
                 new Schema.Field(TIMESTAMP, Schema.Type.LONG),
                 new Schema.Field(TYPE, Schema.Type.STRING),
                 new Schema.Field(TYPE_CONFIG, Schema.Type.STRING)
         ).build();
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field(DATA_SOURCE_ID, Schema.Type.LONG), this.dataSourceId);
        return new PrimaryKey(fieldToObjectMap);
    }

    public Map toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DATA_SOURCE_ID, dataSourceId);
        map.put(DATA_SOURCE_NAME, this.dataSourceName);
        map.put(DESCRIPTION, this.description);
        map.put(TAGS, this.tags);
        map.put(TIMESTAMP, this.timestamp);
        map.put(TYPE, type.name());
        map.put(TYPE_CONFIG, typeConfig);
        return map;
    }

    public DataSource fromMap(Map<String, Object> map) {
        this.dataSourceId = (Long) map.get(DATA_SOURCE_ID);
        this.dataSourceName = (String)  map.get(DATA_SOURCE_NAME);
        this.description = (String)  map.get(DESCRIPTION);
        this.tags = (String)  map.get(TAGS);
        this.timestamp = (Long) map.get(TIMESTAMP);
        type = Type.valueOf((String) map.get(TYPE));
        typeConfig = (String) map.get(TYPE_CONFIG);
        return this;
    }

    public Long getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(Long dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataSource that = (DataSource) o;

        if (dataSourceId != null ? !dataSourceId.equals(that.dataSourceId) : that.dataSourceId != null) return false;
        if (dataSourceName != null ? !dataSourceName.equals(that.dataSourceName) : that.dataSourceName != null)
            return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (tags != null ? !tags.equals(that.tags) : that.tags != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        return !(typeConfig != null ? !typeConfig.equals(that.typeConfig) : that.typeConfig != null);

    }

    @Override
    public int hashCode() {
        int result = dataSourceId != null ? dataSourceId.hashCode() : 0;
        result = 31 * result + (dataSourceName != null ? dataSourceName.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (typeConfig != null ? typeConfig.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DataSource{" +
                "dataSourceId=" + dataSourceId +
                ", dataSourceName='" + dataSourceName + '\'' +
                ", description='" + description + '\'' +
                ", tags='" + tags + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", typeConfig='" + typeConfig + '\'' +
                '}';
    }

}
