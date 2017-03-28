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
package com.hortonworks.streamline.streams.security.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@StorableEntity
public class RoleHierarchy extends AbstractStorable {
    public static final String NAMESPACE = "role_hierarchy";
    public static final String PARENT_ID = "parentId";
    public static final String CHILD_ID = "childId";

    private Long parentId;
    private Long childId;

    @JsonIgnore
    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(PARENT_ID, Schema.Type.LONG), this.parentId);
        fieldToObjectMap.put(new Schema.Field(CHILD_ID, Schema.Type.LONG), this.childId);
        return new PrimaryKey(fieldToObjectMap);
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getChildId() {
        return childId;
    }

    public void setChildId(Long childId) {
        this.childId = childId;
    }

    // for jackson to serialize properly
    @Override
    public Long getId() {
        return null;
    }

    @Override
    public void setId(Long id) {
        // noop
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoleHierarchy that = (RoleHierarchy) o;

        if (parentId != null ? !parentId.equals(that.parentId) : that.parentId != null) return false;
        return childId != null ? childId.equals(that.childId) : that.childId == null;

    }

    @Override
    public int hashCode() {
        int result = parentId != null ? parentId.hashCode() : 0;
        result = 31 * result + (childId != null ? childId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RoleHierarchy{" +
                "parentId=" + parentId +
                ", childId=" + childId +
                "} " + super.toString();
    }
}
