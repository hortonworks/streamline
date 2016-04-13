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

package com.hortonworks.iotas.layout.design.rule.action;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link Action} to send notifications.
 *
 */
public class NotifierAction extends Action {
    private String notifierName = "dummy";
    protected Map<String, Object> outputFieldsAndDefaults = new HashMap<>();

    public NotifierAction() { }

    /**
     * The name of the output fields and the default values for them
     * to be emitted as a part of this action.
     */
    public void setOutputFieldsAndDefaults(Map<String, Object> outputFieldsAndDefaults) {
        this.outputFieldsAndDefaults.clear();
        this.outputFieldsAndDefaults.putAll(outputFieldsAndDefaults);
    }

    public Map<String, Object> getOutputFieldsAndDefaults() {
        return Collections.unmodifiableMap(outputFieldsAndDefaults);
    }

    public String getNotifierName() {
        return notifierName;
    }

    public void setNotifierName(String notifierName) {
        this.notifierName = notifierName;
    }

    @Override
    public String toString() {
        return "NotifierAction{" +
                "notifierName='" + notifierName + '\'' +
                ", outputFieldsAndDefaults=" + outputFieldsAndDefaults +
                '}'+super.toString();
    }
}
