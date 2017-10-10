/**
 * Copyright 2017 Hortonworks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.hortonworks.streamline.streams.cluster.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.registries.storage.PrimaryKey;
import com.hortonworks.registries.storage.annotation.StorableEntity;
import com.hortonworks.registries.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

/**
 * Component Process represents an individual process of Component, especially for H/A.
 */
@StorableEntity
public class ComponentProcess extends AbstractStorable {
    private static final String NAMESPACE = "component_process";

    private Long id;
    private Long componentId;
    private String host;
    /**
     The protocol communicating in this port.
     Its representation is up to component.
     For example. protocols for KAFKA are PLAINTEXT, SSL, etc.
     */
    private String protocol;
    private Integer port;
    private Long timestamp;

    /**
     * The primary key.
     */
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * The foreign key reference to the component id.
     */
    public Long getComponentId() {
        return componentId;
    }

    public void setComponentId(Long componentId) {
        this.componentId = componentId;
    }

    /**
     * The host where the component process runs.
     */
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    /**
     * The protocol where the component communicates. (optional)
     */
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * The port where the component listens. (optional)
     */
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
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
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComponentProcess)) return false;

        ComponentProcess that = (ComponentProcess) o;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getComponentId() != null ? !getComponentId().equals(that.getComponentId()) : that.getComponentId() != null)
            return false;
        if (getHost() != null ? !getHost().equals(that.getHost()) : that.getHost() != null) return false;
        if (getProtocol() != null ? !getProtocol().equals(that.getProtocol()) : that.getProtocol() != null)
            return false;
        if (getPort() != null ? !getPort().equals(that.getPort()) : that.getPort() != null) return false;
        return getTimestamp() != null ? getTimestamp().equals(that.getTimestamp()) : that.getTimestamp() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getComponentId() != null ? getComponentId().hashCode() : 0);
        result = 31 * result + (getHost() != null ? getHost().hashCode() : 0);
        result = 31 * result + (getProtocol() != null ? getProtocol().hashCode() : 0);
        result = 31 * result + (getPort() != null ? getPort().hashCode() : 0);
        result = 31 * result + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ComponentProcess{" +
                "id=" + id +
                ", componentId=" + componentId +
                ", host='" + host + '\'' +
                ", protocol='" + protocol + '\'' +
                ", port=" + port +
                ", timestamp=" + timestamp +
                '}';
    }
}
