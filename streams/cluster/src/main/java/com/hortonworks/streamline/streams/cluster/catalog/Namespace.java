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

import java.util.HashMap;
import java.util.Map;

/**
 * Virtual group of services which are from multiple clusters
 */
@StorableEntity
public class Namespace extends AbstractStorable {
  public static final String NAMESPACE = "namespace";

  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String STREAMING_ENGINE = "streamingEngine";
  public static final String TIME_SERIES_DB = "timeSeriesDB";
  public static final String LOG_SEARCH_SERVICE = "logSearchService";
  public static final String DESCRIPTION = "description";
  public static final String INTERNAL = "internal";
  public static final String TIMESTAMP = "timestamp";

  private Long id;
  @SearchableField
  private String name;
  @SearchableField
  private String streamingEngine;
  private String timeSeriesDB;
  private String logSearchService;
  @SearchableField
  private String description = "";
  private Boolean internal = false;
  private Long timestamp;

  @JsonIgnore
  @Override
  public String getNameSpace() {
    return NAMESPACE;
  }

  @JsonIgnore
  @Override
  public PrimaryKey getPrimaryKey() {
    Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
    fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
    return new PrimaryKey(fieldToObjectMap);
  }

  @JsonIgnore
  @Override
  public Schema getSchema() {
    return Schema.of(
            Schema.Field.of(ID, Schema.Type.LONG),
            Schema.Field.of(NAME, Schema.Type.STRING),
            Schema.Field.of(STREAMING_ENGINE, Schema.Type.STRING),
            Schema.Field.of(TIME_SERIES_DB, Schema.Type.STRING),
            Schema.Field.of(LOG_SEARCH_SERVICE, Schema.Type.STRING),
            Schema.Field.of(DESCRIPTION, Schema.Type.STRING),
            Schema.Field.of(INTERNAL, Schema.Type.BOOLEAN),
            Schema.Field.of(TIMESTAMP, Schema.Type.LONG)
    );
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
   * The name of the namespace
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * The selected streaming engine of the namespace
   */
  public String getStreamingEngine() {
    return streamingEngine;
  }

  public void setStreamingEngine(String streamingEngine) {
    this.streamingEngine = streamingEngine;
  }

  /**
   * The selected Time-series DB of the namespace
   */
  public String getTimeSeriesDB() {
    return timeSeriesDB;
  }

  public void setTimeSeriesDB(String timeSeriesDB) {
    this.timeSeriesDB = timeSeriesDB;
  }

  /**
   * The selected Log Search Service of the namespace
   */
  public String getLogSearchService() {
    return logSearchService;
  }

  public void setLogSearchService(String logSearchService) {
    this.logSearchService = logSearchService;
  }

  /**
   * The namespace description (optional)
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

  public Boolean getInternal() {
    return internal;
  }

  public void setInternal(Boolean internal) {
    this.internal = internal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Namespace)) return false;

    Namespace namespace = (Namespace) o;

    if (getId() != null ? !getId().equals(namespace.getId()) : namespace.getId() != null) return false;
    if (getName() != null ? !getName().equals(namespace.getName()) : namespace.getName() != null) return false;
    if (getStreamingEngine() != null ? !getStreamingEngine().equals(namespace.getStreamingEngine()) : namespace.getStreamingEngine() != null)
      return false;
    if (getTimeSeriesDB() != null ? !getTimeSeriesDB().equals(namespace.getTimeSeriesDB()) : namespace.getTimeSeriesDB() != null)
      return false;
    if (getLogSearchService() != null ? !getLogSearchService().equals(namespace.getLogSearchService()) : namespace.getLogSearchService() != null)
      return false;
    if (getDescription() != null ? !getDescription().equals(namespace.getDescription()) : namespace.getDescription() != null)
      return false;
    if (getInternal() != null ? !getInternal().equals(namespace.getInternal()) : namespace.getInternal() != null)
      return false;
    return getTimestamp() != null ? getTimestamp().equals(namespace.getTimestamp()) : namespace.getTimestamp() == null;
  }

  @Override
  public int hashCode() {
    int result = getId() != null ? getId().hashCode() : 0;
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    result = 31 * result + (getStreamingEngine() != null ? getStreamingEngine().hashCode() : 0);
    result = 31 * result + (getTimeSeriesDB() != null ? getTimeSeriesDB().hashCode() : 0);
    result = 31 * result + (getLogSearchService() != null ? getLogSearchService().hashCode() : 0);
    result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
    result = 31 * result + (getInternal() != null ? getInternal().hashCode() : 0);
    result = 31 * result + (getTimestamp() != null ? getTimestamp().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Namespace{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", streamingEngine='" + streamingEngine + '\'' +
            ", timeSeriesDB='" + timeSeriesDB + '\'' +
            ", logSearchService='" + logSearchService + '\'' +
            ", description='" + description + '\'' +
            ", internal=" + internal +
            ", timestamp=" + timestamp +
            '}';
  }
}
