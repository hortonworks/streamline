package com.hortonworks.iotas.catalog;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.StorableKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSource extends AbstractStorable {
    public static final String NAME_SPACE = "datasources";
    public static final String DATA_SOURCE_ID = "id";
    public static final String DATA_SOURCE_NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String DATAFEED_ID = "datafeedId";
    public static final String TAGS = "tags";
    public static final String TIMESTAMP = "timestamp";
    public static final String TYPE = "type";
    public static final String TYPE_CONFIG = "typeConfig";

    /**
     * The known types of data sources.
     */
    public enum Type {
        DEVICE,
        DATASET,
        UNKNOWN;
    }

    /**
     * Unique identifier. This is the primary key.
     */
    private Long id;

    /**
     * Human readable name.
     */
    private String name;

    /**
     * Human readable description.
     */
    private String description;

    /**
     * the tags associated with this datasource.
     */
    private List<Tag> tags;

    /**
     * Time when this entry was created/updated.
     */
    private Long timestamp;

    /**
     * The type of the datasource e.g. 'DEVICE'
     */
    private Type type;

    /**
     * The config string specific to the type. e.g. Device Config.
     */
    private String typeConfig;

    @JsonIgnore
    public String getNameSpace() {
        return NAME_SPACE;
    }

    @JsonIgnore
    public Schema getSchema() {
         return Schema.of(
                 new Schema.Field(DATA_SOURCE_ID, Schema.Type.LONG),
                 new Schema.Field(DATA_SOURCE_NAME, Schema.Type.STRING),
                 new Schema.Field(DESCRIPTION, Schema.Type.STRING),
                 new Schema.Field(TIMESTAMP, Schema.Type.LONG),
                 new Schema.Field(TYPE, Schema.Type.STRING),
                 new Schema.Field(TYPE_CONFIG, Schema.Type.STRING)
         );
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field(DATA_SOURCE_ID, Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    public StorableKey getStorableKey() {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    public Map<String, Object> toMap() {
        Map map = super.toMap();
        map.put(TYPE, type.name());
        return map;
    }

    public DataSource fromMap(Map<String, Object> map) {
        type = Type.valueOf((String) map.remove(TYPE));
        super.fromMap(map);
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataSource that = (DataSource) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        return !(typeConfig != null ? !typeConfig.equals(that.typeConfig) : that.typeConfig != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (typeConfig != null ? typeConfig.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DataSource{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", tags='" + tags + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", typeConfig='" + typeConfig + '\'' +
                '}';
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getTypeConfig() {
        return typeConfig;
    }

    public void setTypeConfig(String typeConfig) {
        this.typeConfig = typeConfig;
    }
}
