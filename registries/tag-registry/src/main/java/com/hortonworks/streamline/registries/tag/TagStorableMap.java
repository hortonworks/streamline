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
package com.hortonworks.streamline.registries.tag;

import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.util.HashMap;
import java.util.Map;

/**
 * This entity is to maintain the many-many relation
 * between the Tag and the Storable entitiy. An ORM entity manager
 * could have done this automatically but since we dont have it
 * this has to be maintained manually.
 */
@StorableEntity
public class TagStorableMap extends AbstractStorable {
    public static final String NAMESPACE = "tag_storable_map";
    public static final String FIELD_TAG_ID = "tagId";
    public static final String FIELD_STORABLE_NAMESPACE = "storableNamespace";
    public static final String FIELD_STORABLE_ID = "storableId";

    private Long tagId;
    private String storableNamespace;
    private Long storableId;

    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(FIELD_TAG_ID, Schema.Type.LONG), this.tagId);
        fieldToObjectMap.put(new Schema.Field(FIELD_STORABLE_NAMESPACE, Schema.Type.STRING), this.storableNamespace);
        fieldToObjectMap.put(new Schema.Field(FIELD_STORABLE_ID, Schema.Type.LONG), this.storableId);
        return new PrimaryKey(fieldToObjectMap);
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getStorableNamespace() {
        return storableNamespace;
    }

    public void setStorableNamespace(String storableNamespace) {
        this.storableNamespace = storableNamespace;
    }

    public Long getStorableId() {
        return storableId;
    }

    public void setStorableId(Long storableId) {
        this.storableId = storableId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TagStorableMap that = (TagStorableMap) o;

        if (tagId != null ? !tagId.equals(that.tagId) : that.tagId != null) return false;
        if (storableNamespace != null ? !storableNamespace.equals(that.storableNamespace) : that.storableNamespace != null)
            return false;
        return storableId != null ? storableId.equals(that.storableId) : that.storableId == null;

    }

    @Override
    public int hashCode() {
        int result = tagId != null ? tagId.hashCode() : 0;
        result = 31 * result + (storableNamespace != null ? storableNamespace.hashCode() : 0);
        result = 31 * result + (storableId != null ? storableId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TagStorableMap{" +
                "tagId=" + tagId +
                ", storableNamespace='" + storableNamespace + '\'' +
                ", storableId=" + storableId +
                "} " + super.toString();
    }
}
