package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class DataSink extends AbstractDataResource {
    public static final String NAME_SPACE = "datasinks";
    public static final String DATA_SINK_ID = "id";
    public static final String DATA_SINK_NAME = "name";

    /**
     * The known types of data sinks.
     */
    public enum Type {
        HBASE,
        HDFS,
        HIVE,
        KAFKA
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
     * The type of the datasink e.g. HDFS, HBASE, HIVE, KAFKA
     */
    private Type type;

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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @JsonIgnore
    public String getNameSpace() {
        return NAME_SPACE;
    }

    @JsonIgnore
    public Schema getSchema() {
        return new Schema.SchemaBuilder().fields(
                new Schema.Field(DATA_SINK_ID, Schema.Type.LONG),
                new Schema.Field(DATA_SINK_NAME, Schema.Type.STRING),
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
        fieldToObjectMap.put(new Schema.Field(DATA_SINK_ID, Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    public Map toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DATA_SINK_ID, id);
        map.put(DATA_SINK_NAME, this.name);
        map.put(DESCRIPTION, this.description);
        map.put(TAGS, this.tags);
        map.put(TIMESTAMP, this.timestamp);
        map.put(TYPE, type.name());
        map.put(TYPE_CONFIG, typeConfig);
        return map;
    }

    @Override
    public Storable fromMap(Map<String, Object> map) {
        this.id = (Long) map.get(DATA_SINK_ID);
        this.name = (String)  map.get(DATA_SINK_NAME);
        this.description = (String)  map.get(DESCRIPTION);
        this.tags = (String)  map.get(TAGS);
        this.timestamp = (Long) map.get(TIMESTAMP);
        type = Type.valueOf((String) map.get(TYPE));
        typeConfig = (String) map.get(TYPE_CONFIG);
        return this;

    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataSink)) return false;

        DataSink dataSink = (DataSink) o;

        if (id != null ? !id.equals(dataSink.id) : dataSink.id != null) return false;
        if (name != null ? !name.equals(dataSink.name) : dataSink.name != null) return false;
        if (description != null ? !description.equals(dataSink.description) : dataSink.description != null) return false;
        if (tags != null ? !tags.equals(dataSink.tags) : dataSink.tags != null) return false;
        if (type != null ? !type.equals(dataSink.type) : dataSink.type != null) return false;
        return !(typeConfig != null ? !typeConfig.equals(dataSink.typeConfig) : dataSink.typeConfig != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (typeConfig != null ? typeConfig.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DataSink{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", tags='" + tags + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", typeConfig='" + typeConfig + '\'' +
                '}';
    }
}
