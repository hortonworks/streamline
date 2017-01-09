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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streamline.streams.layout.component.rule.action;


import org.apache.streamline.streams.layout.Transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link Action} to apply the given transformations and send them to the given output streams.
 *
 */
public class TransformAction extends Action {

    protected final List<Transform> transforms = new ArrayList<>();

    public TransformAction() { }

    public TransformAction(List<Transform> transforms) {
        this(transforms, Collections.<String>emptyList());
    }

    protected TransformAction(TransformAction other) {
        super(other);
        transforms.addAll(other.transforms.stream().map(Transform::new).collect(Collectors.toList()));
    }

    public TransformAction(List<Transform> transforms, Collection<String> outputStreams) {
        if(transforms == null) {
            throw new IllegalArgumentException("transforms can not be null");
        }
        this.transforms.addAll(transforms);

        if(outputStreams == null) {
            throw new IllegalArgumentException("outputStreams can not be null");
        }
        this.outputStreams.addAll(outputStreams);
    }

    public List<Transform> getTransforms() {
        return transforms;
    }

    @Override
    public TransformAction copy() {
        return new TransformAction(this);
    }

    @Override
    public String toString() {
        return "TransformAction{" +
                "transforms=" + transforms +
                '}'+super.toString();
    }
}
