package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;

import java.util.HashMap;
import java.util.Map;

/**
 * A storable object mainly to store any information needed by UI in a persistent fashion
 */
public class UIInfo extends AbstractStorable {
    public static final String NAME_SPACE = "uiinfo";
    public static final String TOPOLOGY_ID = "topologyId";
    public static final String JSON_INFO = "jsonInfo";
    public static final String TIMESTAMP = "timestamp";

    private Long topologyId;
    private String jsonInfo;
    private Long timestamp;

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
    }

    public void setJsonInfo(String jsonInfo) {
        this.jsonInfo = jsonInfo;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTopologyId() {
        return topologyId;
    }

    public String getJsonInfo() {
        return jsonInfo;
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
        return "UIInfo{" +
                "topologyId=" + topologyId +
                ", jsonInfo='" + jsonInfo + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UIInfo uiInfo = (UIInfo) o;

        if (topologyId != null ? !topologyId.equals(uiInfo.topologyId) : uiInfo.topologyId != null) return false;
        return !(jsonInfo != null ? !jsonInfo.equals(uiInfo.jsonInfo) : uiInfo.jsonInfo != null);

    }

    @Override
    public int hashCode() {
        int result = topologyId != null ? topologyId.hashCode() : 0;
        result = 31 * result + (jsonInfo != null ? jsonInfo.hashCode() : 0);
        return result;
    }
}
