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

import com.hortonworks.streamline.streams.runtime.storm.bolt.query.RealtimeJoinBolt.StreamKind;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.windowing.TimestampExtractor;

import java.util.HashMap;


public class SLMultistreamTimestampExtractor implements TimestampExtractor {
    HashMap<String,FieldSelector> tsFields = new HashMap<>(); // stream name -> field selector

    /** Field selectors should be qualified with stream name and can refer to nested fields. Aliases if any will be ignored.
     *     Ex: "stream1:tsField"  or  "stream1:outerField.TsField"
     *  'streamline-event.' prefix will be added to the events automatically
     */
    public SLMultistreamTimestampExtractor(String... fieldSelectors) {
        for (int i = 0; i < fieldSelectors.length; i++) {
            String fieldSel = WindowedQueryBolt.convertToStreamLineKeys(fieldSelectors[i]); // Strip any alias & prefix field with 'streamline-event.'
            FieldSelector fs = new FieldSelector(fieldSel, StreamKind.STREAM);
            if ( tsFields.containsKey(fs.streamName) ) {
                throw new IllegalArgumentException("Only one timestamp field allowed per stream");
            }
            tsFields.put(fs.streamName, fs);
        }
    }

    @Override
    public long extractTimestamp(Tuple tuple) {
        FieldSelector tsFieldSel = tsFields.get( tuple.getSourceStreamId() );
        if (tsFieldSel == null) {
            throw new IllegalArgumentException("Unrecognized source stream in Event: '" + tuple.getSourceStreamId() + "'");
        }
        Object tsField = tsFieldSel.findField(tuple);
        if (tsField == null) {
            throw new IllegalArgumentException("Timestamp field not found in event : '" + tsFieldSel.outputName + "'");
        }
        return Long.parseLong(tsField.toString());
    }

}
