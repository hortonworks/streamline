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
package com.hortonworks.iotas.layout.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * RuntimeService to create runtime instances of {@code R} for a given design time instance of {@code D}.
 */
public class RuntimeService<R, D> {
    private static final Logger log = LoggerFactory.getLogger(RuntimeService.class);

    protected Map<Class<? extends D>, Factory<R, D>> factories;

    /**
     *
     * @param factories factories to be registered
     */
    protected RuntimeService(Map<Class<? extends D>, Factory<R, D>> factories) {
        this.factories = factories;
    }

    /**
     * Return an instance of {@code r} for a given {@code d}
     *
     * @param d
     */
    public R get(D d) {
        final Factory<R, D> transformFactory = factories.get(d.getClass());
        if (transformFactory == null) {
            log.error("No factory is registered for [{}]", d.getClass());
            throw new IllegalArgumentException("No runtime factory is registered for "+d.getClass());
        }

        return transformFactory.create(d);
    }

    /**
     * Factory to create an instance of {@link R} for a given {@link D}
     *
     * @param <R>
     * @param <D>
     */
    public interface Factory<R, D> {
        public R create(D d);
    }
}
