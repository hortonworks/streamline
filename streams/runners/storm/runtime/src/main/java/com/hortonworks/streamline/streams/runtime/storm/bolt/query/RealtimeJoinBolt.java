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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.LinkedListMultimap;
import org.apache.storm.Config;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.utils.TupleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.time.Duration;


enum JoinType {INNER, LEFT, RIGHT, OUTER}

/**
 * Note: This class will get moved to Storm. This does not have any streamline specific code.
 *       The Streamline specific customizations are in the derived class (SLRealtimeJoinBolt)
 *
 * Provides ability to join two streams. Features:
 *    - Inner/left/right/outer joins are supported.
 *    - Supports multikey joins
 *    - Custom join comparators can be provided
 *
 ****  Examples: ****
 *
 *  1) -- Count based retention window. Join based on Stream ID. ---
 *
 *    new RealtimeJoinBolt(StreamKind.STREAM)
 *            .from("purchases", 10, false )
 *            .leftJoin("ads"  , 10, false, Cmp.ignoreCase("ads:product","purchases:product")
 *                                        , Cmp.equal("userId", "purchases:userId") )
 *            .select("orders:id, ads:userId as userid, ads:product as prod, price")
 *            .withOutputStream("outStreamName");
 *
 *
 *   2) -- Time based retention window. Join based on Source Component ID. ---
 *
 *    new RealtimeJoinBolt(StreamKind.COMPONENT)
 *            .from("purchases", Duration.ofSeconds(10), false )
 *            .leftJoin("ads",   Duration.ofSeconds(20), false, Cmp.ignoreCase("ads:product","purchases:product")
 *                                                            , Cmp.equal("userId", "purchases:userId") )
 *            .select("orders:id, ads:userId as userid, ads:product as prod, price")
 *            .withOutputStream("outStreamName");
 *
 */
public class RealtimeJoinBolt extends BaseRichBolt  {
    private static final Logger LOG = LoggerFactory.getLogger(RealtimeJoinBolt.class);

    public static final int NUM_STREAMS = 2; // currently we only support two stream joins
    JoinInfo[] joinInfos = new JoinInfo[NUM_STREAMS]; // 0=> from stream, 1=> joined stream

    protected FieldSelector[] outputFields = null;   // specified via bolt.select() ... used in declaring Output fields
    protected String outputStream;    // output stream name

    private OutputCollector collector;

    private String fromStream = null;  // first stream
    private String joinStream = null;  // second stream being joined to first

    public enum StreamKind {
        STREAM(0), SOURCE(1);
        int value;

        StreamKind(int value) {
            this.value = value;
        }

        // Returns either the source component Id or the stream Id for the tuple
        String getStreamId(Tuple ti) {
            switch (value) {
                case 0:
                    return ti.getSourceStreamId();
                case 1:
                    return ti.getSourceComponent();
                default:
                    throw new RuntimeException(value + " is unexpected");
            }
        }

    }

