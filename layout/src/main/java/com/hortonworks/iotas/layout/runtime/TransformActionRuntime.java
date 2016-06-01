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

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.layout.design.rule.action.Action;
import com.hortonworks.iotas.layout.design.rule.action.TransformAction;
import com.hortonworks.iotas.layout.design.transform.Transform;
import com.hortonworks.iotas.layout.runtime.rule.action.AbstractActionRuntime;
import com.hortonworks.iotas.layout.runtime.rule.action.ActionRuntime;
import com.hortonworks.iotas.layout.runtime.rule.action.ActionRuntimeContext;
import com.hortonworks.iotas.layout.runtime.transform.IdentityTransformRuntime;
import com.hortonworks.iotas.layout.runtime.transform.TransformRuntime;
import com.hortonworks.iotas.layout.runtime.transform.TransformRuntimeService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * {@link ActionRuntime} instance for applying the given chain of {@link Transform}s.
 *
 */
public class TransformActionRuntime extends AbstractActionRuntime {
    private String stream;
    private final List<TransformRuntime> transformRuntimes;

    /**
     * Creates a new {@link TransformActionRuntime} with the given {@code action} instance
     *
     * @param action
     */
    public TransformActionRuntime(TransformAction action) {
        this.transformRuntimes = getTransformRuntimes(action.getTransforms());

        if(action.getOutputStreams() != null && !action.getOutputStreams().isEmpty()) {
            this.stream = action.getOutputStreams().iterator().next();
        }
    }

    private List<TransformRuntime> getTransformRuntimes(List<Transform> transforms) {
        if(transforms == null || transforms.isEmpty()) {
            return Collections.<TransformRuntime>singletonList(new IdentityTransformRuntime());
        }
        List<TransformRuntime> transformRuntimes = new ArrayList<>();
        for (Transform transform : transforms) {
            TransformRuntime transformRuntime = TransformRuntimeService.get().get(transform);
            transformRuntimes.add(transformRuntime);
        }

        return transformRuntimes;
    }

    @Override
    public void setActionRuntimeContext(ActionRuntimeContext actionRuntimeContext) {
        if(stream == null) {
            stream = actionRuntimeContext.getRule().getOutputStreamNameForAction(actionRuntimeContext.getAction());
        }
    }

    /**
     * {@inheritDoc}
     * Recursively applies the list of {@link TransformRuntime} (s) associated with this object and
     * returns the {@link Result}
     */
    @Override
    public List<Result> execute(IotasEvent input) {
        return Collections.singletonList(new Result(stream, doTransform(input)));
    }

    /*
     * applies the transformation chain to the input and returns the transformed events
     */
    protected List<IotasEvent> doTransform(IotasEvent input) {
        return doTransform(input, 0);
    }

    /*
     * applies the i th transform and recursively invokes the method to apply
     * the rest of the transformations in the chain.
     */
    private List<IotasEvent> doTransform(IotasEvent inputEvent, int i) {
        if (i >= transformRuntimes.size()) {
            return Collections.singletonList(inputEvent);
        }
        List<IotasEvent> transformed = new ArrayList<>();
        final List<IotasEvent> iotasEvents = transformRuntimes.get(i).execute(inputEvent);
        //todo handle split/join events here.
        // explore approaches to handle these scenarios.
        // add empty event when it returns null or empty collection
        // currently, we can not handle splitting of the partial events in to more partial events.
        // set a constraint that partial event can not be split again. throw an error if it does
        // todo we can solve this when we can have system level join stream from each stage processor to join processor.
        // which will send total no of those events so that join can wait.

        for (IotasEvent event : iotasEvents) {
            transformed.addAll(doTransform(event, i + 1));
        }
        return transformed;
    }

    @Override
    public Set<String> getOutputStreams() {
        return Collections.singleton(stream);
    }

    @Override
    public String toString() {
        return "TransformActionRuntime{" +
                "stream='" + stream + '\'' +
                ", transformRuntimes=" + transformRuntimes +
                '}';
    }


    public static class Factory implements RuntimeService.Factory<ActionRuntime, Action> {

        @Override
        public ActionRuntime create(Action action) {
            return new TransformActionRuntime((TransformAction)action);
        }
    }
}
