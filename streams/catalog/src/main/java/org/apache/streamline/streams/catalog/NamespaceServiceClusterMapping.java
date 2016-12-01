package org.apache.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.streamline.common.Schema;
import org.apache.streamline.storage.PrimaryKey;
import org.apache.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

/**
 * The association between service in namespace and cluster.
 */
public class NamespaceServiceClusterMapping extends AbstractStorable {
  private static final String NAMESPACE = "namespace_service_cluster_mapping";
  private static final String FIELD_NAMESPACE_ID = "namespaceId";
  private static final String FIELD_SERVICE_NAME = "serviceName";
  private static final String FIELD_CLUSTER_ID = "clusterId";

  private Long namespaceId;
  private String serviceName;
  private Long clusterId;

  public NamespaceServiceClusterMapping() {
  }

  public NamespaceServiceClusterMapping(Long namespaceId, String serviceName, Long clusterId) {
    this.namespaceId = namespaceId;
    this.serviceName = serviceName;
    this.clusterId = clusterId;
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
    fieldToObjectMap.put(Schema.Field.of(FIELD_NAMESPACE_ID, Schema.Type.LONG), this.namespaceId);
    fieldToObjectMap.put(Schema.Field.of(FIELD_SERVICE_NAME, Schema.Type.STRING), this.serviceName);
    fieldToObjectMap.put(Schema.Field.of(FIELD_CLUSTER_ID, Schema.Type.LONG), this.clusterId);
    return new PrimaryKey(fieldToObjectMap);
  }

  public Long getNamespaceId() {
    return namespaceId;
  }

  public void setNamespaceId(Long namespaceId) {
    this.namespaceId = namespaceId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof NamespaceServiceClusterMapping)) return false;

    NamespaceServiceClusterMapping that = (NamespaceServiceClusterMapping) o;

    if (getNamespaceId() != null ?
        !getNamespaceId().equals(that.getNamespaceId()) :
        that.getNamespaceId() != null) return false;
    if (getServiceName() != null ?
        !getServiceName().equals(that.getServiceName()) :
        that.getServiceName() != null) return false;
    return getClusterId() != null ?
        getClusterId().equals(that.getClusterId()) :
        that.getClusterId() == null;

  }

  @Override
  public int hashCode() {
    int result = getNamespaceId() != null ? getNamespaceId().hashCode() : 0;
    result = 31 * result + (getServiceName() != null ? getServiceName().hashCode() : 0);
    result = 31 * result + (getClusterId() != null ? getClusterId().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "NamespaceServiceClusterMapping{" +
        "namespaceId=" + namespaceId +
        ", serviceName='" + serviceName + '\'' +
        ", clusterId=" + clusterId +
        '}';
  }

  @JsonIgnore
  @Override
  public Long getId() {
    return super.getId();
  }
}
