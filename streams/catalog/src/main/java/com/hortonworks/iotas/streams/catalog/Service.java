package com.hortonworks.iotas.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

/**
 * Service represents a component of system. For example, STORM, KAFKA, etc.
 */
public class Service extends AbstractStorable {
  private static final String NAMESPACE = "services";

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
