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
package com.hortonworks.iotas.layout.design.splitjoin;

import com.hortonworks.iotas.layout.design.rule.action.Action;

/**
 * {@link Action} configuration for splitting the events.
 *
 */
public class SplitAction extends Action {
    private Long jarId;
    private String splitterClassName;

    public SplitAction() {
    }

    public SplitAction(Long jarId, String splitterClassName) {
        this.jarId = jarId;
        this.splitterClassName = splitterClassName;
    }

    public Long getJarId() {
        return jarId;
    }

    public String getSplitterClassName() {
        return splitterClassName;
    }

    @Override
    public String toString() {
        return "SplitAction{" +
                "jarId='" + jarId + '\'' +
                ", splitterClassName='" + splitterClassName + '\'' +
                '}' + super.toString();
    }
}
