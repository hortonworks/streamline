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
import com.hortonworks.registries.common.Schema;
import com.hortonworks.registries.storage.PrimaryKey;
import com.hortonworks.registries.storage.annotation.StorableEntity;
import com.hortonworks.registries.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

/**
 * This class stores test cases of topology test run.
 * Note that actual test records are stored to TopologyTestRunCaseSource for each source.
 */
@StorableEntity
public class TopologyTestRunCase extends AbstractStorable {
    public static final String NAMESPACE = "topology_test_run_case";

    private Long id;
    private String name;
    private Long topologyId;
    private Long versionId;
    private Long timestamp;

    public TopologyTestRunCase() {
    }

    public TopologyTestRunCase(TopologyTestRunCase other) {
        if (other != null) {
            setId(other.getId());
            setName(other.getName());
            setTopologyId(other.getTopologyId());
            setVersionId(other.getVersionId());
            setTimestamp(other.getTimestamp());
        }
    }

    @JsonIgnore
    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
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

    /**
     * The name of test run case
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The foreign key reference to the topology id.
     */
    public Long getTopologyId() {
        return topologyId;
    }

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
    }

    /**
     * The foreign key reference to the topology version id.
     */
    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TopologyTestRunCase)) return false;

        TopologyTestRunCase that = (TopologyTestRunCase) o;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
        if (getTopologyId() != null ? !getTopologyId().equals(that.getTopologyId()) : that.getTopologyId() != null)
            return false;
        if (getVersionId() != null ? !getVersionId().equals(that.getVersionId()) : that.getVersionId() != null)
            return false;
        return getTimestamp() != null ? getTimestamp().equals(that.getTimestamp()) : that.getTimestamp() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getTopologyId() != null ? getTopologyId().hashCode() : 0);
        result = 31 * result + (getVersionId() != null ? getVersionId().hashCode() : 0);
        result = 31 * result + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TopologyTestRunCase{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", topologyId=" + topologyId +
                ", versionId=" + versionId +
                ", timestamp=" + timestamp +
                '}';
    }

}
