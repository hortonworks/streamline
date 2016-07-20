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

package com.hortonworks.iotas.cache.view.service;

import com.hortonworks.iotas.cache.view.Factory;
import com.hortonworks.iotas.cache.view.config.CacheConfig;
import com.hortonworks.iotas.cache.view.config.ConnectionConfig;
import com.hortonworks.iotas.cache.view.config.DataStoreConfig;
import com.hortonworks.iotas.cache.view.config.ExpiryPolicy;
import com.hortonworks.iotas.cache.view.config.TypeConfig;
import com.hortonworks.iotas.cache.view.datastore.DataStoreReader;
import com.hortonworks.iotas.cache.view.datastore.DataStoreWriter;
import com.hortonworks.iotas.cache.view.datastore.phoenix.PhoenixDataStore;
import com.hortonworks.iotas.cache.view.impl.redis.connection.RedisConnectionFactory;
import com.hortonworks.iotas.cache.view.impl.redis.connection.RedisConnectionPoolFactory;
import com.hortonworks.iotas.cache.view.io.loader.CacheLoaderAsyncFactory;
import com.hortonworks.iotas.cache.view.io.loader.CacheLoaderFactory;
import com.hortonworks.iotas.cache.view.io.loader.CacheLoaderSyncFactory;
import com.hortonworks.iotas.cache.view.io.writer.CacheWriter;
import com.hortonworks.iotas.cache.view.io.writer.CacheWriterAsync;
import com.hortonworks.iotas.cache.view.io.writer.CacheWriterSync;
import com.hortonworks.iotas.cache.view.service.registry.CacheServiceLocalRegistry;
import com.hortonworks.iotas.common.util.ReflectionHelper;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;

import java.util.Arrays;

public class RedisCacheServiceBuilder {
    private CacheConfig cacheConfig;

    public RedisCacheServiceBuilder(CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
    }

    public void register() {
        CacheServiceLocalRegistry.INSTANCE.register(new CacheServiceId(cacheConfig.getId()), getCacheService());
    }

    private <T extends CacheService> T getCacheService()  {
        TypeConfig.Cache cacheType = cacheConfig.getCacheType();
        switch (cacheType) {
            case REDIS:
                return (T) getRedisCacheService();
            case GUAVA:
                return (T) getGuavaCacheService();
            case MEMCACHED:
            default:
                throw new IllegalStateException("Invalid cache option. " + cacheType
                        + ". Valid options are " + Arrays.toString(TypeConfig.Cache.values()));
        }
    }


    private RedisCacheService getRedisCacheService() {
        final String cacheServiceId = cacheConfig.getId();
        final TypeConfig.Cache cacheType = cacheConfig.getCacheType();
        return (RedisCacheService) new RedisCacheService.Builder(cacheServiceId, cacheType, getRedisConnectionFactory())
                .setCacheLoaderFactory(getCacheLoaderFactory())
                .setCacheWriter(getCacheWriter(getDataStoreWriter(getNamespace())))
                .setDataStoreReader(getDataStoreReader(getNamespace()))
                .setExpiryPolicy(getExpiryPolicy())
                .build();
    }

    private CacheService getGuavaCacheService() {
        throw new UnsupportedOperationException("Must implement Guava Cache Service");
    }

    private ExpiryPolicy getExpiryPolicy() {
        return cacheConfig.getExpiryPolicy();
    }

    private String getNamespace() {
        return cacheConfig.getDataStore().getNamespace();
    }

    private DataStoreWriter getDataStoreWriter(String namespace) {
        final TypeConfig.DataStore dataStoreType = cacheConfig.getDataStore().getDataStoreType();
        switch (dataStoreType) {
            case PHOENIX:
                return new PhoenixDataStore<>(namespace);
            case MYSQL:
                return null;
            case HBASE:
                return null;
            default:
                throw new IllegalStateException("Invalid DataStore option. " + dataStoreType
                        + ". Valid options are " + Arrays.toString(TypeConfig.DataStore.values()));
        }
    }

    private DataStoreReader<Object, Object> getDataStoreReader(String namespace) {
        final TypeConfig.DataStore dataStoreType = cacheConfig.getDataStore().getDataStoreType();
        switch (dataStoreType) {
            case PHOENIX:
                return new PhoenixDataStore<>(namespace);
            case MYSQL:
                return null;
            case HBASE:
                return null;
            default:
                throw new IllegalStateException("Invalid DataStore option. " + dataStoreType
                        + ". Valid options are " + Arrays.toString(TypeConfig.DataStore.values()));
        }
    }

    private CacheWriter getCacheWriter(DataStoreWriter dataStoreWriter) {
        final TypeConfig.CacheWriter cacheWriterType = cacheConfig.getDataStore().getCacheWriterType();
        switch (cacheWriterType) {
            case SYNC:
                return new CacheWriterSync(dataStoreWriter);
            case ASYNC:
                return new CacheWriterAsync(dataStoreWriter);
            default:
                throw new IllegalStateException("Invalid CacheWriter option. " + cacheWriterType
                        + ". Valid options are " + Arrays.toString(TypeConfig.CacheWriter.values()));
        }
    }

    private CacheLoaderFactory getCacheLoaderFactory() {
        final DataStoreConfig dataStore = cacheConfig.getDataStore();
        if (dataStore != null) {
            final TypeConfig.CacheLoader cacheLoaderType = dataStore.getCacheLoaderType();
            switch (cacheLoaderType) {
                case SYNC:
                    return new CacheLoaderSyncFactory<>();
                case ASYNC:
                    return new CacheLoaderAsyncFactory<>();
                default:
                    throw new IllegalStateException("Invalid CacheLoader option. " + cacheLoaderType
                            + ". Valid options are " + Arrays.toString(TypeConfig.CacheLoader.values()));
            }
        }
        return null;
    }

    private Factory<RedisConnection> getRedisConnectionFactory() {
        final ConnectionConfig.RedisConnectionConfig connectionConfig = (ConnectionConfig.RedisConnectionConfig) cacheConfig.getConnectionConfig();

        if (connectionConfig != null) {
            if (connectionConfig.getPool() != null) {
                return new RedisConnectionPoolFactory(RedisClient.create(getRedisUri()), getRedisCodec());
            } else {
                return new RedisConnectionFactory(RedisClient.create(getRedisUri()), getRedisCodec());
            }
        }
        return null;
    }

    private RedisCodec getRedisCodec() {
        final String codec = cacheConfig.getCacheEntry().getCodec();
        if (codec != null) {
            return new Utf8StringCodec();
        } else {
            try {
                return ReflectionHelper.newInstance(codec);
            } catch (Exception e) {
                throw new RuntimeException("Exception occurred creating codec", e);
            }
        }
    }

    private String getRedisUri() {
        ConnectionConfig.RedisConnectionConfig rcc = (ConnectionConfig.RedisConnectionConfig) cacheConfig.getConnectionConfig();
        return "redis://" +  rcc.getHost() + ":" + rcc.getPort();
    }
}
