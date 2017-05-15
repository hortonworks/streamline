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

public class Cmp {
    static class Equal extends JoinComparator {
        public Equal(String fieldSelector1, String fieldSelector2) {
            super(fieldSelector1, fieldSelector2);
        }

        @Override
        public boolean compare(Tuple t1, Tuple t2) throws InvalidTuple {
            Object f1 = fromField.findField(t1);
            if (f1==null)
                throw new InvalidTuple("Field '" + fromField.canonicalFieldName() + "' not found in tuple", t1 );
            Object f2 = joinField.findField(t2);
            if (f2==null)
                throw new InvalidTuple("Field '" + joinField.canonicalFieldName() + "' not found in tuple", t2 );
            return f1.equals(f2);
        }

    }

    public static Equal equal(String fieldSelector1, String fieldSelector2) {
        return new Equal(fieldSelector1, fieldSelector2);
    }

    // Case-insensitive comparison of two String fields
    static class IgnoreCase extends Equal {
        public IgnoreCase(String fieldSelector1, String fieldSelector2) {
            super(fieldSelector1, fieldSelector2);
        }

        @Override
        public boolean compare(Tuple t1, Tuple t2) throws InvalidTuple {
            Object f1 = fromField.findField(t1);
            if (f1==null)
                throw new InvalidTuple("Field '" + fromField.canonicalFieldName() + "' not found in tuple", t1 );
            Object f2 = joinField.findField(t2);
            if (f2==null)
                throw new InvalidTuple("Field '" + joinField.canonicalFieldName() + "' not found in tuple", t2 );
            return f1.toString().equalsIgnoreCase(f2.toString());
        }
    }

    public static IgnoreCase ignoreCase(String fieldSelector1, String fieldSelector2) {
        return new IgnoreCase(fieldSelector1, fieldSelector2);
    }
}