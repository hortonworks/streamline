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

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime abstraction for the action to be taken when a rule matches the condition
 */
public interface ActionRuntime extends Serializable {

    /**
     * This is invoked after this object is constructed before any of the other methods are invoked. Any resources can
     * be initialized using the given {@code actionRuntimeContext}.
     *
     * @param actionRuntimeContext contextual information of {@link ActionRuntime}
     */
    public void setActionRuntimeContext(ActionRuntimeContext actionRuntimeContext);

    /**
     * This is invoked before {@link #execute(IotasEvent)} method is invoked. Any resources can be initialized which are used in
     * processing the received events
     *
     * @param config configuration key/values which could have been configured at action/processor/topology level.
     */
    public void initialize(Map<String, Object> config);

    /**
     * Execute the current action and return a {@link List} of {@link Result}s.
     *
     * @param input the input IotasEvent
     * @return the results
     */
    List<Result> execute(IotasEvent input);


    /**
     * The streams where the result of this action are sent out
     *
     * @return streams where the result of this action are sent out
     */
    Set<String> getOutputStreams();

}
