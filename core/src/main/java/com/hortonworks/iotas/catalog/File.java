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

import java.util.HashMap;
import java.util.Map;

/**
 * File configuration
 */
public class File extends AbstractStorable {
    public static final String NAME_SPACE = "files";
    public static final String ID = "id";

    /**
     * Unique Id for a jar instance. This is the primary key column.
     */
    private Long id;

    /**
     * Human readable name.
     * (name, version) pair is unique constraint.
     */
    private String name;

    /**
     * Name of the jar in the configured storage.
     */
    private String storedFileName;

    /**
     * Jar version.
     * (name, version) pair is unique constraint.
     */
    private Long version;

    /**
     * Time at which this jar was created/updated.
     */
    private Long timestamp;

    /**
     * Extra information about the Jar.
     */
    private String auxiliaryInfo;

    @Override
    @JsonIgnore
    public String getNameSpace() {
        return NAME_SPACE;
    }

    @Override
    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldObjectMap = new HashMap<>();
        fieldObjectMap.put(new Schema.Field(ID, Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldObjectMap);
    }

    /**
     * @return Unique Id for a jar instance, which is the primary key column.
     *
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return Human readable name.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Storage location of the jar.
     */
    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    /**
     * @return version of the Jar.
     */
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * @return the time at which this jar was created/updated.
     */
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return Extra information about this Jar which is represented in String format.
     */
    public String getAuxiliaryInfo() {
        return auxiliaryInfo;
    }

    public void setAuxiliaryInfo(String auxiliaryInfo) {
        this.auxiliaryInfo = auxiliaryInfo;
    }

    @Override
    public String toString() {
        return "File{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", storedFileName='" + storedFileName + '\'' +
                ", version=" + version +
                ", timestamp=" + timestamp +
                ", auxiliaryInfo='" + auxiliaryInfo + '\'' +
                '}'+super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof File)) return false;

        File file = (File) o;

        if (id != null ? !id.equals(file.id) : file.id != null) return false;
        if (name != null ? !name.equals(file.name) : file.name != null) return false;
        if (storedFileName != null ? !storedFileName.equals(file.storedFileName) : file.storedFileName != null)
            return false;
        if (version != null ? !version.equals(file.version) : file.version != null) return false;
        if (timestamp != null ? !timestamp.equals(file.timestamp) : file.timestamp != null) return false;
        return !(auxiliaryInfo != null ? !auxiliaryInfo.equals(file.auxiliaryInfo) : file.auxiliaryInfo != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (storedFileName != null ? storedFileName.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (auxiliaryInfo != null ? auxiliaryInfo.hashCode() : 0);
        return result;
    }
}
