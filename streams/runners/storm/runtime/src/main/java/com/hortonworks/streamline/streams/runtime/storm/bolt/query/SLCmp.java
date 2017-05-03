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

// Streamline specific join comparators
public class SLCmp {

    public static class Equal extends Cmp.Equal {
        public Equal(String fieldSelector1, String fieldSelector2) {
            super(   SLRealtimeJoinBolt.insertStreamlinePrefix(fieldSelector1)
                    , SLRealtimeJoinBolt.insertStreamlinePrefix(fieldSelector2) );
        }

    }

    public static Equal equal(String fieldSelector1, String fieldSelector2) {
        return new Equal(fieldSelector1, fieldSelector2);
    }


    public static class IgnoreCase extends Cmp.IgnoreCase {
        public IgnoreCase(String fieldSelector1, String fieldSelector2) {
            super(  SLRealtimeJoinBolt.insertStreamlinePrefix(fieldSelector1)
                   , SLRealtimeJoinBolt.insertStreamlinePrefix(fieldSelector2) );
        }
    }

    public static IgnoreCase ignoreCase(String fieldSelector1, String fieldSelector2) {
        return new IgnoreCase(fieldSelector1, fieldSelector2);
    }
}
