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

public class ConnectionConfig {
    private String host;
    private String port;

    public ConnectionConfig() {
    }

    public ConnectionConfig(String host, String port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public static class RedisConnectionConfig extends ConnectionConfig {
        private Pool pool;

        public RedisConnectionConfig() {
        }

        public RedisConnectionConfig(String host, String port, Pool pool) {
            super(host, port);
            this.pool = pool;
        }

        public Pool getPool() {
            return pool;
        }

        public void setPool(Pool pool) {
            this.pool = pool;
        }

        public static class Pool {
            private int maxIdle;
            private int maxActive;

            public Pool() {
            }

            public Pool(int maxIdle, int maxActive) {
                this.maxIdle = maxIdle;
                this.maxActive = maxActive;
            }

            @JsonProperty("max-idle")
            public int getMaxIdle() {
                return maxIdle;
            }

            @JsonProperty("max-idle")
            public void setMaxIdle(int maxIdle) {
                this.maxIdle = maxIdle;
            }

            @JsonProperty("max-active")
            public int getMaxActive() {
                return maxActive;
            }

            @JsonProperty("max-active")
            public void setMaxActive(int maxActive) {
                this.maxActive = maxActive;
            }
        }
    }
}
