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
package com.hortonworks.iotas.layout.runtime.rule.action;

import com.hortonworks.iotas.util.ProxyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Abstract class of {@link ActionRuntime} which implements {@link #setActionRuntimeContext(ActionRuntimeContext)} and {@link #initialize(Map)}
 */
public abstract class AbstractActionRuntime implements ActionRuntime {
    private static final Logger log = LoggerFactory.getLogger(AbstractActionRuntime.class);

    protected ActionRuntimeContext actionRuntimeContext;
    protected Map<String, Object> config;


    @Override
    public void setActionRuntimeContext(ActionRuntimeContext actionRuntimeContext) {
        this.actionRuntimeContext = actionRuntimeContext;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        this.config = config;
    }

    protected String getJarPathFor(Long jarId) {
        // IOT-226 takes care of this.
        return null;
    }

    /**
     * Creates an instance of the given class which is loaded from the given jar or current class loader if {@code jarId} is null.
     *
     * @param jarId id of the jar resource
     * @param fqcn FullyQualifiedClassName of the object to be created
     * @param klass Class instance of the object to be created
     * @param <T> type of the object to be created
     * @return
     */
    protected <T> T getInstance(Long jarId, String fqcn, Class<T> klass) {
        T instance = null;
        if (fqcn != null) {
            try {
                if (jarId != null) {
                    ProxyUtil<T> proxyUtil = new ProxyUtil<>(klass, this.getClass().getClassLoader());
                    String jarPath = getJarPathFor(jarId);
                    instance = proxyUtil.loadClassFromJar(jarPath, fqcn);
                } else {
                    // FQCN is given but no jarId then that class is assumed to be accessible from current class loader.
                    try {
                        instance = (T) Class.forName(fqcn, true, Thread.currentThread().getContextClassLoader()).newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return instance;
    }
}
