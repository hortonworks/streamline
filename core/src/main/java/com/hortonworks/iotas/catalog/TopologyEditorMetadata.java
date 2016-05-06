package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;

import java.util.HashMap;
import java.util.Map;

/**
 * A storable object mainly to store any information needed by UI in a persistent fashion
 */
public class TopologyEditorMetadata extends AbstractStorable {
    public static final String NAME_SPACE = "topology_editor_metadata";
    public static final String TOPOLOGY_ID = "topologyId";
    public static final String DATA = "data";
    public static final String TIMESTAMP = "timestamp";

    private Long topologyId;
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

    @Override
    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field(TOPOLOGY_ID, Schema.Type.LONG), this.topologyId);
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
                ", data='" + data + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopologyEditorMetadata topologyEditorMetadata = (TopologyEditorMetadata) o;

        if (topologyId != null ? !topologyId.equals(topologyEditorMetadata.topologyId) : topologyEditorMetadata.topologyId != null) return false;
        return !(data != null ? !data.equals(topologyEditorMetadata.data) : topologyEditorMetadata.data != null);

    }

    @Override
    public int hashCode() {
        int result = topologyId != null ? topologyId.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @JsonIgnore
    @Override
    public Long getId() {
        return super.getId();
    }
}
