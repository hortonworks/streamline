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
import com.hortonworks.registries.storage.annotation.StorableEntity;
import com.hortonworks.registries.storage.PrimaryKey;
import com.hortonworks.registries.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

/**
 * Service represents a component of system. For example, STORM, KAFKA, etc.
 */
@StorableEntity
public class Service extends AbstractStorable {
  private static final String NAMESPACE = "service";

  private Long id;
  private Long clusterId;
  private String name;
  private String description = "";
  private Long timestamp;

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
   * The foreign key reference to the cluster id.
   */
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  /**
   * The name of the cluster
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Service)) return false;

    Service service = (Service) o;

    if (getId() != null ? !getId().equals(service.getId()) : service.getId() != null) return false;
    if (getClusterId() != null ?
        !getClusterId().equals(service.getClusterId()) :
        service.getClusterId() != null) return false;
    if (getName() != null ? !getName().equals(service.getName()) : service.getName() != null)
      return false;
    if (getDescription() != null ?
        !getDescription().equals(service.getDescription()) :
        service.getDescription() != null) return false;
    return getTimestamp() != null ?
        getTimestamp().equals(service.getTimestamp()) :
        service.getTimestamp() == null;

  }

  @Override
  public int hashCode() {
    int result = getId() != null ? getId().hashCode() : 0;
    result = 31 * result + (getClusterId() != null ? getClusterId().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
    result = 31 * result + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Service{" +
        "id=" + id +
        ", clusterId='" + clusterId + '\'' +
        ", name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", timestamp=" + timestamp +
        "}";
  }
}
