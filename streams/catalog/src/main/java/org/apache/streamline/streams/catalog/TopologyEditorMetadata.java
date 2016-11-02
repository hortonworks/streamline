package org.apache.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.streamline.common.Schema;
import org.apache.streamline.storage.PrimaryKey;
import org.apache.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

/**
 * A storable object mainly to store any information needed by UI in a persistent fashion
 */
public class TopologyEditorMetadata extends AbstractStorable {
    public static final String NAME_SPACE = "topology_editor_metadata";
    public static final String TOPOLOGY_ID = "topologyId";
    public static final String VERSION_ID = "versionId";
    public static final String DATA = "data";
    public static final String TIMESTAMP = "timestamp";

    private Long topologyId;
    private Long versionId;
    private String data;
    private Long timestamp;

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTopologyId() {
        return topologyId;
    }

    public String getData() {
        return data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    @Override
    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(TOPOLOGY_ID, Schema.Type.LONG), this.topologyId);
        fieldToObjectMap.put(new Schema.Field(VERSION_ID, Schema.Type.LONG), this.versionId);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    @JsonIgnore
    public String getNameSpace() {
        return NAME_SPACE;
    }

    @Override
    public String toString() {
        return "TopologyEditorMetadata{" +
                "topologyId=" + topologyId +
                ", versionId=" + versionId +
                ", data='" + data + '\'' +
                ", timestamp=" + timestamp +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopologyEditorMetadata that = (TopologyEditorMetadata) o;

        if (topologyId != null ? !topologyId.equals(that.topologyId) : that.topologyId != null) return false;
        if (versionId != null ? !versionId.equals(that.versionId) : that.versionId != null) return false;
        return data != null ? data.equals(that.data) : that.data == null;

    }

    @Override
    public int hashCode() {
        int result = topologyId != null ? topologyId.hashCode() : 0;
        result = 31 * result + (versionId != null ? versionId.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @JsonIgnore
    @Override
    public Long getId() {
        return super.getId();
    }
}
