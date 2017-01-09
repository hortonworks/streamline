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
package com.hortonworks.streamline.streams.runtime.rule.action;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.layout.Transform;
import com.hortonworks.streamline.streams.layout.component.rule.action.Action;
import com.hortonworks.streamline.streams.layout.component.rule.action.NotifierAction;
import com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction;
import com.hortonworks.streamline.streams.layout.component.rule.action.transform.AddHeaderTransform;
import com.hortonworks.streamline.streams.layout.component.rule.action.transform.MergeTransform;
import com.hortonworks.streamline.streams.layout.component.rule.action.transform.ProjectionTransform;
import com.hortonworks.streamline.streams.layout.component.rule.action.transform.SubstituteTransform;
import com.hortonworks.streamline.streams.runtime.RuntimeService;
import com.hortonworks.streamline.streams.runtime.TransformActionRuntime;
import com.hortonworks.streamline.streams.runtime.transform.AddHeaderTransformRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link ActionRuntime} implementation for notifications.
 */
public class NotifierActionRuntime extends AbstractActionRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(NotifierActionRuntime.class);

    private final NotifierAction notifierAction;
    private TransformActionRuntime transformActionRuntime;
    private String outputStream;

    public NotifierActionRuntime(NotifierAction notifierAction) {
        this.notifierAction = notifierAction;
    }

    @Override
    public void setActionRuntimeContext(ActionRuntimeContext actionRuntimeContext) {
        outputStream = notifierAction.getOutputStreams().iterator().next();
        LOG.debug("Notifier action {}, outputStream set to {}", notifierAction, outputStream);
        transformActionRuntime = new TransformActionRuntime(
                new TransformAction(getNotificationTransforms(notifierAction, actionRuntimeContext.getRule().getId()),
                        Collections.singleton(outputStream)));
    }

    @Override
    public List<Result> execute(StreamlineEvent input) {
        return transformActionRuntime.execute(input);
    }

    @Override
    public Set<String> getOutputStreams() {
        return Collections.singleton(outputStream);
    }


    /**
     * Returns the necessary transforms to perform based on the action.
     */
    private List<Transform> getNotificationTransforms(NotifierAction action, Long ruleId) {
        List<Transform> transforms = new ArrayList<>();
        if (action.getOutputFieldsAndDefaults() != null && !action.getOutputFieldsAndDefaults().isEmpty()) {
            transforms.add(new MergeTransform(action.getOutputFieldsAndDefaults()));
            transforms.add(new SubstituteTransform(action.getOutputFieldsAndDefaults().keySet()));
            transforms.add(new ProjectionTransform("projection-" + ruleId, action.getOutputFieldsAndDefaults().keySet()));
        }

        Map<String, Object> headers = new HashMap<>();
        headers.put(AddHeaderTransformRuntime.HEADER_FIELD_NOTIFIER_NAME, action.getNotifierName());
        headers.put(AddHeaderTransformRuntime.HEADER_FIELD_RULE_ID, ruleId);
        transforms.add(new AddHeaderTransform(headers));

        return transforms;
    }

    public static class Factory implements RuntimeService.Factory<ActionRuntime, Action> {
        @Override
        public ActionRuntime create(Action action) {
            return new NotifierActionRuntime((NotifierAction) action);
        }
    }

}
