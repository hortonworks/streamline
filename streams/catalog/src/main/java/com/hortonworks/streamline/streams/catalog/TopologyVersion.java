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
package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import org.apache.commons.lang.StringUtils;
import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

/**
 * Version info specific to a topology.
 */
@StorableEntity
public class TopologyVersion extends AbstractStorable {
    public static final String NAME_SPACE = "topology_version";
    public static final String ID = "id";
    public static final String VERSION_PREFIX = "V";

    private Long id;
    private Long topologyId;
    private String name;
    private String description;
    private Long timestamp;

    public TopologyVersion() {
    }

    public TopologyVersion(TopologyVersion other) {
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

    @Override
    public Long getId() {
        return id;
    }

    @Override
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

        TopologyVersion that = (TopologyVersion) o;

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
