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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Component)) return false;

        Component component = (Component) o;

        if (port != component.port) return false;
        if (id != null ? !id.equals(component.id) : component.id != null) return false;
        if (clusterId != null ? !clusterId.equals(component.clusterId) : component.clusterId != null) return false;
        if (name != null ? !name.equals(component.name) : component.name != null) return false;
        if (type != component.type) return false;
        if (description != null ? !description.equals(component.description) : component.description != null)
            return false;
        if (config != null ? !config.equals(component.config) : component.config != null) return false;
        return !(hosts != null ? !hosts.equals(component.hosts) : component.hosts != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (clusterId != null ? clusterId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (config != null ? config.hashCode() : 0);
        result = 31 * result + (hosts != null ? hosts.hashCode() : 0);
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "Component{" +
                "id=" + id +
                ", clusterId=" + clusterId +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", description='" + description + '\'' +
                ", config='" + config + '\'' +
                ", hosts='" + hosts + '\'' +
                ", port=" + port +
                ", timestamp=" + timestamp +
                '}';
    }
}
