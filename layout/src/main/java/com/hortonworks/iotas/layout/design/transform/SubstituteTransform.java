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
package com.hortonworks.iotas.layout.design.transform;

import com.hortonworks.iotas.common.IotasEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Expands template variables in the IotasEvent values by looking up the variables in {@link IotasEvent#getFieldsAndValues()}.
 */
public class SubstituteTransform extends Transform {
    private final Set<String> fields = new HashSet<>();

    public SubstituteTransform() {
        this(Collections.<String>emptySet());
    }

    public SubstituteTransform(Set<String> fields) {
        this.fields.addAll(fields);
    }

    public Set<String> getFields() {
        return Collections.unmodifiableSet(fields);
    }

    @Override
    public String toString() {
        return "SubstituteTransform{" +
                "fields=" + fields +
                '}'+super.toString();
    }
}
