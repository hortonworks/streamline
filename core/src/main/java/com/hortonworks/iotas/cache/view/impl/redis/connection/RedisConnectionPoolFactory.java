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

package com.hortonworks.iotas.cache.view.impl.redis.connection;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionPool;
import com.lambdaworks.redis.codec.RedisCodec;

public class RedisConnectionPoolFactory<K,V> extends AbstractRedisConnectionFactory<K,V> {
    // Defaults for Lettuce Redis Client 3.4.2
    private static final int MAX_IDLE = 5;
    private static final int MAX_ACTIVE = 20;

    private final int maxIdle;
    private final int maxActive;

    public RedisConnectionPoolFactory(RedisClient redisClient, RedisCodec<K, V> codec, int maxIdle, int maxActive) {
        super(redisClient, codec);
        this.maxIdle = maxIdle;
        this.maxActive = maxActive;
    }

    public RedisConnectionPoolFactory(RedisClient redisClient, RedisCodec<K, V> codec) {
        this(redisClient, codec, MAX_IDLE, MAX_ACTIVE);
    }

    public RedisConnection<K, V> create() {
        return redisClient.pool(codec, MAX_IDLE, MAX_ACTIVE).allocateConnection();
    }
}
