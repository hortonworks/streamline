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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Catalog that matches one configuration file of service.
 */
@StorableEntity
public class ServiceConfiguration extends AbstractStorable {
  private static final String NAMESPACE = "service_configuration";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private Long id;
  private Long serviceId;
  private String name;
  private String configuration;
  private String description = "";
  private String filename = "";
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
   * The foreign key reference to the service id.
   */
  public Long getServiceId() {
    return serviceId;
  }

  public void setServiceId(Long serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * The name of the configuration. (e.g storm-site)
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * The configuration represented to JSON.
   */
  public String getConfiguration() {
    return configuration;
  }

  /**
   * JSON configuration deserialized to a {@code Map<String,String>}.
   */
  public Map<String, String> getConfigurationMap() throws IOException {
    if (configuration != null) {    // ternary operator does not allow for type inference
      return objectMapper.readValue(configuration, new TypeReference<Map<String, String>>(){ });
    }
    return null;
  }

  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  /**
   * The configuration description (optional)
   */
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Actual file name of configuration. (optional)
   */
  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
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
    if (!(o instanceof ServiceConfiguration)) return false;

    ServiceConfiguration that = (ServiceConfiguration) o;

    if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
    if (getServiceId() != null ?
        !getServiceId().equals(that.getServiceId()) :
        that.getServiceId() != null) return false;
    if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null)
      return false;
    if (getConfiguration() != null ?
        !getConfiguration().equals(that.getConfiguration()) :
        that.getConfiguration() != null) return false;
    if (getDescription() != null ?
        !getDescription().equals(that.getDescription()) :
        that.getDescription() != null) return false;
    if (getFilename() != null ?
        !getFilename().equals(that.getFilename()) :
        that.getFilename() != null) return false;
    return getTimestamp() != null ?
        getTimestamp().equals(that.getTimestamp()) :
        that.getTimestamp() == null;

  }

  @Override
  public int hashCode() {
    int result = getId() != null ? getId().hashCode() : 0;
    result = 31 * result + (getServiceId() != null ? getServiceId().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (getConfiguration() != null ? getConfiguration().hashCode() : 0);
    result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
    result = 31 * result + (getFilename() != null ? getFilename().hashCode() : 0);
    result = 31 * result + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ServiceConfiguration{" +
        "id=" + id +
        ", serviceId=" + serviceId +
        ", name='" + name + '\'' +
        ", configuration='" + configuration + '\'' +
        ", description='" + description + '\'' +
        ", filename='" + filename + '\'' +
        ", timestamp=" + timestamp +
        '}';
  }
}
