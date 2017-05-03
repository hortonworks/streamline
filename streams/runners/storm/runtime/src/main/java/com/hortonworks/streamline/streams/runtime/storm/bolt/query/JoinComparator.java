/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.hortonworks.streamline.streams.runtime.storm.bolt.query;


import org.apache.storm.tuple.Tuple;

abstract class JoinComparator {
    private final String fieldStr1; // from 1st stream
    private final String fieldStr2; // from 2nd stream

    FieldSelector field1;
    FieldSelector field2;

    public JoinComparator(String fieldSelector1, String fieldSelector2) {
        this.fieldStr1 = fieldSelector1;
        this.fieldStr2 = fieldSelector2;
    }

    // Unfortunately we need to do some additional initialization here after construction
    // as these two args are not available at construction time
    public void init(RealtimeJoinBolt.StreamKind streamKind, String defaultStream) {
        this.field1 = new FieldSelector(fieldStr1, streamKind);
        this.field2 = new FieldSelector(fieldStr2, streamKind);
        if (field1.streamName==null)
            field1.streamName = defaultStream;
        if (field2.streamName==null)
            field2.streamName = defaultStream;
        if (field1.streamName.equalsIgnoreCase(field2.streamName))
            throw new IllegalArgumentException("Both field selectors in cannot refer to same stream in a comparator: "
                    + fieldStr1 + "," + fieldStr2);
    }

    public FieldSelector getField1() {
        return field1;
    }

    public FieldSelector getField2() {
        return field2;
    }

    // TODO: see how to optimize this lookup as it falls in critical path
    public FieldSelector getFieldForStream(String stream) {
        if (field1.streamName.equalsIgnoreCase(stream))
            return field1;
        else if (field2.streamName.equalsIgnoreCase(stream))
            return field2;
        return null;
    }

    public abstract boolean compare(Tuple t1, Tuple t2) throws InvalidTuple;

}
