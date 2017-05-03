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

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * This class derives from RealtimeBolt to customize the handling of 'streamline-event' prefix
 *
 *
 **** Examples: ****
 *
 *  NOTE:  We use the streamline specific comparators from  SLCmp.* instead of Cmp.*
 *
 *  1) -- Count based retention window ---
 *    new SLRealtimeJoinBolt(RealtimeJoinBolt.StreamKind.STREAM)
 *            .from("purchases", 10, false )
 *            .leftJoin("ads"  , 10, false, SLCmp.ignoreCase("ads:product","purchases:product")
 *                                        , SLCmp.equal("userId", "purchases:userId") )
 *            .select("orders:id , ads:userId, ads:product, orders:product, price")
 *            .withOutputStream("outStreamName");
 *
 *
 *   2) -- Time based retention window ---
 *    new SLRealtimeJoinBolt(RealtimeJoinBolt.StreamKind.STREAM)
 *            .from("purchases", Duration.ofSeconds(10), false )
 *            .leftJoin("ads",   Duration.ofSeconds(20), false, SLCmp.ignoreCase("ads:product","purchases:product")
 *                                                            , SLCmp.equal("userId", "purchases:userId") )
 *            .select("orders:id , ads:userId, ads:product, orders:product, price")
 *            .withOutputStream("outStreamName");
 *
 */
public class SLRealtimeJoinBolt extends RealtimeJoinBolt {
    final static String EVENT_PREFIX = StreamlineEvent.STREAMLINE_EVENT + ".";

    /**
     * Calls  RealtimeJoinBolt(StreamKind.STREAM)
     */
    public SLRealtimeJoinBolt() {
        super(StreamKind.STREAM);
    }

    @Override
    public SLRealtimeJoinBolt withOutputStream(String streamName) {
        return (SLRealtimeJoinBolt) super.withOutputStream(streamName);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(this.outputStream, new Fields(StreamlineEvent.STREAMLINE_EVENT));
    }

    @Override
    public SLRealtimeJoinBolt from(String stream, int retentionCount, boolean unique) {
        return (SLRealtimeJoinBolt) super.from(stream, retentionCount, unique);
    }

    public SLRealtimeJoinBolt from(String stream, BaseWindowedBolt.Duration retentionTime, boolean unique) {
        return (SLRealtimeJoinBolt) super.from(stream, Duration.ofMillis(retentionTime.value), unique);
    }

    @Override
    public SLRealtimeJoinBolt innerJoin(String stream, int retentionCount, boolean unique, JoinComparator... comparators) {
        return (SLRealtimeJoinBolt) super.innerJoin(stream, retentionCount, unique, comparators);
    }

    public SLRealtimeJoinBolt innerJoin(String stream, BaseWindowedBolt.Duration retentionTime, boolean unique, JoinComparator... comparators) {
        return (SLRealtimeJoinBolt) super.innerJoin(stream, Duration.ofMillis(retentionTime.value), unique, comparators);
    }

    @Override
    public SLRealtimeJoinBolt leftJoin(String stream, int retentionCount, boolean unique, JoinComparator... comparators) {
        return (SLRealtimeJoinBolt) super.leftJoin(stream, retentionCount, unique, comparators);
    }

    public SLRealtimeJoinBolt leftJoin(String stream, BaseWindowedBolt.Duration retentionTime, boolean unique, JoinComparator... comparators) {
        return (SLRealtimeJoinBolt) super.leftJoin(stream, Duration.ofMillis(retentionTime.value), unique, comparators);
    }

    @Override
    public SLRealtimeJoinBolt rightJoin(String stream, int retentionCount, boolean unique, JoinComparator... comparators) {
        return (SLRealtimeJoinBolt) super.rightJoin(stream, retentionCount, unique, comparators);
    }

    public SLRealtimeJoinBolt rightJoin(String stream, BaseWindowedBolt.Duration retentionTime, boolean unique, JoinComparator... comparators) {
        return (SLRealtimeJoinBolt) super.rightJoin(stream, Duration.ofMillis(retentionTime.value), unique, comparators);
    }

    @Override
    public SLRealtimeJoinBolt outerJoin(String stream, int retentionCount, boolean unique, JoinComparator... comparators) {
        return (SLRealtimeJoinBolt) super.outerJoin(stream, retentionCount, unique, comparators);
    }

    public SLRealtimeJoinBolt outerJoin(String stream, BaseWindowedBolt.Duration retentionTime, boolean unique, JoinComparator... comparators) {
        return (SLRealtimeJoinBolt) super.outerJoin(stream, Duration.ofMillis(retentionTime.value), unique, comparators);
    }

    /** Convenience method for Streamline that prefixes each keyname with 'streamline-event.'
     *
     * @param commaSeparatedKeys
     * @return
     */
    public SLRealtimeJoinBolt select(String commaSeparatedKeys) {
        String prefixedKeys = convertToStreamLineKeys(commaSeparatedKeys);
        return (SLRealtimeJoinBolt) super.select(prefixedKeys);
    }

    /**
     *  NOTE: Streamline specific convenience method. Creates output tuple as a StreamlineEvent
     * @param tuple1  can be null
     * @param tuple2  can be null
     * @return
     */
    @Override
    protected List<Object> doProjection(Tuple tuple1, Tuple tuple2) {
        StreamlineEventImpl.Builder eventBuilder = StreamlineEventImpl.builder();

        for ( int i = 0; i < outputFields.length; i++ ) {
            FieldSelector outField = outputFields[i];

            Object field = outField.findField(tuple1) ;
            if (field==null)
                field = outField.findField(tuple2);
            String outputKeyName = dropStreamLineEventPrefix(outField.outputName );
            eventBuilder.put(outputKeyName, field); // adds null if field is not found in both tuples
        }

        StreamlineEventImpl slEvent = eventBuilder.dataSourceId("multiple sources").build();
        return Collections.singletonList(slEvent);
    }

    // Prefixes each key with 'streamline-event.' Example:
    //   arg = "stream1:key1, key2, stream2:key3.key4, key5"
    //   result  = "stream1:streamline-event.key1, streamline-event.key2, stream2:streamline-event.key3.key4, streamline-event.key5"
    static String convertToStreamLineKeys(String commaSeparatedKeys) {
        String[] keyNames = commaSeparatedKeys.replaceAll("\\s+","").split(",");

        String[] prefixedKeys = new String[keyNames.length];
        for (int i = 0; i < keyNames.length; i++) {
            prefixedKeys[i] = insertStreamlinePrefix(keyNames[i]);
        }

        return String.join(", ", prefixedKeys);
    }

    public static String insertStreamlinePrefix(String keyName) {
        FieldSelector fs = new FieldSelector(keyName, null); // 2nd arg here is null as we don't care about it. FieldSelector used only for parsing and not calling findField()
        if (fs.streamName==null)
            return   EVENT_PREFIX +  fs.canonicalFieldName();
        else
            return   fs.streamName + ":" + EVENT_PREFIX + fs.canonicalFieldName();
    }

    private static String dropStreamLineEventPrefix(String flattenedKey) {
        int pos = flattenedKey.indexOf(EVENT_PREFIX);
        if(pos==0)
            return flattenedKey.substring(EVENT_PREFIX.length());
        return flattenedKey.substring(0,pos) + flattenedKey.substring(pos+EVENT_PREFIX.length());
    }
}

