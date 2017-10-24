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
 * For storing the per user customization of the editor toolbar that contains the various
 * sources sinks and processors.
 */
@StorableEntity
public class TopologyEditorToolbar extends AbstractStorable {
    public static final String NAMESPACE = "topology_editor_toolbar";
    public static final String USER_ID = "userId";
    public static final String DATA = "data";
    public static final String TIMESTAMP = "timestamp";

    private Long userId;
    private String data;
    private Long timestamp;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
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
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(USER_ID, Schema.Type.LONG), userId);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    @Override
    public Long getId() {
        return super.getId();
    }
}
