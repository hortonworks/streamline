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
package com.hortonworks.iotas.layout.design.transform;

import com.google.common.base.Preconditions;
import com.hortonworks.iotas.common.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Inmemory data provider by taking the required data as a Map.
 */
public class InmemoryTransformDataProvider extends TransformDataProvider {

    private final Map<Object, Object> data = new HashMap<>();

    private InmemoryTransformDataProvider() {
        this(Collections.emptyMap());
    }

    public InmemoryTransformDataProvider(Map<Object, Object> data) {
        super(null);
        if(data == null) {
            throw new IllegalArgumentException("data can not be null");
        }

        this.data.putAll(data);
    }

    public Map<Object, Object> getData() {
        return Collections.unmodifiableMap(data);
    }
}
