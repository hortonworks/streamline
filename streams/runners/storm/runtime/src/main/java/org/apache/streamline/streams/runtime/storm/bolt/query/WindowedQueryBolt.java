/**
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
package org.apache.streamline.streams.runtime.storm.bolt.query;

// todo: flux enablement
//     : test on streamline
// todo: add stream:keyName support in select

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.windowing.TupleWindow;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.common.StreamlineEventImpl;
import org.apache.streamline.streams.layout.component.rule.expression.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class WindowedQueryBolt extends BaseWindowedBolt {

    private static final Logger LOG = LoggerFactory.getLogger(WindowedQueryBolt.class);
    private OutputCollector collector;

    ArrayList<String> streamJoinOrder = new ArrayList<>(); // order in which to join the streams

    // Map[StreamName -> Map[Key -> List<Tuple>]  ]
    HashMap<String, HashMap<Object, ArrayList<TupleImpl> >> hashedInputs = new HashMap<>(); // holds remaining streams


    // Map[StreamName -> JoinInfo]
    HashMap<String, JoinInfo> joinCriteria = new HashMap<>();
    private String[][] outputKeys;  // specified via bolt.select() ... used in declaring Output fields
    private String[] dotSeparatedOutputKeyNames; // used for naming output fields
    private boolean streamLineStyleProjection = false;

    // Use streamId, source component name OR field in tuple to distinguish incoming tuple streams
    public enum  StreamSelector { STREAM, SOURCE }
    private final StreamSelector streamSelectorType;



    /**
     * StreamId to start the join with. Equivalent SQL ...
     *       select .... from streamId ...
     * @param type Specifies whether 'streamId' refers to stream name/source component
     * @param streamId name of stream/source component
     * @param key the fieldName to use as key for the stream (used for performing joins)
     */
    public WindowedQueryBolt(StreamSelector type, String streamId, String key) {
        // todo: support other types of stream selectors
        if (type!=StreamSelector.STREAM && type!=StreamSelector.SOURCE)
            throw new IllegalArgumentException(type.name());
        streamSelectorType = type;
        streamJoinOrder.add(streamId);
        joinCriteria.put(streamId, new JoinInfo(key) );
    }

    /**
     * Performs inner Join.
     *  SQL    :   from priorStream inner join stream on stream.key = priorStream.key1
     *  same as:   new WindowedQueryBolt(priorStream,key1). join(stream, key, priorStream);
     *
     *  Note: priorStream must be previously joined.
     *    Valid ex:    new WindowedQueryBolt(s1,k1). join(s2,k2, s1). join(s3,k3, s2);
     *    Invalid ex:  new WindowedQueryBolt(s1,k1). join(s3,k3, s2). join(s2,k2, s1);
     */
    public WindowedQueryBolt join(String stream, String key, String priorStream) {
        hashedInputs.put(stream, new HashMap<Object, ArrayList<TupleImpl>>());
        JoinInfo joinInfo = joinCriteria.get(priorStream);
        if( joinInfo==null )
            throw new IllegalArgumentException("Stream '" + priorStream + "' was not previously declared");
        joinCriteria.put(stream, new JoinInfo(key, priorStream, joinInfo, JoinType.INNER) );
        streamJoinOrder.add(stream);
        return this;
    }


    /**
     * Performs left Join.
     *  SQL    :   from stream1  left join stream2  on stream2.key = stream1.key1
     *  same as:   new  WindowedQueryBolt(stream1, key1). leftJoin(stream2, key, stream1);
     *
     *  Note: priorStream must be previously joined
     *    Valid ex:    new WindowedQueryBolt(s1,k1). leftJoin(s2,k2, s1). leftJoin(s3,k3, s2);
     *    Invalid ex:  new WindowedQueryBolt(s1,k1). leftJoin(s3,k3, s2). leftJoin(s2,k2, s1);
     */
    public WindowedQueryBolt leftJoin(String stream, String key, String priorStream) {
        hashedInputs.put(stream, new HashMap<Object, ArrayList<TupleImpl>>());
        JoinInfo joinInfo = joinCriteria.get(priorStream);
        if ( joinInfo==null )
            throw new IllegalArgumentException("Stream '" + priorStream + "' was not previously declared");
        joinCriteria.put(stream, new JoinInfo(key, priorStream, joinInfo, JoinType.LEFT));
        streamJoinOrder.add(stream);
        return this;
    }


    /**
     * Performs projection. i.e. Specifies the keys to include in the output.
     *      e.g: .select("key1, key2, key3")
     * Nested Key names are supported for nested types:
     *      e.g: .select("outerKey1.innerKey1, outerKey1.innerKey2, outerKey2.innerKey3)"
     * Inner types (non leaf) must be Map<> in order to support lookup by keyname
     * This selected keys implicitly declare the output fieldNames for the bolt based.
     * @param commaSeparatedKeys
     * @return
     */
    public WindowedQueryBolt select(String commaSeparatedKeys) {
        String[] keyNames = commaSeparatedKeys.split(",");
        dotSeparatedOutputKeyNames = new String[keyNames.length];
        outputKeys = new String[keyNames.length][];
        for (int i = 0; i < keyNames.length; i++) {
            dotSeparatedOutputKeyNames[i] = keyNames[i].trim();
            outputKeys[i] = dotSeparatedOutputKeyNames[i].split("\\.");
        }
        return this;
    }

    /** Similar to select(), but has two differences:
     *    - each key in 'commaSeparatedKeys' is automatically prefixed with 'streamline-event.'
     *    - the projected tuple is a StreamlineEvent object instead of regular Storm tuple
     *  Note: This will be kept Streamline specific and wont be migrated to Storm.
     */
    public WindowedQueryBolt selectStreamLine(String commaSeparatedKeys) {
        streamLineStyleProjection = true;
        //prefix each key with "streamline-event."
        String prefixedKeyNames = convertToStreamLineKeys(commaSeparatedKeys);
        return select(prefixedKeyNames);
    }
    // prefixes each key with 'streamlin-event.'
    private static String convertToStreamLineKeys(String commaSeparatedKeys) {
        String[] keyNames = commaSeparatedKeys.replaceAll("\\s+","").split(",");
        String prefix = StreamlineEvent.STREAMLINE_EVENT + ".";
        return prefix + String.join("," + prefix, keyNames);
    }

    /** Supports configuring windowing related settings via Streamline GUI.
     *  Note: This will be kept Streamline specific and wont be migrated to Storm.
     * */
    public void withWindowConfig(Window windowConfig) throws IOException {
        if (windowConfig.getWindowLength() instanceof Window.Duration) {
            Duration windowLength = new Duration(((Window.Duration) windowConfig.getWindowLength()).getDurationMs(), TimeUnit.MILLISECONDS);
            if (windowConfig.getSlidingInterval() instanceof Window.Duration) {
                Duration slidingInterval = new Duration(((Window.Duration) windowConfig.getSlidingInterval()).getDurationMs(), TimeUnit.MILLISECONDS);
                withWindow(windowLength, slidingInterval);
            } else if (windowConfig.getSlidingInterval() instanceof Window.Count) {
                Count slidingInterval = new Count(((Window.Count) windowConfig.getSlidingInterval()).getCount());
                withWindow(windowLength, slidingInterval);
            } else {
                withWindow(windowLength);
            }
        } else if (windowConfig.getWindowLength() instanceof Window.Count) {
            Count windowLength = new Count(((Window.Count) windowConfig.getWindowLength()).getCount());
            if (windowConfig.getSlidingInterval() instanceof Window.Duration) {
                Duration slidingInterval = new Duration(((Window.Duration) windowConfig.getWindowLength()).getDurationMs(), TimeUnit.MILLISECONDS);
                withWindow(windowLength, slidingInterval);
            } else if (windowConfig.getSlidingInterval() instanceof Window.Count) {
                Count slidingInterval = new Count(((Window.Count) windowConfig.getWindowLength()).getCount());
                withWindow(windowLength, slidingInterval);
            } else {
                withWindow(windowLength);
            }
        }

        if (windowConfig.getLagMs() != 0) {
            withLag(new Duration(windowConfig.getLagMs(), TimeUnit.MILLISECONDS));
        }

        if (windowConfig.getTsField() != null) {
            withTimestampField(windowConfig.getTsField());
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(dotSeparatedOutputKeyNames));
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        // initialize the hashedInputs data structure
        for (int i = 1; i < streamJoinOrder.size(); i++) {
            hashedInputs.put(streamJoinOrder.get(i),  new HashMap<Object, ArrayList<TupleImpl>>());
        }
        if(outputKeys==null) {
            throw new IllegalArgumentException("Must specify output fields via .select() method.");
        }
    }

    @Override
    public void execute(TupleWindow inputWindow) {

        // 1) Perform Join
        List<Tuple> currentWindow = inputWindow.get();
        JoinAccumulator joinResult = hashJoin(currentWindow);

        // 2) Emit results
        for (ResultRecord resultRecord : joinResult.getRecords()) {
            ArrayList<Object> outputTuple = resultRecord.getOutputFields();
            collector.emit( outputTuple );
        }
    }

    private void clearHashedInputs() {
        for (HashMap<Object, ArrayList<TupleImpl>> mappings : hashedInputs.values()) {
            mappings.clear();
        }
    }

    protected JoinAccumulator hashJoin(List<Tuple> tuples) {
        clearHashedInputs();

        JoinAccumulator probe = new JoinAccumulator();

        // 1) Build phase - Segregate tuples in the Window into streams.
        //    First stream's tuples go into probe, rest into HashMaps in hashedInputs
        String firstStream = streamJoinOrder.get(0);
        for (Tuple t : tuples) {
            TupleImpl tuple = (TupleImpl) t;
            String streamId = getStreamSelector(tuple);
            if ( ! streamId.equals(firstStream) ) {
                Object key = getKeyField(streamId, tuple);
                ArrayList<TupleImpl> recs = hashedInputs.get(streamId).get(key);
                if(recs == null) {
                    recs = new ArrayList<TupleImpl>();
                    hashedInputs.get(streamId).put(key, recs);
                }
                recs.add(tuple);

            }  else {
                ResultRecord probeRecord = new ResultRecord(tuple, streamJoinOrder.size() == 1);
                probe.insert( probeRecord );  // first stream's data goes into the probe
            }
        }

        // 2) Join the streams in order of streamJoinOrder
        for (int i = 1; i < streamJoinOrder.size(); i++) {
            String streamName = streamJoinOrder.get(i) ;
            boolean finalJoin = (i==streamJoinOrder.size()-1);
            probe = doJoin(probe, hashedInputs.get(streamName), joinCriteria.get(streamName), finalJoin );
        }

        return probe;
    }

    // Dispatches to the right join method (inner/left/right/outer) based on the joinInfo.joinType
    protected JoinAccumulator doJoin(JoinAccumulator probe, HashMap<Object, ArrayList<TupleImpl>> buildInput, JoinInfo joinInfo, boolean finalJoin) {
        final JoinType joinType = joinInfo.getJoinType();
        switch ( joinType ) {
            case INNER:
                return doInnerJoin(probe, buildInput, joinInfo, finalJoin);
            case LEFT:
                return doLeftJoin(probe, buildInput, joinInfo, finalJoin);
            case RIGHT:
            case OUTER:
            default:
                throw new RuntimeException("Unsupported join type : " + joinType.name() );
        }
    }

    // inner join - core implementation
    protected JoinAccumulator doInnerJoin(JoinAccumulator probe, Map<Object, ArrayList<TupleImpl>> buildInput, JoinInfo joinInfo, boolean finalJoin) {
        String[] probeKeyName = joinInfo.getOtherKey();
        JoinAccumulator result = new JoinAccumulator();
        for (ResultRecord rec : probe.getRecords()) {
            Object probeKey = rec.getField(joinInfo.otherStream, probeKeyName);
            if (probeKey!=null) {
                ArrayList<TupleImpl> matchingBuildRecs = buildInput.get(probeKey);
                if(matchingBuildRecs!=null) {
                    for (TupleImpl matchingRec : matchingBuildRecs) {
                        ResultRecord mergedRecord = new ResultRecord(rec, matchingRec, finalJoin);
                        result.insert(mergedRecord);
                    }
                }
            }
        }
        return result;
    }

    // left join - core implementation
    protected JoinAccumulator doLeftJoin(JoinAccumulator probe, Map<Object, ArrayList<TupleImpl>> buildInput, JoinInfo joinInfo, boolean finalJoin) {
        String[] probeKeyName = joinInfo.getOtherKey();
        JoinAccumulator result = new JoinAccumulator();
        for (ResultRecord rec : probe.getRecords()) {
            Object probeKey = rec.getField(joinInfo.otherStream, probeKeyName);
            if (probeKey!=null) {
                ArrayList<TupleImpl> matchingBuildRecs = buildInput.get(probeKey); // ok if its return null
                if (matchingBuildRecs!=null && !matchingBuildRecs.isEmpty() ) {
                    for (TupleImpl matchingRec : matchingBuildRecs) {
                        ResultRecord mergedRecord = new ResultRecord(rec, matchingRec, finalJoin);
                        result.insert(mergedRecord);
                    }
                } else {
                    ResultRecord mergedRecord = new ResultRecord(rec, null, finalJoin);
                    result.insert(mergedRecord);
                }

            }
        }
        return result;
    }


    // Identify the key for the stream, and look it up in 'tuple'. key can be nested key:  outerKey.innerKey
    private Object getKeyField(String streamId, TupleImpl tuple) {
        JoinInfo ji = joinCriteria.get(streamId);
        if(ji==null) {
            throw new RuntimeException("Join information for '" + streamId + "' not found. Check the join clauses.");
        }
        return getNestedField(ji.getNestedKeyName(), tuple);
    }

    // Steps down into a nested tuple based on the nestedKeyName
    protected static Object getNestedField(String[] nestedKeyName, TupleImpl tuple) {
        Object curr = null;
        for (int i=0; i < nestedKeyName.length; i++) {
            if (i==0) {
                if (tuple.contains(nestedKeyName[i]) )
                    curr = tuple.getValueByField(nestedKeyName[i]);
                else
                    return null;
            }  else  {
                curr = ((Map) curr).get(nestedKeyName[i]);
                if (curr==null)
                    return null;
            }
        }
        return curr;
    }


    private String getStreamSelector(TupleImpl ti) {
        switch (streamSelectorType) {
            case STREAM:
                return ti.getSourceStreamId();
            case SOURCE:
                return ti.getSourceComponent();
            default:
                throw new RuntimeException(streamSelectorType + " stream selector type not yet supported");
        }
    }


    protected enum JoinType {INNER, LEFT, RIGHT, OUTER}

    /** Describes how to join the other stream with the current stream */
    protected static class JoinInfo implements Serializable {
        final static long serialVersionUID = 1L;

        String[] nestedKeyName;    // nested  key name for the current stream:  outer.inner -> { "outer", "inner }
        String   otherStream;      // name of the other stream to join with
        String[] otherKey;         // key name of the other stream
        JoinType joinType;         // nature of join

        public JoinInfo(String nestedKey) {
            this.nestedKeyName = nestedKey.split("\\.");
            this.otherStream = null;
            this.otherKey = null;
            this.joinType = null;
        }
        public JoinInfo(String nestedKey, String otherStream, JoinInfo otherStreamJoinInfo,  JoinType joinType) {
            this.nestedKeyName = nestedKey.split("\\.");
            this.otherStream = otherStream;
            this.otherKey = otherStreamJoinInfo.nestedKeyName;
            this.joinType = joinType;
        }

        public String[] getNestedKeyName() {
            return nestedKeyName;
        }

        public String getOtherStream() {
            return otherStream;
        }

        public String[] getOtherKey() {
            return otherKey;
        }

        public JoinType getJoinType() {
            return joinType;
        }

    } // class JoinInfo

    // Join helper to concat fields to the record
    protected class ResultRecord {

        ArrayList<TupleImpl> tupleList = new ArrayList<>(); // contains one TupleImpl per Stream being joined
        ArrayList<Object> outputFields = null; // refs to fields that will be part of output fields

        // 'cacheOutputFields' enables us to avoid projection unless it is the final stream being joined
        public ResultRecord(TupleImpl tuple, boolean cacheOutputFields) {
            tupleList.add(tuple);
            if(cacheOutputFields) {
                outputFields = doProjection(tupleList, outputKeys);
            }
        }

        public ResultRecord(ResultRecord lhs, TupleImpl rhs, boolean cacheOutputFields) {
            if(lhs!=null)
                tupleList.addAll(lhs.tupleList);
            if(rhs!=null)
                tupleList.add(rhs);
            if(cacheOutputFields) {
                outputFields = doProjection(tupleList, outputKeys);
            }
        }

        public ArrayList<Object> getOutputFields() {
            return outputFields;
        }

        public Object getField(String stream, String[] nestedFieldName) {
            for (TupleImpl tuple : tupleList) {
                if(getStreamSelector(tuple).equals(stream))
                    return getNestedField(nestedFieldName, tuple);
            }
            return null;
        }
    }

    protected class JoinAccumulator {
        ArrayList<ResultRecord> records = new ArrayList<>();

        public void insert(ResultRecord tuple) {
            records.add( tuple );
        }

        public Collection<ResultRecord> getRecords() {
            return records;
        }
    }

    // Performs projection on the tuples based on the 'projectionKeys'
    protected ArrayList<Object> doProjection(ArrayList<TupleImpl> tuples, String[][] projectionKeys) {
        if(streamLineStyleProjection)
            return doProjectionStreamLine(tuples, outputKeys);

        ArrayList<Object> result = new ArrayList<>(projectionKeys.length);
        // Todo: optimize this computation... perhaps inner loop can be outside to avoid rescanning tuples
        for ( int i = 0; i < projectionKeys.length; i++ ) {
            boolean missingField = true;
            for ( TupleImpl tuple : tuples ) {
                Object field = getNestedField(projectionKeys[i], tuple ) ;
                if (field != null) {
                    result.add(field);
                    missingField=false;
                    break;
                }
            }
            if(missingField) { // add a null for missing fields (usually in case of outer joins)
                result.add(null);
            }
        }
        return result;
    }

    // Performs projection and creates output tuple structure as expected by StreamLine compliant
    protected ArrayList<Object> doProjectionStreamLine(ArrayList<TupleImpl> tuplesRow, String[][] projectionKeys) {

        HashMap<String, Object> projection = new HashMap<>(projectionKeys.length);

        // Todo: note to self: may be able to optimize this ... perhaps inner loop can be outside to avoid rescanning tuples
        for ( int i = 0; i < projectionKeys.length; i++ ) {
            String flattenedKey = dotSeparatedOutputKeyNames[i];
            for ( TupleImpl cell : tuplesRow ) {
                Object field = getNestedField(projectionKeys[i], cell) ;
                if (field != null) {
                    projection.put(flattenedKey, field);
                    break;
}
            }
        }
        ArrayList<Object> resultRow = new ArrayList<>();
        StreamlineEventImpl slEvent = new StreamlineEventImpl(projection, "sourceID");
        resultRow.add(slEvent);
        return resultRow;
    }
}

