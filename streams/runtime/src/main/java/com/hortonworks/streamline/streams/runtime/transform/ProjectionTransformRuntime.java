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
package org.apache.streamline.streams.runtime.transform;

import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.common.StreamlineEventImpl;
import org.apache.streamline.streams.layout.Transform;
import org.apache.streamline.streams.layout.component.rule.action.transform.ProjectionTransform;
import org.apache.streamline.streams.runtime.RuntimeService;
import org.apache.streamline.streams.runtime.TransformRuntime;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Project given fields from the input StreamlineEvent.
 */
public class ProjectionTransformRuntime implements TransformRuntime {
    private final ProjectionTransform projectionTransform;

    /**
     * Selects the fields from the event matching the input fields.
     *
     * @param projectionTransform ProjectionTransform contains the fields to be selected
     */
    public ProjectionTransformRuntime(ProjectionTransform projectionTransform) {
        this.projectionTransform = projectionTransform;
    }

    @Override
    public List<StreamlineEvent> execute(StreamlineEvent input) {
        return doTransform(input);
    }

    private List<StreamlineEvent> doTransform(StreamlineEvent input) {
        Map<String, Object> result = new HashMap<>();
        for (String field : projectionTransform.getProjectionFields()) {
            result.put(field, input.get(field));
        }
        return Collections.<StreamlineEvent>singletonList(new StreamlineEventImpl(result, input.getDataSourceId()));
    }

    @Override
    public String toString() {
        return "ProjectionTransformRuntime{" +
                "projectionTransform=" + projectionTransform +
                '}';
    }


    public static class Factory implements RuntimeService.Factory<TransformRuntime, Transform> {

        @Override
        public TransformRuntime create(Transform transform) {
            return new ProjectionTransformRuntime((ProjectionTransform) transform);
        }
    }
}