    // Indicates if we are using streamId or source component name to distinguish incoming tuple streams
    protected final StreamKind streamKind;


    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        String[] outputFieldNames = new String[outputFields.length];
        for( int i=0; i<outputFields.length; ++i ) {
            outputFieldNames[i] = outputFields[i].outputName ;
        }
        if (outputStream !=null) {
            declarer.declareStream(outputStream, new Fields(outputFieldNames));
        } else {
            declarer.declare(new Fields(outputFieldNames));
        }
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        // We setup tick tuples to expire tuples when one of the streams has time based retention
        // to ensure expiration occurs even if there is no incoming data
        Config conf = new Config();
        if (needTicks())
            conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, 1);
        return conf;
    }

    private boolean needTicks() {
        long fromStreamRetentionMs = joinInfos[0].retentionTime==null ? 0 : joinInfos[0].retentionTime;
        long joinStreamRetentionMs = joinInfos[1].retentionTime==null ? 0 : joinInfos[1].retentionTime;
        return fromStreamRetentionMs>0  || joinStreamRetentionMs>0;
    }

    /**
     * Constructor
     * @param streamKind   Specifies whether we should use stream id or source component id as stream name.
     */
    public RealtimeJoinBolt(StreamKind streamKind) {
        this.streamKind = streamKind;
    }


    public RealtimeJoinBolt from(String stream, int retentionCount, boolean unique) {
        if (fromStream!=null)
            throw new IllegalArgumentException("from() method can be called only once.");
        fromStream = stream;
        this.joinInfos[0] = new JoinInfo(null, null, retentionCount, unique);
        return this;
    }

    public RealtimeJoinBolt from(String stream, Duration retentionTime, boolean unique) {
        if (fromStream!=null)
            throw new IllegalArgumentException("from() method can be called only once.");
        fromStream = stream;
        this.joinInfos[0] = new JoinInfo(null, retentionTime.toMillis(), null, unique);
        return this;
    }

    // INNER JOINS
    public RealtimeJoinBolt innerJoin(String stream, int retentionCount, boolean unique, JoinComparator... comparators) {
        return joinHelperCountRetention(JoinType.INNER, stream, retentionCount, unique, comparators);
    }

    public RealtimeJoinBolt innerJoin(String stream, Duration retentionTime, boolean unique, JoinComparator... comparators) {
        return joinHelperTimeRetention(JoinType.INNER, stream, retentionTime, unique, comparators);
    }


    // LEFT JOINS
    public RealtimeJoinBolt leftJoin(String stream, int retentionCount, boolean unique, JoinComparator... comparators) {
        return joinHelperCountRetention(JoinType.LEFT, stream, retentionCount, unique, comparators);
    }

    public RealtimeJoinBolt leftJoin(String stream, Duration retentionTime, boolean unique, JoinComparator... comparators) {
        return joinHelperTimeRetention(JoinType.LEFT, stream, retentionTime, unique, comparators);
    }


    // RIGHT JOINS
    public RealtimeJoinBolt rightJoin(String stream, int retentionCount, boolean unique, JoinComparator... comparators) {
        return joinHelperCountRetention(JoinType.RIGHT, stream, retentionCount, unique, comparators);
    }

    public RealtimeJoinBolt rightJoin(String stream, Duration retentionTime, boolean unique, JoinComparator... comparators) {
        return joinHelperTimeRetention(JoinType.RIGHT, stream, retentionTime, unique, comparators);
    }


    // OUTER JOINS
    public RealtimeJoinBolt outerJoin(String stream, int retentionCount, boolean unique, JoinComparator... comparators) {
        return joinHelperCountRetention(JoinType.OUTER, stream, retentionCount, unique, comparators);
    }

    public RealtimeJoinBolt outerJoin(String stream, Duration retentionTime, boolean unique, JoinComparator... comparators) {
        return joinHelperTimeRetention(JoinType.OUTER, stream, retentionTime, unique, comparators);
    }


    private RealtimeJoinBolt joinHelperCountRetention(JoinType joinType, String stream, int retentionCount, boolean unique, JoinComparator[] comparators) {
        if (fromStream==null)
            throw  new IllegalArgumentException("Need to call from() before calling any of the *join() methods.");

        // 1- Check stream names and make explicit any implicit stream names
        validateAndSetupStreamNames(comparators, stream);
        // 2- Check and set up the field names
        if (joinStream!=null)
            throw  new IllegalArgumentException("Join support limited to two streams currently.");
        if (retentionCount<=0)
            throw  new IllegalArgumentException("Retention count must be positive number");

        joinStream = stream;
        this.joinInfos[1] = new JoinInfo(joinType, null, retentionCount, unique, comparators);

        if (joinType==JoinType.LEFT || joinType==JoinType.OUTER)
            joinInfos[0].emitUnmatchedTuples = true;
        if (joinType==JoinType.RIGHT || joinType==JoinType.OUTER)
            joinInfos[1].emitUnmatchedTuples = true;
        return this;
    }

    private RealtimeJoinBolt joinHelperTimeRetention(JoinType joinType, String stream, Duration retentionTime,
                                                     boolean unique, JoinComparator[] comparators) {
        if (fromStream==null)
            throw  new IllegalArgumentException("Need to call from() before calling any of the *join() methods.");

        // 1- Check stream names and make explicit any implicit stream names
        validateAndSetupStreamNames(comparators, stream);

        // 2- Check and set up the field names
        if (joinStream!=null)
            throw  new IllegalArgumentException("Join support limited to two streams currently.");
        if (retentionTime.toMillis()<=0)
            throw  new IllegalArgumentException("Retention count must be positive number");

        joinStream = stream;
        this.joinInfos[1] = new JoinInfo(joinType, retentionTime.toMillis(), null, unique, comparators);

        if (joinType==JoinType.LEFT || joinType==JoinType.OUTER)
            joinInfos[0].emitUnmatchedTuples = true;
        if (joinType==JoinType.RIGHT || joinType==JoinType.OUTER)
            joinInfos[1].emitUnmatchedTuples = true;
        return this;
    }

    private void validateAndSetupStreamNames(JoinComparator[] comparators, String currentStream) {
        for (JoinComparator cmp : comparators) {
            cmp.init(streamKind, currentStream);
            FieldSelector f1 = cmp.getFromField();
            FieldSelector f2 = cmp.getJoinField();

            String s1 = f1.streamName;
            String s2 = f2.streamName;
            if (s1==null && s2==null)
                throw  new IllegalArgumentException("Either one or both field selectors must have an explicit stream qualifier in a join condition: '"
                        + f1.canonicalFieldName() + "' & '" + f2.canonicalFieldName() + "'");
            if (s1!=null && s2!=null && s1.equalsIgnoreCase(s2))
                throw  new IllegalArgumentException("Both field selectors must cannot have same stream prefix: '" + f1.outputName + "' & '" + f2.outputName + "'");


            if ( f1.streamName == null) {
                f1.streamName = currentStream; // make it explicit
            } else  if ( f2.streamName == null) {
                f2.streamName = currentStream;
            }
        }
    }


    /**
     * Specify output fields
     *      e.g: .select("lookupField, stream2:dataField, field3")
     * Nested Key names are supported for nested types:
     *      e.g: .select("outerKey1.innerKey1, outerKey1.innerKey2, stream3:outerKey2.innerKey3)"
     * Inner types (non leaf) must be Map<> in order to support nested lookup using this dot notation
     * The selected fields implicitly declare the output fieldNames for the bolt based.
     * @param commaSeparatedKeys
     * @return
     */
    public RealtimeJoinBolt select(String commaSeparatedKeys) {
        String[] fieldNames = commaSeparatedKeys.split(",");

        outputFields = new FieldSelector[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            outputFields[i] = new FieldSelector(fieldNames[i], streamKind);
        }
        return this;
    }

    public RealtimeJoinBolt withOutputStream(String streamName) {
        this.outputStream = streamName;
        return this;
    }

    @VisibleForTesting
    public String[] getOutputFields() {
        String[] result = new String[outputFields.length];
        for (int i = 0; i < outputFields.length; i++) {
            result[i] = outputFields[i].outputName;
        }
        return result;
    }

    @Override
    public void execute(Tuple tuple) {
        long currTime = System.currentTimeMillis();
        for (JoinInfo joinInfo : joinInfos) {
            joinInfo.expireAndAckTimedOutEntries(collector, currTime);
        }

        if (TupleUtils.isTick(tuple))
            return;

        try {
            String stream = streamKind.getStreamId(tuple);
            if (stream.equalsIgnoreCase(fromStream))
                processFromStreamTuple(tuple, currTime);
            else if(stream.equalsIgnoreCase(joinStream))
                processJoinStreamTuple(tuple, currTime);
            else
                throw new InvalidTuple("Source component/streamId for Tuple not part of streams being joined : " + stream, tuple);
        } catch (InvalidTuple e) {
            collector.ack(tuple);
            LOG.warn("{}. Tuple will be dropped.",  e.toString());
        }
    }


    private void processFromStreamTuple(Tuple tuple, long currTime) throws InvalidTuple {
        String key = getFromStreamKey(tuple);

        // 1- Remove older duplicate if 'unique' flag was set
        TupleInfo duplicate = null;
        if (joinInfos[0].unique) {
            duplicate = joinInfos[0].remove(key);
        }

        // 2- Match tuple against other stream (unless its a duplicate) and emit results if any
        boolean matchFound = false;
        if(duplicate==null) {
            List<TupleInfo> matches = joinInfos[1].findMatches(key); // match with joinStream
            if (matches != null && !matches.isEmpty()) {  // match found
                for (TupleInfo tupleInfo : matches) {
                    tupleInfo.matched = true;
                    List<Object> outputTuple = doProjection(tupleInfo.tuple, tuple);
                    emit(outputTuple, tuple, tupleInfo.tuple);
                }
                matchFound = true;
            }
        } else {
            matchFound = duplicate.matched; // clone this setting from the older entry
        }

        // 3- Add to retention buffer
        Tuple expired = joinInfos[0].addTuple(key, tuple, matchFound, currTime); // emits unmatched expiring tuples depending on join type

        // 4- ACK any expired tuples
        if (expired!=null)
            collector.ack(expired);
        if (duplicate!=null)
            collector.ack(duplicate.tuple);

    }

    private void processJoinStreamTuple(Tuple tuple, long currTime) throws InvalidTuple {
        String key = getJoinStreamKey(tuple);

        // 1- Remove older duplicate if 'unique' flag was set
        TupleInfo duplicate = null;
        if (joinInfos[1].unique) {
            duplicate = joinInfos[1].remove(key);
        }

        // 2- Match tuple against other stream (unless its a duplicate) and emit results if any
        boolean matchFound = false;
        if(duplicate==null) {
            List<TupleInfo> matches = joinInfos[0].findMatches(key); // match with fromStream
            if (matches != null && !matches.isEmpty()) {  // match found
                for (TupleInfo tupleInfo : matches) {
                    tupleInfo.matched = true;
                    List<Object> outputTuple = doProjection(tupleInfo.tuple, tuple);
                    emit(outputTuple, tuple, tupleInfo.tuple);
                }
                matchFound = true;
            }
        } else {
            matchFound = duplicate.matched; // clone this setting from the older entry
        }

        // 3- Add to retention buffer
        Tuple expired = joinInfos[1].addTuple(key, tuple, matchFound, currTime); // emits unmatched expiring tuples depending on join type

        // 4- ACK any expired tuples
        if (expired!=null)
            collector.ack(expired);
        if (duplicate!=null)
            collector.ack(duplicate.tuple);
    }


    /**
     *  Get the composite key for the tuple from the From Stream
     * @param tuple
     * @return
     * @throws InvalidTuple
     */
    private String getFromStreamKey(Tuple tuple) throws InvalidTuple {
        StringBuilder key = new StringBuilder();
        for (JoinComparator cmp : joinInfos[1].comparators) { // info always comes from the join stream as from stream doesnt have
            FieldSelector field = cmp.getFieldForFromStream();
            Object partialKey = field.findField(tuple);
            if (partialKey==null)
                throw new InvalidTuple("'" + field + "' field is missing in the tuple", tuple);
            key.append( partialKey.toString() );
            key.append(".");
        }
        return key.toString();
    }


    /**
     *  Get the composite key for the tuple from the Joined Stream
     * @param tuple
     * @return
     * @throws InvalidTuple
     */
    private String getJoinStreamKey(Tuple tuple) throws InvalidTuple {
        StringBuilder key = new StringBuilder();
        for (JoinComparator cmp : joinInfos[1].comparators) { // info always comes from the join stream as from stream doesnt have
            FieldSelector field = cmp.getFieldForJoinStream();
            Object partialKey = field.findField(tuple);
            if (partialKey==null)
                throw new InvalidTuple("'" + field + "' field is missing in the tuple", tuple);
            key.append( partialKey.toString() );
            key.append(".");
        }
        return key.toString();
    }

    private void emit(List<Object> outputTuple, Tuple anchor) {
        if ( outputStream ==null )
            collector.emit(anchor, outputTuple);
        else
            collector.emit(outputStream, anchor, outputTuple);
    }

    private void emit(List<Object> outputTuple, Tuple dataTupleAnchor, Tuple lookupTupleAnchor) {
        List<Tuple> anchors = Arrays.asList(dataTupleAnchor, lookupTupleAnchor);
        if ( outputStream ==null )
            collector.emit(anchors, outputTuple);
        else
            collector.emit(outputStream, anchors, outputTuple);
    }

    private void emitIfUnMatchedTuple(TupleInfo expired) {
        if(!expired.matched) {
            List<Object> outputTuple = doProjection(expired.tuple, null);
            emit(outputTuple, expired.tuple);
        }
    }

    /** Performs projection on the tuples based on 'projectionFields'
     * @param tuple1   can be null
     * @param tuple2   can be null
     * @return   project fields
     */
    protected List<Object> doProjection(Tuple tuple1, Tuple tuple2) {
        ArrayList<Object> result = new ArrayList<>(outputFields.length);
        for ( int i = 0; i < outputFields.length; i++ ) {
            FieldSelector outField = outputFields[i];
            Object field = outField.findField(tuple1) ;
            if (field==null)
                field = outField.findField(tuple2);
            result.add(field); // adds null if field is not found in both tuples
        }
        return result;
    }

    class JoinInfo implements Serializable {
        final static long serialVersionUID = 1L;

        final JoinType joinType;              // null for first stream defined via from()
        final Long retentionTime;             // in millis. can be null.
        final Integer retentionCount;         // can be null
        final Boolean unique;
        final JoinComparator[] comparators;   // null for first stream defined via from()
        boolean emitUnmatchedTuples = false;

        final LinkedListMultimap<String, TupleInfo> buffer;   // retention window. A [key->tuple] map.

        public JoinInfo(JoinType joinType, Long retentionTimeMs, Integer retentionCount, Boolean unique, JoinComparator... comparators) {
            if (retentionCount!=null && retentionTimeMs!=null)
                throw new IllegalArgumentException("Either retentionTimeMs or retentionCount must be null");
            this.joinType = joinType;
            this.retentionTime = retentionTimeMs;
            this.retentionCount = retentionCount;
            this.unique = unique;
            this.comparators = comparators;
            int estimateWindowSz = retentionCount != null ? retentionCount : 100_000;
//            this.timeTracker = (retentionTimeMs!=null) ?  new LinkedHashMap<String, Long>( estimateWindowSz ) : null;
            this.buffer = LinkedListMultimap.create( estimateWindowSz );
        }

        // returns null if no match
        List<TupleInfo> findMatches(String tupleKey) throws InvalidTuple {
            return buffer.get(tupleKey);
        }

        // Removes timedout entries from lookupBuffer & timeTracker. ACKs tuples being expired.
        public void expireAndAckTimedOutEntries(OutputCollector collector, long currTime) {
            if(buffer.isEmpty() || retentionTime==null)
                return;
            long expirationTime = currTime - retentionTime;

            while ( !buffer.isEmpty() ) {
                Map.Entry<String, TupleInfo> oldest = buffer.entries().get(0);
                if ( expirationTime < oldest.getValue().insertionTime )
                    break;

                TupleInfo expired = buffer.entries().remove(0).getValue();
                if (emitUnmatchedTuples)
                    emitIfUnMatchedTuple(expired);
                collector.ack(expired.tuple);
            }
        }

        // Adds a new tuple into buffer, and removes the oldest tuple if size limit is reached (for count based retention case) or null
        // returns an expiring tuple (if any) or null
        public  Tuple addTuple(String key, Tuple tuple, boolean matched, long insertionTime) {
            buffer.put(key, new TupleInfo(tuple, matched, insertionTime) );

            if (retentionCount!=null && buffer.size() > retentionCount) {
                TupleInfo expired = expireOldest();
                if (emitUnmatchedTuples)
                    emitIfUnMatchedTuple(expired);
                return expired.tuple;
            }
            return null;
        }

        private TupleInfo expireOldest() {
            return buffer.entries().remove(0).getValue();
        }

        // remove the entry (if exsits) with this key and returns the removed entry or null
        public TupleInfo remove(String key) {
            List<TupleInfo> expired = buffer.removeAll(key);
            if (expired==null || expired.isEmpty())
                return null;
            return expired.get(0); // 'expired' will have only one element due to dedup
        }
    } // class JoinInfo
}


class TupleInfo {
    Tuple tuple;
    boolean matched = false;
    long insertionTime;

    public TupleInfo(Tuple tuple, boolean matched, long insertionTime) {
        this.tuple = tuple;
        this.matched = matched;
        this.insertionTime =insertionTime;
    }
}