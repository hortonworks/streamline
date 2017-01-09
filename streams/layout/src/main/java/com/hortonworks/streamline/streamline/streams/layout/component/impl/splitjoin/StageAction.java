/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.streamline.streams.layout.component.impl.splitjoin;

import org.apache.streamline.streams.layout.Transform;
import org.apache.streamline.streams.layout.component.rule.action.Action;
import org.apache.streamline.streams.layout.component.rule.action.TransformAction;
import org.apache.streamline.streams.layout.component.rule.action.transform.EnrichmentTransform;
import org.apache.streamline.streams.layout.component.rule.action.transform.ProjectionTransform;

import java.util.List;

/**
 * {@link Action} for stage processor which is invoked by split or stage processors.
 *
 */
public class StageAction extends TransformAction {

    public StageAction() {
    }

    public StageAction(List<Transform> transforms) {
        super(transforms);
        validateSupportedTransforms(transforms);
    }

    private StageAction(StageAction other) {
        super(other);
    }

    @Override
    public StageAction copy() {
        return new StageAction(this);
    }

    private void validateSupportedTransforms(List<Transform> transforms) {
        for (Transform transform : transforms) {
            if(!(transform instanceof ProjectionTransform || transform instanceof EnrichmentTransform)) {
                throw new IllegalArgumentException("Given transform is not supported. It should be either ProjectionTransform or EnrichmentTransform");
            }
        }
    }

    @Override
    public String toString() {
        return "StageAction{}"+super.toString();
    }
}
