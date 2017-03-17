package com.hortonworks.streamline.streams.catalog.topology.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@StorableEntity
public class TopologyState extends AbstractStorable {
    public static final String NAMESPACE = "topology_state";
    public static final String TOPOLOGY_ID = "topologyId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";

    private Long topologyId;

    private String name;

    private String description;

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

    @JsonIgnore
    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(TOPOLOGY_ID, Schema.Type.LONG), this.topologyId);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    public String toString() {
        return "[" + name + ", " + description + "]";
    }

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public void setId(Long id) {
        // noop
    }
}
