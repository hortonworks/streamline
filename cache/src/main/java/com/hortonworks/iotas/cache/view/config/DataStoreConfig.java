/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.hortonworks.iotas.cache.view.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataStoreConfig {
    private String id;
    private TypeConfig.DataStore dataStoreType;
    private String namespace;
    private ConnectionConfig connectionConfig;
    private TypeConfig.CacheLoader cacheLoaderType;
    private TypeConfig.CacheReader cacheReader;
    private TypeConfig.CacheWriter cacheWriterType;

    public DataStoreConfig() {
    }

    public DataStoreConfig(String id, TypeConfig.DataStore dataStoreType, ConnectionConfig connectionConfig,
            TypeConfig.CacheLoader cacheLoaderType, TypeConfig.CacheReader cacheReader, TypeConfig.CacheWriter cacheWriterType) {
        this.id = id;
        this.dataStoreType = dataStoreType;
        this.connectionConfig = connectionConfig;
        this.cacheLoaderType = cacheLoaderType;
        this.cacheReader = cacheReader;
        this.cacheWriterType = cacheWriterType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("type")
    public TypeConfig.DataStore getDataStoreType() {
        return dataStoreType;
    }

    @JsonProperty("type")
    public void setDataStoreType(TypeConfig.DataStore dataStoreType) {
        this.dataStoreType = dataStoreType;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @JsonProperty("connection")
    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    @JsonProperty("connection")
    public void setConnectionConfig(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    @JsonProperty("load")
    public TypeConfig.CacheLoader getCacheLoaderType() {
        return cacheLoaderType;
    }

    @JsonProperty("load")
    public void setCacheLoaderType(TypeConfig.CacheLoader cacheLoaderType) {
        this.cacheLoaderType = cacheLoaderType;
    }

    @JsonProperty("read")
    public TypeConfig.CacheReader getCacheReader() {
        return cacheReader;
    }

    @JsonProperty("read")
    public void setCacheReader(TypeConfig.CacheReader cacheReader) {
        this.cacheReader = cacheReader;
    }

    @JsonProperty("write")
    public TypeConfig.CacheWriter getCacheWriterType() {
        return cacheWriterType;
    }

    @JsonProperty("write")
    public void setCacheWriterType(TypeConfig.CacheWriter cacheWriterType) {
        this.cacheWriterType = cacheWriterType;
    }
}
