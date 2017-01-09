package org.apache.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.streamline.common.Schema;
import org.apache.streamline.storage.PrimaryKey;
import org.apache.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

/**
 * Logical cluster which contains services.
 * @see Service
 */
public class Cluster extends AbstractStorable {
    private static final String NAMESPACE = "clusters";

    private Long id;
    private String name;
    private String ambariImportUrl = "";
    private String description = "";
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

    public String getAmbariImportUrl() {
        return ambariImportUrl;
    }

    public void setAmbariImportUrl(String ambariImportUrl) {
        this.ambariImportUrl = ambariImportUrl;
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
     * The primary key
     */
    @Override
    public Long getId() {
        return id;
    }

    @Override
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
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cluster)) return false;

        Cluster cluster = (Cluster) o;

        if (getId() != null ? !getId().equals(cluster.getId()) : cluster.getId() != null) return false;
        if (getName() != null ? !getName().equals(cluster.getName()) : cluster.getName() != null) return false;
        if (getAmbariImportUrl() != null ? !getAmbariImportUrl().equals(cluster.getAmbariImportUrl()) : cluster.getAmbariImportUrl() != null)
            return false;
        if (getDescription() != null ? !getDescription().equals(cluster.getDescription()) : cluster.getDescription() != null)
            return false;
        return getTimestamp() != null ? getTimestamp().equals(cluster.getTimestamp()) : cluster.getTimestamp() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getAmbariImportUrl() != null ? getAmbariImportUrl().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Cluster{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", ambariImportUrl='" + ambariImportUrl + '\'' +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
