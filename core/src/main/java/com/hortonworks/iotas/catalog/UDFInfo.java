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
package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.catalog.AbstractStorable;
import static com.hortonworks.iotas.streams.layout.component.rule.expression.Udf.Type;

import java.util.HashMap;
import java.util.Map;

public class UDFInfo extends AbstractStorable {
    private static final String NAMESPACE = "udfs";

    private Long id;
    private String name;
    private String description;
    private Type type;
    private String className;
    private String jarStoragePath;

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
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    public boolean isAggregate() {
        return type == Type.AGGREGATE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UDFInfo udfInfo = (UDFInfo) o;

        if (id != null ? !id.equals(udfInfo.id) : udfInfo.id != null) return false;
        if (name != null ? !name.equals(udfInfo.name) : udfInfo.name != null) return false;
        if (description != null ? !description.equals(udfInfo.description) : udfInfo.description != null) return false;
        if (type != udfInfo.type) return false;
        if (className != null ? !className.equals(udfInfo.className) : udfInfo.className != null) return false;
        return jarStoragePath != null ? jarStoragePath.equals(udfInfo.jarStoragePath) : udfInfo.jarStoragePath == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (className != null ? className.hashCode() : 0);
        result = 31 * result + (jarStoragePath != null ? jarStoragePath.hashCode() : 0);
        return result;
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
                '}';
    }
}
