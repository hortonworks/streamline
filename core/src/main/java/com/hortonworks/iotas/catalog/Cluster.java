package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;

import java.util.HashMap;
import java.util.Map;

public class Cluster extends AbstractStorable {
    private static final String NAMESPACE = "cluster";

    /**
     * The cluster type - STORM, KAFKA, HDFS
     */
    public enum Type {
        STORM, KAFKA, HDFS
    }

    private Long id;
    private String name;
    private Type type;
    private String description = "";
    private String tags = "";
    private Long timestamp;

    /**
     * The name of the cluster
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The type of the cluster. Should be one of {@link com.hortonworks.iotas.catalog.Cluster.Type}
     */
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }


    /**
     * The cluster description (optional)
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * A comma separated tags associated with the cluster (optional)
     */
    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * The primary key
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonIgnore
    public String getNameSpace() {
        return NAMESPACE;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonIgnore
    public Schema getSchema() {
        return new Schema.SchemaBuilder()
                .fields(new Schema.Field("id", Schema.Type.LONG),
                        new Schema.Field("name", Schema.Type.STRING),
                        new Schema.Field("type", Schema.Type.STRING),
                        new Schema.Field("description", Schema.Type.STRING),
                        new Schema.Field("tags", Schema.Type.STRING),
                        new Schema.Field("timestamp", Schema.Type.LONG)).build();
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

}
