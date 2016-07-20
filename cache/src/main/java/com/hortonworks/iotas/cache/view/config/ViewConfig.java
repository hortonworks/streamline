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

public class ViewConfig {
    private String id;
    private ExpiryPolicy expiryPolicy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("expiry-policy")
    public ExpiryPolicy getExpiryPolicy() {
        return expiryPolicy;
    }

    @JsonProperty("expiry-policy")
    public void setExpiryPolicy(ExpiryPolicy expiryPolicy) {
        this.expiryPolicy = expiryPolicy;
    }

    public class RedisViewConfig extends ViewConfig {
        private TypeConfig.RedisDatatype redisDatatype;
        private String key;

        @JsonProperty("type")
        public TypeConfig.RedisDatatype getRedisDatatype() {
            return redisDatatype;
        }

        @JsonProperty("type")
        public void setRedisDatatype(TypeConfig.RedisDatatype redisDatatype) {
            this.redisDatatype = redisDatatype;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}
