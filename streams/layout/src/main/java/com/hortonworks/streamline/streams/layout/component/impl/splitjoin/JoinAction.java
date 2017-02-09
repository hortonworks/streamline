/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/

package com.hortonworks.streamline.streams.layout.component.impl.splitjoin;


import com.hortonworks.streamline.streams.layout.component.rule.action.Action;

/**
 * {@link Action} configuration for joining the events split by a {@link SplitProcessor}.
 * When {@code joinerClassName} is not given then {@link com.hortonworks.streamline.layout.runtime.splitjoin.DefaultJoiner} is used.
 */
public class JoinAction extends Action {

    public static final long DEFAULT_EXPIRY_INTERVAL = Long.MAX_VALUE;

    /**
     * id of a jar resource which contains {@code joinerClassName} and its dependent classes
     */
    private Long jarId;

    /**
     * custom {@link com.hortonworks.streamline.layout.runtime.splitjoin.Joiner} class name
     */
    private String joinerClassName;

    /**
     * Expiry interval in milli seconds of a single split group to be stored
     */
    private Long groupExpiryInterval = DEFAULT_EXPIRY_INTERVAL;

    /**
     * Expiry interval in milli seconds of a partial event to be stored for a specific split group.
     */
    private Long eventExpiryInterval = DEFAULT_EXPIRY_INTERVAL;

    public JoinAction() {
    }

    public JoinAction(String joinerClassName) {
        this(null, joinerClassName);
    }

    private JoinAction(JoinAction other) {
        super(other);
        jarId = other.jarId;
        joinerClassName = other.joinerClassName;
        groupExpiryInterval = other.groupExpiryInterval;
        eventExpiryInterval = other.eventExpiryInterval;
    }

    @Override
    public JoinAction copy() {
        return new JoinAction(this);
    }

    public JoinAction(Long jarId, String joinerClassName) {
        this.jarId = jarId;
        this.joinerClassName = joinerClassName;
    }

    public JoinAction(Long jarId, String joinerClassName, Long groupExpiryInterval, Long eventExpiryInterval) {
        this.jarId = jarId;
        this.joinerClassName = joinerClassName;

        if(groupExpiryInterval == null) {
            throw new IllegalArgumentException("groupExpiryInterval can not be null");
        }
        if(eventExpiryInterval == null) {
            throw new IllegalArgumentException("eventExpiryInterval can not be null");
        }

        this.groupExpiryInterval = groupExpiryInterval;
        this.eventExpiryInterval = eventExpiryInterval;
    }

    /**
     * @return id of a jar resource which contains {@code joinerClassName} and its dependent classes
     */
    public Long getJarId() {
        return jarId;
    }

    /**
     * @return custom {@link com.hortonworks.streamline.layout.runtime.splitjoin.Joiner} class name
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
                '}' + super.toString();
    }
}
