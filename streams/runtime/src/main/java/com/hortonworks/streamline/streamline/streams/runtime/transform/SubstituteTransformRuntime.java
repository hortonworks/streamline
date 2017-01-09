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
import org.apache.streamline.streams.layout.component.rule.action.transform.SubstituteTransform;
import org.apache.streamline.streams.runtime.RuntimeService;
import org.apache.streamline.streams.runtime.TransformRuntime;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Expands template variables in the StreamlineEvent values by looking up the
 * variables in the StreamlineEvent fieldsAndValues.
 */
public class SubstituteTransformRuntime implements TransformRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(SubstituteTransformRuntime.class);
    private final SubstituteTransform substituteTransform;

    /**
     * Does variable substitution for all the fields in the StreamlineEvent
     */
    public SubstituteTransformRuntime() {
        this(new SubstituteTransform());
    }

    /**
     * Does variable substitution for the specified set of fields in the StreamlineEvent
     */
    public SubstituteTransformRuntime(SubstituteTransform substituteTransform) {
        this.substituteTransform = substituteTransform;
    }

    @Override
    public List<StreamlineEvent> execute(StreamlineEvent input) {
        List<StreamlineEvent> result;
        try {
            result = substitute(input);
        } catch (Exception ex) {
            LOG.error("Variable substitution failed", ex);
            LOG.error("Returning the input event as is without replacing the variables");
            result = Collections.singletonList(input);
        }
        return result;
    }

    private List<StreamlineEvent> substitute(StreamlineEvent input) {
        Map<String, Object> substitutedFieldsAndValues = new HashMap<>();
        StrSubstitutor substitutor = new StrSubstitutor(input);
        for(Map.Entry<String, Object> entry: input.entrySet()) {
            if(shouldSubstitue(entry.getKey(), entry.getValue())) {
                substitutedFieldsAndValues.put(entry.getKey(), substitutor.replace(entry.getValue()));
            } else {
                substitutedFieldsAndValues.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.<StreamlineEvent>singletonList(new StreamlineEventImpl(substitutedFieldsAndValues, input.getDataSourceId()));
    }

    private boolean shouldSubstitue(String key, Object value) {
        return value instanceof String
                && (substituteTransform.getFields().isEmpty() || substituteTransform.getFields().contains(key));
    }

    @Override
    public String toString() {
        return "SubstituteTransformRuntime{" +
                "substituteTransform=" + substituteTransform +
                '}';
    }


    public static class Factory implements RuntimeService.Factory<TransformRuntime, Transform> {

        @Override
        public TransformRuntime create(Transform transform) {
            return new SubstituteTransformRuntime((SubstituteTransform) transform);
        }
    }
}
