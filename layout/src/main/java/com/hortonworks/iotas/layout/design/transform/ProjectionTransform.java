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
package com.hortonworks.iotas.layout.design.transform;

import java.util.Collections;
import java.util.Set;

/**
 * This class can be used to configure projection transform which can be used in any {@link com.hortonworks.iotas.layout.design.rule.action.Action}
 * of a rule based processor. It projects the required fields of a received {@link com.hortonworks.iotas.common.IotasEvent}
 *
 */
public class ProjectionTransform extends Transform {

    private Set<String> projectionFields;

    private ProjectionTransform() {
    }

    /**
     *
     * @param name name of the transform
     * @param projectionFields fields to be projected
     */
    public ProjectionTransform(String name, Set<String> projectionFields) {
        super(name);
        this.projectionFields = projectionFields;
    }

    public Set<String> getProjectionFields() {
        return Collections.unmodifiableSet(projectionFields);
    }

    @Override
    public String toString() {
        return "ProjectionTransform{" +
                "projectionFields=" + projectionFields +
                '}'+super.toString();
    }
}
