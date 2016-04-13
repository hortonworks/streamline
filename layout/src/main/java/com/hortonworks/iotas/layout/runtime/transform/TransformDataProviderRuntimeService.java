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
package com.hortonworks.iotas.layout.runtime.transform;

import com.hortonworks.iotas.layout.design.transform.InmemoryTransformDataProvider;
import com.hortonworks.iotas.layout.design.transform.TransformDataProvider;
import com.hortonworks.iotas.layout.runtime.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to create {@link TransformDataProviderRuntime} instances of a given {@link TransformDataProvider}
 * by using respective factory
 */
public class TransformDataProviderRuntimeService extends RuntimeService<TransformDataProviderRuntime, TransformDataProvider> {
    private static final Logger log = LoggerFactory.getLogger(TransformDataProviderRuntimeService.class);

    private static Map<Class<? extends TransformDataProvider>, RuntimeService.Factory<TransformDataProviderRuntime, TransformDataProvider>> transformFactories = new ConcurrentHashMap<>();

    static {
        // register factories
        // todo this can be moved to startup listener to add all supported DataProviders.
        // factories instance can be taken as an argument
        transformFactories.put(InmemoryTransformDataProvider.class, new InmemoryTransformDataProviderRuntime.Factory());

        log.info("Registered factories : [{}]", transformFactories);
    }

    private static TransformDataProviderRuntimeService instance = new TransformDataProviderRuntimeService();

    private TransformDataProviderRuntimeService() {
        super(transformFactories);
    }

    public static TransformDataProviderRuntimeService get() {
        return instance;
    }

}
