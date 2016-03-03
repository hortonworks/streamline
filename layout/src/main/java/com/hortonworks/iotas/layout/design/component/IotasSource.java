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
import java.util.Set;

/**
 * The base implementation of a {@link Source} that all Iotas sources should extend.
 */
public class IotasSource extends IotasComponent implements Source {
    private final Set<Stream> declaredOutputs;

    // for serialization
    protected IotasSource() {
        this(Collections.EMPTY_SET);
    }

    public IotasSource(Set<Stream> declaredOutputs) {
        this.declaredOutputs = declaredOutputs;
    }

    @Override
    public Set<Stream> getDeclaredOutputs() {
        return declaredOutputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IotasSource that = (IotasSource) o;

        return declaredOutputs != null ? declaredOutputs.equals(that.declaredOutputs) : that.declaredOutputs == null;

    }

    @Override
    public Stream getStream(String streamId) {
        for (Stream stream : this.getDeclaredOutputs()) {
            if (stream.getId().equals(streamId)) {
                return stream;
            }
        }
        throw new IllegalArgumentException("Invalid streamId " + streamId);
    }

    @Override
    public int hashCode() {
        return declaredOutputs != null ? declaredOutputs.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "IotasSource{" +
                "declaredOutputs=" + declaredOutputs +
                "} " + super.toString();
    }
}
