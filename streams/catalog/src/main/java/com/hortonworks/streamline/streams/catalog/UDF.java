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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.registries.storage.annotation.StorableEntity;
import com.hortonworks.registries.storage.PrimaryKey;
import com.hortonworks.registries.storage.Storable;
import com.hortonworks.registries.storage.catalog.AbstractStorable;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;


import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.streamline.streams.layout.component.rule.expression.Udf.Type;

@StorableEntity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UDF extends AbstractStorable {
    public static final String NAMESPACE = "udf";

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DISPLAYNAME = "displayName";
    public static final String DESCRIPTION = "description";
    public static final String CLASSNAME = "className";
    public static final String JARSTORAGEPATH = "jarStoragePath";
    public static final String TYPE = "type";
    public static final String DIGEST = "digest";
    public static final String ARGTYPES = "argTypes";
    public static final String RETURNTYPE = "returnType";
    public static final String BUILTIN = "builtin";

    private Long id;
    private String name;
    private String displayName;
    private String description;
    private Type type;
    private String className;
    private String jarStoragePath;
    private List<String> argTypes;
    private Schema.Type returnType;
    private Boolean builtin = false;

    /**
     * The jar file digest which can be used to de-dup jar files.
     * If a newly submitted jar's digest matches with that of an already
     * existing jar, we just use that jar file path rather than storing a copy.
     */
    private String digest;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Should be fqdn class name - If canonical form of an inner-class name was provided at
     * upload time, it will be converted to the fqdn name (with '$') during upload processing.
     * @return class name
     */
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @JsonIgnore
    public String getJarStoragePath() {
        return jarStoragePath;
    }

    @JsonIgnore
    public void setJarStoragePath(String jarStoragePath) {
        this.jarStoragePath = jarStoragePath;
    }

    @JsonIgnore
    public String getNameSpace() {
        return NAMESPACE;
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    public boolean isAggregate() {
        return type == Type.AGGREGATE;
    }

    /**
     * The jar file digest which can be used to de-dup jar files.
     * If a newly submitted jar's digest matches with that of an already
     * existing jar, we just use that jar file path rather than storing a copy.
     */
    @JsonIgnore
    public String getDigest() {
        return digest;
    }

    @JsonIgnore
    public void setDigest(String digest) {
        this.digest = digest;
    }

    public List<String> getArgTypes() {
        return argTypes;
    }

    public void setArgTypes(List<String> argTypes) {
        this.argTypes = argTypes;
    }

    public Schema.Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Schema.Type returnType) {
        this.returnType = returnType;
    }

    public Boolean getBuiltin() {
        return builtin;
    }

    public void setBuiltin(Boolean builtin) {
        this.builtin = builtin;
    }

    @JsonIgnore
    @Override
    public Schema getSchema() {
        return Schema.of(
                Schema.Field.of(ID, Schema.Type.LONG),
                Schema.Field.of(NAME, Schema.Type.STRING),
                Schema.Field.of(DISPLAYNAME, Schema.Type.STRING),
                Schema.Field.of(DESCRIPTION, Schema.Type.STRING),
                Schema.Field.of(TYPE, Schema.Type.STRING),
                Schema.Field.of(CLASSNAME, Schema.Type.STRING),
                Schema.Field.of(JARSTORAGEPATH, Schema.Type.STRING),
                Schema.Field.of(DIGEST, Schema.Type.STRING),
                Schema.Field.of(ARGTYPES, Schema.Type.STRING),
                Schema.Field.of(RETURNTYPE, Schema.Type.STRING),
                Schema.Field.of(BUILTIN, Schema.Type.STRING)
                );
    }

    @Override
    public Map<String, Object> toMap() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = super.toMap();
        try {
            map.put(TYPE, type != null ? type.toString() : "");
            map.put(ARGTYPES, argTypes != null ? mapper.writerFor(new TypeReference<List<String>>() {
            }).writeValueAsString(argTypes) : "");
            map.put(RETURNTYPE, returnType != null ? returnType.toString() : "");
            map.put(BUILTIN, builtin.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    @Override
    public Storable fromMap(Map<String, Object> map) {
        ObjectMapper mapper = new ObjectMapper();
        setId((Long) map.get(ID));
        setName((String) map.get(NAME));
        setDisplayName((String) map.get(DISPLAYNAME));
        setDescription((String) map.get(DESCRIPTION));
        setClassName((String) map.get(CLASSNAME));
        setJarStoragePath((String) map.get(JARSTORAGEPATH));
        setDigest((String) map.get(DIGEST));
        if (map.get(BUILTIN) != null) {
            setBuiltin(Boolean.valueOf(((String) map.get(BUILTIN)).trim()));
        }

        String typeStr = (String) map.get(TYPE);
        try {
            if (!StringUtils.isEmpty(typeStr)) {
                setType(Enum.valueOf(Type.class, typeStr));
            }
            String argTypesStr = (String) map.get(ARGTYPES);
            if (!StringUtils.isEmpty(argTypesStr)) {
                List<String> argTypes = mapper.readValue(argTypesStr, new TypeReference<List<String>>() {
                });
                setArgTypes(argTypes);
            } else {
                setArgTypes(Collections.<String>emptyList());
            }
            String returnTypeStr = (String) map.get(RETURNTYPE);
            if (!StringUtils.isEmpty(returnTypeStr)) {
                setReturnType(Enum.valueOf(Schema.Type.class, returnTypeStr));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UDF udf = (UDF) o;

        return id != null ? id.equals(udf.id) : udf.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "UDF{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", className='" + className + '\'' +
                ", jarStoragePath='" + jarStoragePath + '\'' +
                ", argTypes=" + argTypes +
                ", returnType=" + returnType +
                ", builtin=" + builtin +
                ", digest='" + digest + '\'' +
                '}';
    }
}
