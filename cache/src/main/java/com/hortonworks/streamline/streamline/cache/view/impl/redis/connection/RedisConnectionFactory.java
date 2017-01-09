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

package org.apache.streamline.cache.view.impl.redis.connection;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;

public class RedisConnectionFactory<K,V> extends AbstractRedisConnectionFactory<K,V> {
    public RedisConnectionFactory(RedisClient redisClient, RedisCodec<K, V> codec) {
        super(redisClient, codec);
    }

    @Override
    public RedisConnection<K, V> create() {
        return redisClient.connect(codec);
    }
}
