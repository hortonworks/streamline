package org.apache.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;
import org.apache.streamline.common.Schema;
import org.apache.streamline.storage.PrimaryKey;
import org.apache.streamline.storage.StorableKey;
import org.apache.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

/**
 * Version info specific to a topology.
 */
public class TopologyVersionInfo extends AbstractStorable {
    public static final String NAME_SPACE = "topology_versioninfos";
    public static final String ID = "id";
    public static final String VERSION_PREFIX = "V";

    private Long id;
    private Long topologyId;
    private String name;
    private String description;
    private Long timestamp;

    public TopologyVersionInfo() {
    }

    public TopologyVersionInfo(TopologyVersionInfo other) {
        setId(other.getId());
        setTopologyId(other.getTopologyId());
        setName(other.getName());
        setDescription(other.getDescription());
        setTimestamp(other.getTimestamp());
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(ID, Schema.Type.LONG),
                this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    public StorableKey getStorableKey() {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    @JsonIgnore
    public String getNameSpace() {
        return NAME_SPACE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTopologyId() {
        return topologyId;
    }

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonIgnore
    public Integer getVersionNumber() {
        if (StringUtils.isEmpty(name) || !name.startsWith(VERSION_PREFIX)) {
            throw new IllegalArgumentException("Cannot get version number from " + name);
        }
        return Integer.parseInt(name.substring(name.indexOf(VERSION_PREFIX) + 1));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopologyVersionInfo that = (TopologyVersionInfo) o;

        return id != null ? id.equals(that.id) : that.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TopologyVersionInfo{" +
                "id=" + id +
                ", topologyId=" + topologyId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                "}";
    }
}
