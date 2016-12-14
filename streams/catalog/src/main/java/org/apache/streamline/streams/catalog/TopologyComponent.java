/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.common.Config;
import org.apache.streamline.common.Schema;
import org.apache.streamline.storage.PrimaryKey;
import org.apache.streamline.storage.catalog.AbstractStorable;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class TopologyComponent extends AbstractStorable {
    public static final String NAMESPACE = "topology_components";

    public static final String ID = "id";
    public static final String VERSIONID = "versionId";
    public static final String TOPOLOGYID = "topologyId";
    public static final String TOPOLOGY_COMPONENT_BUNDLE_ID = "topologyComponentBundleId";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String CONFIG = "config";
    public static final String CONFIG_DATA = "configData";

    private Long id;
    private Long topologyId = -1L;
    private Long topologyComponentBundleId = -1L;
    private Long versionId = -1L;
    private String name = StringUtils.EMPTY;
    private String description = StringUtils.EMPTY;
    private Config config;
    // this is not saved in storage but REST apis includes version timestamp here
    private Long versionTimestamp;

    public TopologyComponent() {

    }

    public TopologyComponent(TopologyComponent other) {
        setId(other.getId());
        setTopologyId(other.getTopologyId());
        setTopologyComponentBundleId(other.getTopologyComponentBundleId());
        setVersionId(other.getVersionId());
        setName(other.getName());
        setDescription(other.getDescription());
        setConfig(new Config(other.getConfig()));
        setVersionTimestamp(other.getVersionTimestamp());
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(ID, Schema.Type.LONG), this.id);
        fieldToObjectMap.put(new Schema.Field(VERSIONID, Schema.Type.LONG), this.versionId);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @JsonIgnore
    @Override
    public Schema getSchema() {
        return Schema.of(
                Schema.Field.of(ID, Schema.Type.LONG),
                Schema.Field.of(VERSIONID, Schema.Type.LONG),
                Schema.Field.of(TOPOLOGYID, Schema.Type.LONG),
                Schema.Field.of(TOPOLOGY_COMPONENT_BUNDLE_ID, Schema.Type.LONG),
                Schema.Field.of(NAME, Schema.Type.STRING),
                Schema.Field.of(DESCRIPTION, Schema.Type.STRING),
                Schema.Field.of(CONFIG_DATA, Schema.Type.STRING));
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public Long getTopologyId() {
        return topologyId;
    }

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
    }

    public Long getTopologyComponentBundleId() {
        return topologyComponentBundleId;
    }

    public void setTopologyComponentBundleId(Long topologyComponentBundleId) {
        this.topologyComponentBundleId = topologyComponentBundleId;
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

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    @JsonIgnore
    public String getConfigData() throws Exception {
        if (config != null) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(config);
        }
        return "";
    }

    @JsonIgnore
    public void setConfigData(String configData) throws Exception {
        if (!StringUtils.isEmpty(configData)) {
            ObjectMapper mapper = new ObjectMapper();
            config = mapper.readValue(configData, Config.class);
        }
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.remove(CONFIG);
        try {
            map.put(CONFIG_DATA, getConfigData());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("timestamp")
    public Long getVersionTimestamp() {
        return versionTimestamp;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("timestamp")
    public void setVersionTimestamp(Long timestamp) {
        this.versionTimestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopologyComponent that = (TopologyComponent) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return versionId != null ? versionId.equals(that.versionId) : that.versionId == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (versionId != null ? versionId.hashCode() : 0);
        return result;
    }
}
