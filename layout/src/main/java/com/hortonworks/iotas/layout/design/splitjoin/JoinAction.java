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
package com.hortonworks.iotas.layout.design.splitjoin;

import com.hortonworks.iotas.layout.design.rule.action.Action;

/**
 * {@link Action} configuration for joining the events split by a {@link SplitProcessor}.
 * When {@code joinerClassName} is not given then {@link com.hortonworks.iotas.layout.runtime.splitjoin.DefaultJoiner} is used.
 */
public class JoinAction extends Action {

    public static final int DEFAULT_EXPIRY_INTERVAL = Integer.MAX_VALUE;

    /**
     * id of a jar resource which contains {@code joinerClassName} and its dependent classes
     */
    private String jarId;

    /**
     * custom {@link com.hortonworks.iotas.layout.runtime.splitjoin.Joiner} class name
     */
    private String joinerClassName;

    /**
     * Expiry interval in milli seconds of a single split group to be stored
     */
    private long groupExpiryInterval = DEFAULT_EXPIRY_INTERVAL;

    /**
     * Expiry interval in milli seconds of a partial event to be stored for a specific split group.
     */
    private long eventExpiryInterval = DEFAULT_EXPIRY_INTERVAL;

    public JoinAction() {
    }

    public JoinAction(String jarId, String joinerClassName, long groupExpiryInterval, long eventExpiryInterval) {
        this.jarId = jarId;
        this.joinerClassName = joinerClassName;
        this.groupExpiryInterval = groupExpiryInterval;
        this.eventExpiryInterval = eventExpiryInterval;
    }

    /**
     * @return id of a jar resource which contains {@code joinerClassName} and its dependent classes
     */
    public String getJarId() {
        return jarId;
    }

    /**
     * @return custom {@link com.hortonworks.iotas.layout.runtime.splitjoin.Joiner} class name
     */
    public String getJoinerClassName() {
        return joinerClassName;
    }

    /**
     * @return Expiry interval in milli seconds of a single split group to be stored
     */
    public long getGroupExpiryInterval() {
        return groupExpiryInterval;
    }

    /**
     * @return Expiry interval in milli seconds of a partial event to be stored for a specific split group.
     */
    public long getEventExpiryInterval() {
        return eventExpiryInterval;
    }

    @Override
    public String toString() {
        return "JoinAction{" +
                "jarId='" + jarId + '\'' +
                ", joinerClassName='" + joinerClassName + '\'' +
                ", groupExpiryInterval=" + groupExpiryInterval +
                ", eventExpiryInterval=" + eventExpiryInterval +
                '}';
    }
}
