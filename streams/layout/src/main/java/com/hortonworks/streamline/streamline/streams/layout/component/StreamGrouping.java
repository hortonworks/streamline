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

import java.util.List;

public class StreamGrouping {
    private final Stream stream;
    private final Stream.Grouping grouping;
    private final List<String> fields;

    public StreamGrouping(Stream stream, Stream.Grouping grouping) {
        this(stream, grouping, null);
    }

    public StreamGrouping(Stream stream, Stream.Grouping grouping, List<String> fields) {
        this.stream = stream;
        this.grouping = grouping;
        this.fields = fields;
    }

    public Stream.Grouping getGrouping() {
        return grouping;
    }

    public Stream getStream() {
        return stream;
    }

    public List<String> getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StreamGrouping that = (StreamGrouping) o;

        if (stream != null ? !stream.equals(that.stream) : that.stream != null) return false;
        return grouping == that.grouping;

    }

    @Override
    public int hashCode() {
        int result = stream != null ? stream.hashCode() : 0;
        result = 31 * result + (grouping != null ? grouping.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StreamGrouping{" +
                "stream=" + stream +
                ", grouping=" + grouping +
                '}';
    }
}
