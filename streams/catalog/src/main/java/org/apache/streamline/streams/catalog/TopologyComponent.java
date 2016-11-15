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
    public static final String TOPOLOGYID = "topologyId";
    public static final String TOPOLOGY_COMPONENT_BUNDLE_ID = "topologyComponentBundleId";
    public static final String NAME = "name";
    public static final String CONFIG = "config";
    public static final String CONFIG_DATA = "configData";

    private Long id;
    private Long topologyId = -1L;
    private Long topologyComponentBundleId = -1L;
    private String name = StringUtils.EMPTY;
    private Config config;

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
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
                Schema.Field.of(TOPOLOGYID, Schema.Type.LONG),
                Schema.Field.of(TOPOLOGY_COMPONENT_BUNDLE_ID, Schema.Type.LONG),
                Schema.Field.of(NAME, Schema.Type.STRING),
                Schema.Field.of(CONFIG_DATA, Schema.Type.STRING));
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

}
