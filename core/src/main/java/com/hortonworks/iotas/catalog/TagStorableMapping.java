package com.hortonworks.iotas.catalog;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;

import java.util.HashMap;
import java.util.Map;

/**
 * This entity is to maintain the many-many relation
 * between the Tag and the Storable entitiy. An ORM entity manager
 * could have done this automatically but since we dont have it
 * this has to be maintained manually.
 */
public class TagStorableMapping extends AbstractStorable {
    private static final String NAMESPACE = "tag_storable_mapping";
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

        TagStorableMapping that = (TagStorableMapping) o;

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
        return "TagStorableMapping{" +
                "tagId=" + tagId +
                ", storableNamespace='" + storableNamespace + '\'' +
                ", storableId=" + storableId +
                "} " + super.toString();
    }
}
