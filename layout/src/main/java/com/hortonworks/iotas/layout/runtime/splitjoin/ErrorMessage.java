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
package com.hortonworks.iotas.layout.runtime.splitjoin;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;

import java.util.List;

/**
 * Error message to be sent in error stream from an Action or Stage Processor to a RuleProcessor or Sink.
 */
public class ErrorMessage {
    /**
     * componentId in which this error occurred.
     */
    public final Long componentId;

    /**
     * Encountered error string.
     */
    public final String error;

    /**
     * Received event in an Action or StageProcessor at which this error occurred.
     */
    public final List<IotasEvent> receivedEvents;

    /**
     * Result event to be sent to target stream. This can be null in a case error occurred before computing result event.
     */
    public final Result resultEvent;

    public ErrorMessage(Long componentId, String error, List<IotasEvent> receivedEvents, Result resultEvent) {
        this.componentId = componentId;
        this.error = error;
        this.receivedEvents = receivedEvents;
        this.resultEvent = resultEvent;
    }
}
