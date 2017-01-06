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
package com.hortonworks.streamline.streams.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class TopologyOutputComponent extends TopologyComponent {
    private List<Long> outputStreamIds;
    private List<StreamInfo> outputStreams;

    public TopologyOutputComponent() {
    }

    public TopologyOutputComponent(TopologyOutputComponent other) {
        super(other);
        if (other.getOutputStreamIds() != null) {
            setOutputStreamIds(new ArrayList<>(other.getOutputStreamIds()));
        }
        if (other.getOutputStreams() != null) {
            setOutputStreams(other.getOutputStreams().stream().map(StreamInfo::new).collect(Collectors.toList()));
        }
    }

    @JsonIgnore
    public List<Long> getOutputStreamIds() {
        return outputStreamIds;
    }

    @JsonProperty
    public void setOutputStreamIds(List<Long> outputStreamIds) {
        this.outputStreamIds = outputStreamIds;
    }

    public List<StreamInfo> getOutputStreams() {
        return outputStreams;
    }

    public void setOutputStreams(List<StreamInfo> outputStreams) {
        this.outputStreams = outputStreams;
    }
}
