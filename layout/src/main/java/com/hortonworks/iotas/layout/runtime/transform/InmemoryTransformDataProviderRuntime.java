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

/**
 *
 */
public class InmemoryTransformDataProviderRuntime implements TransformDataProviderRuntime {
    private final InmemoryTransformDataProvider inmemoryTransformDataProvider;

    public InmemoryTransformDataProviderRuntime(InmemoryTransformDataProvider inmemoryTransformDataProvider) {
        this.inmemoryTransformDataProvider = inmemoryTransformDataProvider;
    }

    @Override
    public void prepare() {
    }

    @Override
    public Object get(Object key) {
        return inmemoryTransformDataProvider.getData().get(key);
    }

    @Override
    public void cleanup() {
    }

    public static class Factory implements RuntimeService.Factory<TransformDataProviderRuntime, TransformDataProvider> {

        @Override
        public TransformDataProviderRuntime create(TransformDataProvider transformDataProvider) {
            return new InmemoryTransformDataProviderRuntime((InmemoryTransformDataProvider) transformDataProvider);
        }
    }
}
