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

package com.hortonworks.streamline.registries.model.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.common.Schema.Field;
import com.hortonworks.streamline.common.Schema.Type;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

/*
** Storable Entity for saving the pmml model details.
 */
@StorableEntity
@JsonIgnoreProperties(ignoreUnknown = true)
public final class MLModelInfo extends AbstractStorable {
    public static final String NAME = "name";
    private static final String NAME_SPACE = "ml_models";
    private static final String ID = "id";
    private Long id;
    private Long timestamp;
    private String name;
    private String pmml;
    private String uploadedFileName;

    public MLModelInfo() {
    }

    public void setPmml(String pmml) {
        this.pmml = pmml;
    }

    public String getPmml() {
        return pmml;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getNameSpace() {
        return NAME_SPACE;
    }

    public String getUploadedFileName() {
        return uploadedFileName;
    }

    public void setUploadedFileName(String fileName) {
        this.uploadedFileName = fileName;
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Field, Object> fieldObjectMap = new HashMap<>();
        fieldObjectMap.put(new Schema.Field(ID, Type.LONG), this.id);
        return new PrimaryKey(fieldObjectMap);
    }
}
