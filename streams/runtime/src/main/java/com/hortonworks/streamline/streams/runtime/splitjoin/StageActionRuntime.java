/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/

package com.hortonworks.streamline.streams.runtime.splitjoin;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.layout.Transform;
import com.hortonworks.streamline.streams.layout.component.impl.splitjoin.StageAction;
import com.hortonworks.streamline.streams.layout.component.rule.action.Action;
import com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction;
import com.hortonworks.streamline.streams.runtime.RuntimeService;
import com.hortonworks.streamline.streams.runtime.TransformActionRuntime;
import com.hortonworks.streamline.streams.runtime.rule.action.ActionRuntime;
import com.hortonworks.streamline.streams.runtime.rule.action.ActionRuntimeContext;

import java.util.List;
import java.util.Set;

/**
 * {@link ActionRuntime} of a stage processor.
 *
 */
public class StageActionRuntime extends AbstractSplitJoinActionRuntime {

    private final StageAction stageAction;
    private TransformActionRuntime transformActionRuntime;

    public StageActionRuntime(StageAction stageAction) {
        this.stageAction = stageAction;
    }

    @Override
    public void setActionRuntimeContext(ActionRuntimeContext actionRuntimeContext) {
        super.setActionRuntimeContext(actionRuntimeContext);
        buildTransformActionRuntime();
        transformActionRuntime.setActionRuntimeContext(actionRuntimeContext);
    }

    protected void buildTransformActionRuntime() {
        final List<Transform> transforms = stageAction.getTransforms();
        if(stageAction.getOutputStreams().size() != 1) {
            throw new RuntimeException("Stage can only have one output stream.");
        }
        transformActionRuntime = new TransformActionRuntime(new TransformAction(transforms, stageAction.getOutputStreams()));
    }

    @Override
    public List<Result> execute(StreamlineEvent input) {
        return transformActionRuntime.execute(input);
    }

    @Override
    public Set<String> getOutputStreams() {
        return transformActionRuntime.getOutputStreams();
    }

    public static class Factory implements RuntimeService.Factory<ActionRuntime, StageAction> {
        @Override
        public ActionRuntime create(StageAction action) {
            return new StageActionRuntime(action);
        }
    }
}
