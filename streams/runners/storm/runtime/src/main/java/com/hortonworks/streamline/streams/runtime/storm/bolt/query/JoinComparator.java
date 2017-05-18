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
    private final String fieldStr1; // 1st arg
    private final String fieldStr2; // 2nd arg

    FieldSelector fromField;
    FieldSelector joinField;

    public JoinComparator(String fieldSelector1, String fieldSelector2) {
        this.fieldStr1 = fieldSelector1;
        this.fieldStr2 = fieldSelector2;
    }

    // Unfortunately we need to do some additional initialization using init() after construction
    // as these args are not available at construction time
    // defaultStream is used when a field selector does not have an explicit stream name
    // defaultStream is same as the fromStream for two stream joins
    public void init(RealtimeJoinBolt.StreamKind streamKind, String defaultStream) {
        FieldSelector f1 = new FieldSelector(fieldStr1, streamKind);
        FieldSelector f2 = new FieldSelector(fieldStr2, streamKind);
        // fill in missing stream name (if any) with default stream name
        if (f1.streamName==null) {
            if (f2.streamName==null) {
                throw new IllegalArgumentException("At least one field selector must explicitly specify streamname: prefix in comparator: "
                        + fieldStr1 + "," + fieldStr2);
            }
            f1.streamName = defaultStream;
        }  else if (f2.streamName==null) {
            f2.streamName = defaultStream;
        }

        if (f1.streamName.equalsIgnoreCase(defaultStream)) {
            fromField = f2;
            joinField = f1;
        } else if (f2.streamName.equalsIgnoreCase(defaultStream)) {
            fromField = f1;
            joinField = f2;
        } else {
            throw new IllegalArgumentException("Verify the stream names used for the field selectors in the comparator: "
                    + fieldStr1 + "," + fieldStr2);
        }

        if (fromField.streamName.equalsIgnoreCase(joinField.streamName))
            throw new IllegalArgumentException("Both field selectors in cannot refer to same stream in a comparator: "
                    + fieldStr1 + "," + fieldStr2);
    }

    public FieldSelector getFromField() {
        return fromField;
    }

    public FieldSelector getJoinField() {
        return joinField;
    }

//     TODO: see how to optimize this lookup as it falls in critical path
//    public FieldSelector getFieldForStream(String stream) {
//        if (fromField.streamName.equalsIgnoreCase(stream))
//            return fromField;
//        else if (joinField.streamName.equalsIgnoreCase(stream))
//            return joinField;
//        return null;
//    }

    public FieldSelector getFieldForFromStream() {
        return fromField;
    }

    public FieldSelector getFieldForJoinStream() {
        return joinField;
    }


    public abstract boolean compare(Tuple t1, Tuple t2) throws InvalidTuple;

}
