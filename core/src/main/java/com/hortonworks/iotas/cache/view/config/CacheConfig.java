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

import java.util.List;

public class CacheConfig {
    private String id;
    private TypeConfig.Cache cacheType;
    private CacheEntry cacheEntry;
    private ConnectionConfig connectionConfig;
    private DataStoreConfig dataStore;
    private ExpiryPolicy expiryPolicy;
    List<ViewConfig> viewsConfig;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("type")
    public TypeConfig.Cache getCacheType() {
        return cacheType;
    }

    @JsonProperty("type")
    public void setCacheType(TypeConfig.Cache cacheType) {
        this.cacheType = cacheType;
    }

    @JsonProperty("entry")
    public CacheEntry getCacheEntry() {
        return cacheEntry;
    }

    @JsonProperty("entry")
    public void setCacheEntry(CacheEntry cacheEntry) {
        this.cacheEntry = cacheEntry;
    }

    @JsonProperty("connection")
    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    @JsonProperty("connection")
    public void setConnectionConfig(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    @JsonProperty("data-store")
    public DataStoreConfig getDataStore() {
        return dataStore;
    }

    @JsonProperty("data-store")
    public void setDataStore(DataStoreConfig dataStore) {
        this.dataStore = dataStore;
    }

    @JsonProperty("expiry-policy")
    public ExpiryPolicy getExpiryPolicy() {
        return expiryPolicy;
    }

    @JsonProperty("expiry-policy")
    public void setExpiryPolicy(ExpiryPolicy expiryPolicy) {
        this.expiryPolicy = expiryPolicy;
    }

    @JsonProperty("views")
    public List<ViewConfig> getViewsConfig() {
        return viewsConfig;
    }

    @JsonProperty("views")
    public void setViewsConfig(List<ViewConfig> viewsConfig) {
        this.viewsConfig = viewsConfig;
    }
}
