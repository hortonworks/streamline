package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;

import java.util.HashMap;
import java.util.Map;

public class Component extends AbstractStorable {
    private static final String NAMESPACE = "component";

    /**
     * The component types
     * to ensure that we are dealing with known types
     */
    public enum ComponentType {
        NIMBUS, SUPERVISOR, UI, ZOOKEEPER, BROKER, NAMENODE, DATANODE
    }

    private Long id;
    private Long clusterId;
    private String name;
    private ComponentType type;
    private String description = "";
    private String config = "";
    private String hosts;
    private int port;
    private Long timestamp;

    /**
     * The primary key.
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * The foreign key reference to the cluster id.
     */
    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    /**
     * The component name.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The type of the component (Nimbus, Broker etc).
     */
    public ComponentType getType() {
        return type;
    }

    public void setType(ComponentType type) {
        this.type = type;
    }

    /**
     * Component description.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Component specific config.
     */
    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    /**
     * The set of hosts where the component runs.
     */
    public String getHosts() {
        return hosts;
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    /**
     * The port where the component listens.
     */
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @JsonIgnore
    public String getNameSpace() {
        return NAMESPACE;
    }

    @JsonIgnore
    public Schema getSchema() {
        return null;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

}
