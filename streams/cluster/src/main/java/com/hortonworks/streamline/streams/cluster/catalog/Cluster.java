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
import com.hortonworks.streamline.storage.annotation.SearchableField;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Logical cluster which contains services.
 * @see Service
 */
@StorableEntity
public class Cluster extends AbstractStorable {
    public static final String NAMESPACE = "cluster";

    private Long id;
    @SearchableField
    private String name;
    private String ambariImportUrl = "";
    @SearchableField
    private String description = "";
    private Long timestamp;

    /**
     * The name of the cluster
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAmbariImportUrl() {
        return ambariImportUrl;
    }

    public void setAmbariImportUrl(String ambariImportUrl) {
        this.ambariImportUrl = ambariImportUrl;
    }

    /**
     * The cluster description (optional)
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public String getNameWithImportUrl() {
        if (StringUtils.isEmpty(ambariImportUrl)) {
            return name + " []";
        }
        return name + " [" + ambariImportUrl + "]";
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
        if (!(o instanceof Cluster)) return false;

        Cluster cluster = (Cluster) o;

        if (getId() != null ? !getId().equals(cluster.getId()) : cluster.getId() != null) return false;
        if (getName() != null ? !getName().equals(cluster.getName()) : cluster.getName() != null) return false;
        if (getAmbariImportUrl() != null ? !getAmbariImportUrl().equals(cluster.getAmbariImportUrl()) : cluster.getAmbariImportUrl() != null)
            return false;
        if (getDescription() != null ? !getDescription().equals(cluster.getDescription()) : cluster.getDescription() != null)
            return false;
        return getTimestamp() != null ? getTimestamp().equals(cluster.getTimestamp()) : cluster.getTimestamp() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getAmbariImportUrl() != null ? getAmbariImportUrl().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Cluster{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", ambariImportUrl='" + ambariImportUrl + '\'' +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
