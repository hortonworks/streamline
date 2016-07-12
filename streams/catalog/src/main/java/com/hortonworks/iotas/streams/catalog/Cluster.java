package com.hortonworks.iotas.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

public class Cluster extends AbstractStorable {
    private static final String NAMESPACE = "cluster";
    /**
     * The cluster type
     */
    public enum Type {
        STORM, KAFKA, HDFS, HBASE
    }

    private Long id;
    private String name;
    private Type type;
    private String description = "";
    private String tags = "";
    private Long timestamp;
    private String clusterConfigFileName;
    private String clusterConfigStorageName;

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
     * The type of the cluster. Should be one of {@link Cluster.Type}
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

    /**
     * Name of cluster specific config file. E.g. hdfs-site.xml, hbase-site.xml etc
     */
    public String getClusterConfigFileName() {
        return clusterConfigFileName;
    }

    public void setClusterConfigFileName(String clusterConfigFileName) {
        this.clusterConfigFileName = clusterConfigFileName;
    }

    /**
     * The actual name with which the file is stored in the storage. E.g. hdfs-site.xml-UUID
     */
    public String getClusterConfigStorageName() {
        return clusterConfigStorageName;
    }

    public void setClusterConfigStorageName(String clusterConfigStorageName) {
        this.clusterConfigStorageName = clusterConfigStorageName;
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cluster cluster = (Cluster) o;

        if (id != null ? !id.equals(cluster.id) : cluster.id != null) return false;
        if (name != null ? !name.equals(cluster.name) : cluster.name != null) return false;
        if (type != cluster.type) return false;
        if (description != null ? !description.equals(cluster.description) : cluster.description != null) return false;
        if (tags != null ? !tags.equals(cluster.tags) : cluster.tags != null) return false;
        if (timestamp != null ? !timestamp.equals(cluster.timestamp) : cluster.timestamp != null) return false;
        if (clusterConfigFileName != null ? !clusterConfigFileName.equals(cluster.clusterConfigFileName) : cluster.clusterConfigFileName != null)
            return false;
        return clusterConfigStorageName != null ? clusterConfigStorageName.equals(cluster.clusterConfigStorageName) : cluster.clusterConfigStorageName == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (clusterConfigFileName != null ? clusterConfigFileName.hashCode() : 0);
        result = 31 * result + (clusterConfigStorageName != null ? clusterConfigStorageName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Cluster{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", description='" + description + '\'' +
                ", tags='" + tags + '\'' +
                ", timestamp=" + timestamp +
                ", clusterConfigFileName='" + clusterConfigFileName + '\'' +
                ", clusterConfigStorageName='" + clusterConfigStorageName + '\'' +
                "}";
    }
}
