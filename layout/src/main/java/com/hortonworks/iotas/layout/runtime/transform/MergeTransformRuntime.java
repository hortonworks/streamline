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
package com.hortonworks.iotas.layout.runtime.transform;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.layout.design.transform.MergeTransform;
import com.hortonworks.iotas.layout.design.transform.Transform;
import com.hortonworks.iotas.layout.runtime.RuntimeService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces a new event whose fieldsAndValues is obtained by
 * merging the event's fieldsAndValues with the defaults. The
 * event's fieldsAndValues takes precedence over the defaults.
 */
public class MergeTransformRuntime implements TransformRuntime {

    private final MergeTransform mergeTransform;

    public MergeTransformRuntime(MergeTransform mergeTransform) {
        this.mergeTransform = mergeTransform;
    }

    @Override
    public List<IotasEvent> execute(IotasEvent input) {
        Map<String, Object> merged = new HashMap<>();
        merged.putAll(input.getFieldsAndValues());
        for (Map.Entry<String, ?> entry : mergeTransform.getDefaults().entrySet()) {
            if (!merged.containsKey(entry.getKey())) {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.<IotasEvent>singletonList(new IotasEventImpl(merged, input.getDataSourceId()));
    }

    @Override
    public String toString() {
        return "MergeTransformRuntime{" +
                "mergeTransform=" + mergeTransform +
                '}';
    }


    public static class Factory implements RuntimeService.Factory<TransformRuntime, Transform> {

        @Override
        public TransformRuntime create(Transform transform) {
            return new MergeTransformRuntime((MergeTransform) transform);
        }
    }
}
