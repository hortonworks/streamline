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

import java.util.Map;

/**
 * Produces a new event whose fieldsAndValues is obtained by merging the event's fieldsAndValues with the defaults. The
 * event's fieldsAndValues takes precedence over the defaults.
 */
public class MergeTransform extends Transform {
    private final Map<String, ?> defaults;

    private MergeTransform() {
        this(null);
    }

    public MergeTransform(Map<String, ?> defaults) {
        this.defaults = defaults;
    }

    public Map<String, ?> getDefaults() {
        return defaults;
    }

    @Override
    public String toString() {
        return "MergeTransform{" +
                "defaults=" + defaults +
                '}'+super.toString();
    }
}
