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
package com.hortonworks.iotas.layout.design.component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The base implementation of a {@link Source} that all Iotas sources should extend.
 */
public class IotasSource extends IotasComponent implements Source {
    private final Set<Stream> declaredStreams = new HashSet<>();

    // for serialization
    protected IotasSource() {
        this(Collections.EMPTY_SET);
    }

    public IotasSource(Set<Stream> declaredStreams) {
        this.declaredStreams.addAll(declaredStreams);
    }

    @Override
    public Set<Stream> getDeclaredOutputStreams() {
        return Collections.unmodifiableSet(declaredStreams);
    }

    public void addOutputStream(Stream stream) {
        declaredStreams.add(stream);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IotasSource that = (IotasSource) o;

        return declaredStreams != null ? declaredStreams.equals(that.declaredStreams) : that.declaredStreams == null;

    }

    @Override
    public Stream getStream(String streamId) {
        for (Stream stream : this.getDeclaredOutputStreams()) {
            if (stream.getId().equals(streamId)) {
                return stream;
            }
        }
        throw new IllegalArgumentException("Invalid streamId " + streamId);
    }

    @Override
    public int hashCode() {
        return declaredStreams != null ? declaredStreams.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "IotasSource{" +
                "declaredStreams=" + declaredStreams +
                "} " + super.toString();
    }
}
