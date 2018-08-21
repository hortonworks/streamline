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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.annotation.SchemaIgnore;
import com.hortonworks.streamline.storage.annotation.StorableEntity;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;
import com.hortonworks.streamline.streams.security.Permission;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


@JsonInclude(JsonInclude.Include.NON_NULL)
@StorableEntity
public class AclEntry extends AbstractStorable {
    public static final String NAMESPACE = "acl_entry";
    public static final String ID = "id";
    public static final String OBJECT_ID = "objectId";
    public static final String OBJECT_NAMESPACE = "objectNamespace";
    public static final String SID_ID = "sidId";
    public static final String SID_TYPE = "sidType";
    public static final String PERMISSIONS = "permissions";
    public static final String OWNER = "owner";
    public static final String GRANT = "grant";
    public static final String TIMESTAMP = "timestamp";

    public enum SidType {USER, ROLE}

    private Long id;
    private Long objectId;
    private String objectNamespace;
    private Long sidId; // refers to user id or role id
    @SchemaIgnore
    private String sidName; // if client wants to pass sid name (unique) instead of id
    private SidType sidType;
    private EnumSet<Permission> permissions;
    private boolean owner;
    private boolean grant;
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
        fieldToObjectMap.put(new Schema.Field(ID, Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public String getObjectNamespace() {
        return objectNamespace;
    }

    public void setObjectNamespace(String objectNamespace) {
        this.objectNamespace = objectNamespace;
    }

    public Long getSidId() {
        return sidId;
    }

    public void setSidId(Long sidId) {
        this.sidId = sidId;
    }

    public SidType getSidType() {
        return sidType;
    }

    public void setSidType(SidType sidType) {
        this.sidType = sidType;
    }

    public EnumSet<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(EnumSet<Permission> permissions) {
        this.permissions = EnumSet.copyOf(permissions);
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonIgnore
    @Override
    public Schema getSchema() {
        return Schema.of(
                Schema.Field.of(ID, Schema.Type.LONG),
                Schema.Field.of(OBJECT_ID, Schema.Type.LONG),
                Schema.Field.of(OBJECT_NAMESPACE, Schema.Type.STRING),
                Schema.Field.of(SID_ID, Schema.Type.LONG),
                Schema.Field.of(SID_TYPE, Schema.Type.STRING),
                Schema.Field.of(PERMISSIONS, Schema.Type.STRING),
                Schema.Field.of(OWNER, Schema.Type.BOOLEAN),
                Schema.Field.of(GRANT, Schema.Type.BOOLEAN),
                Schema.Field.of(TIMESTAMP, Schema.Type.LONG)
        );
    }

    @Override
    public Storable fromMap(Map<String, Object> map) {
        ObjectMapper mapper = new ObjectMapper();
        setId((Long) map.get(ID));
        setObjectId((Long) map.get(OBJECT_ID));
        setObjectNamespace((String) map.get(OBJECT_NAMESPACE));
        setSidId((Long) map.get(SID_ID));
        setSidType(Enum.valueOf(SidType.class, (String) map.get(SID_TYPE)));
        String permissionsStr = (String) map.get(PERMISSIONS);
        if (!StringUtils.isEmpty(permissionsStr)) {
            EnumSet<Permission> permissions;
            try {
                permissions = mapper.readValue(permissionsStr, new TypeReference<EnumSet<Permission>>() {
                });
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            setPermissions(permissions);
        }
        setOwner((Boolean) map.get(OWNER));
        setGrant((Boolean) map.get(GRANT));
        setTimestamp((Long) map.get(TIMESTAMP));
        return this;
    }

    @Override
    public Map<String, Object> toMap() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = super.toMap();
        map.put(SID_TYPE, sidType != null ? sidType.toString() : "");
        try {
            map.put(PERMISSIONS, permissions != null ? mapper.writerFor(new TypeReference<EnumSet<Permission>>() {
            }).writeValueAsString(permissions) : "");
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
        return map;
    }

    // needed for storage manager
    public boolean getOwner() {
        return isOwner();
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    // needed for storage manager
    public boolean getGrant() {
        return isGrant();
    }

    public boolean isGrant() {
        return grant;
    }

    public void setGrant(boolean grant) {
        this.grant = grant;
    }

    public String getSidName() {
        return sidName;
    }

    public void setSidName(String sidName) {
        this.sidName = sidName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AclEntry aclEntry = (AclEntry) o;

        return id != null ? id.equals(aclEntry.id) : aclEntry.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AclEntry{" +
                "id=" + id +
                ", objectId=" + objectId +
                ", objectNamespace='" + objectNamespace + '\'' +
                ", sidId=" + sidId +
                ", sidType=" + sidType +
                ", permissions=" + permissions +
                ", owner=" + owner +
                ", grant=" + grant +
                ", timestamp=" + timestamp +
                "} " + super.toString();
    }
}
