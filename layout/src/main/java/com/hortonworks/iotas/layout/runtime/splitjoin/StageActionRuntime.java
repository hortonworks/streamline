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
package com.hortonworks.iotas.layout.runtime.splitjoin;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.layout.design.rule.action.Action;
import com.hortonworks.iotas.layout.design.rule.action.TransformAction;
import com.hortonworks.iotas.layout.design.splitjoin.StageAction;
import com.hortonworks.iotas.layout.design.transform.Transform;
import com.hortonworks.iotas.layout.runtime.RuntimeService;
import com.hortonworks.iotas.layout.runtime.TransformActionRuntime;
import com.hortonworks.iotas.layout.runtime.rule.action.AbstractActionRuntime;
import com.hortonworks.iotas.layout.runtime.rule.action.ActionRuntime;
import com.hortonworks.iotas.layout.runtime.rule.action.ActionRuntimeContext;

import java.util.List;
import java.util.Set;

/**
 * {@link ActionRuntime} of a stage processor.
 *
 */
public class StageActionRuntime extends AbstractActionRuntime {

    private final StageAction stageAction;
    private TransformActionRuntime transformActionRuntime;

    public StageActionRuntime(StageAction stageAction) {
        this.stageAction = stageAction;
        buildTransformActionRuntime();
    }

    protected void buildTransformActionRuntime() {
        final List<Transform> transforms = stageAction.getTransforms();
        if(stageAction.getOutputStreams().size() != 1) {
            throw new RuntimeException("Stage can only have one output stream.");
        }
        transformActionRuntime = new TransformActionRuntime(new TransformAction(transforms, stageAction.getOutputStreams()));
    }

    @Override
    public void setActionRuntimeContext(ActionRuntimeContext actionRuntimeContext) {
        transformActionRuntime.setActionRuntimeContext(actionRuntimeContext);
    }

    @Override
    public List<Result> execute(IotasEvent input) {
        return transformActionRuntime.execute(input);
    }

    @Override
    public Set<String> getOutputStreams() {
        return transformActionRuntime.getOutputStreams();
    }

    public static class Factory implements RuntimeService.Factory<ActionRuntime, Action> {
        @Override
        public ActionRuntime create(Action action) {
            return new StageActionRuntime((StageAction) action);
        }
    }
}
