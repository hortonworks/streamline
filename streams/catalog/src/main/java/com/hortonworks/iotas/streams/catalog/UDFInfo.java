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
package com.hortonworks.iotas.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.catalog.AbstractStorable;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static com.hortonworks.iotas.streams.layout.component.rule.expression.Udf.Type;

public class UDFInfo extends AbstractStorable {
    private static final String NAMESPACE = "udfs";

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String CLASSNAME = "className";
    public static final String JARSTORAGEPATH = "jarStoragePath";
    public static final String TYPE = "type";
    public static final String DIGEST = "digest";

    private Long id;
    private String name;
    private String description;
    private Type type;
    private String className;
    private String jarStoragePath;
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

    public void setId(Long id) {
        this.id = id;
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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

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

    @JsonIgnore
    @Override
    public Schema getSchema() {
        return Schema.of(
                Schema.Field.of(ID, Schema.Type.LONG),
                Schema.Field.of(NAME, Schema.Type.STRING),
                Schema.Field.of(DESCRIPTION, Schema.Type.STRING),
                Schema.Field.of(TYPE, Schema.Type.STRING),
                Schema.Field.of(CLASSNAME, Schema.Type.STRING),
                Schema.Field.of(JARSTORAGEPATH, Schema.Type.STRING),
                Schema.Field.of(DIGEST, Schema.Type.STRING)
        );
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        try {
            map.put("type", type != null ? type.toString() : "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    @Override
    public Storable fromMap(Map<String, Object> map) {
        setId((Long) map.get(ID));
        setName((String) map.get(NAME));
        setDescription((String) map.get(DESCRIPTION));
        setClassName((String) map.get(CLASSNAME));
        setJarStoragePath((String) map.get(JARSTORAGEPATH));
        setDigest((String) map.get(DIGEST));
        String typeStr = (String) map.get(TYPE);
        if (!StringUtils.isEmpty(typeStr)) {
            setType(Enum.valueOf(Type.class, typeStr));
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UDFInfo udfInfo = (UDFInfo) o;

        return id != null ? id.equals(udfInfo.id) : udfInfo.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "UDFInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", className='" + className + '\'' +
                ", jarStoragePath='" + jarStoragePath + '\'' +
                ", digest='" + digest + '\'' +
                "}";
    }
}
