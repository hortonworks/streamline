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

import java.util.Map;

/**
 * Abstract class which implements {@link #setActionRuntimeContext(ActionRuntimeContext)} and {@link #initialize(Map)}
 */
public abstract class AbstractActionRuntime implements ActionRuntime {
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


    protected String getJarPathFor(String jarId) {
        //todo: IOT-226
        return null;
    }

}
