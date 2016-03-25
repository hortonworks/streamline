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
 * The base implementation of a {@link Processor} that all Iotas processors should extend.
 */
public class IotasProcessor extends IotasComponent implements Processor {
    private final Set<Stream> outputStreams = new HashSet<>();

    // for serialization
    protected IotasProcessor() {
        this(Collections.EMPTY_SET);
    }

    public IotasProcessor(Set<Stream> outputStreams) {
        this.outputStreams.addAll(outputStreams);
    }

    @Override
    public Set<Stream> getOutputStreams() {
        return outputStreams;
    }

    public void addOutputStream(Stream stream) {
        outputStreams.add(stream);
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
    public String toString() {
        return "IotasProcessor{" +
                "outputStreams=" + outputStreams +
                '}'+super.toString();
    }
}
