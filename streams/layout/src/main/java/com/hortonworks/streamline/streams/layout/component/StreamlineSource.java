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
package com.hortonworks.streamline.streams.layout.component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The base implementation of a {@link Source} that all Streamline sources should extend.
 */
public class StreamlineSource extends StreamlineComponent implements Source {
    private final Set<Stream> outputStreams = new HashSet<>();

    public StreamlineSource() {
        this(Collections.EMPTY_SET);
    }

    public StreamlineSource(Set<Stream> outputStreams) {
        addOutputStreams(outputStreams);
    }

    @Override
    public Set<Stream> getOutputStreams() {
        return Collections.unmodifiableSet(outputStreams);
    }

    public void addOutputStream(Stream stream) {
        outputStreams.add(stream);
    }

    public void addOutputStreams(Set<Stream> streams) {
        outputStreams.addAll(streams);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StreamlineSource that = (StreamlineSource) o;

        return outputStreams != null ? outputStreams.equals(that.outputStreams) : that.outputStreams == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (outputStreams != null ? outputStreams.hashCode() : 0);
        return result;
    }

    @Override
    public Stream getOutputStream(String streamId) {
        for (Stream stream : this.getOutputStreams()) {
            if (stream.getId().equals(streamId)) {
                return stream;
            }
        }
        throw new IllegalArgumentException("Invalid streamId " + streamId);
    }

    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "StreamlineSource{" +
                "outputStreams=" + outputStreams +
                "} " + super.toString();
    }
}
