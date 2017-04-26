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

import com.google.common.collect.LinkedListMultimap;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;


public class RealtimeJoinBolt extends BaseRichBolt  {
    private static final Logger LOG = LoggerFactory.getLogger(RealtimeJoinBolt.class);
    final static String EVENT_PREFIX = StreamlineEvent.STREAMLINE_EVENT + ".";

    private String dataStream;
    private String lookupStream;

    protected FieldSelector[] outputFields = null;  // specified via bolt.select() ... used in declaring Output fields
    private String outputStream;
    private int retentionTime;
    private int retentionCount;
    private boolean timeBasedRetention;

    private LinkedListMultimap<String, TupleInfo> lookupBuffer;

    private ArrayDeque<Long> timeTracker; // for time based retention
    private ArrayList<JoinInfo> joinCriteria = new ArrayList<>();

    private OutputCollector collector;
    private boolean dropOlderDuplicates;
    private boolean streamLineProjection = false; // NOTE: Streamline Specific


    protected enum JoinType {INNER, LEFT, RIGHT, OUTER}
    private JoinType joinType;

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        String[] outputFieldNames = new String[outputFields.length];
        for( int i=0; i<outputFields.length; ++i ) {
            outputFieldNames[i] = outputFields[i].getOutputName() ;
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
        if (timeBasedRetention) {
            lookupBuffer =  LinkedListMultimap.create(50_000);
            timeTracker = new ArrayDeque<Long>();
        } else { // count Based Retention
            lookupBuffer = LinkedListMultimap.create(retentionCount);
        }
    }


    public enum Selector { STREAM, SOURCE }
    // Indicates if we are using streamId or source component name to distinguish incoming tuple streams
    protected final Selector selectorType;

    /**
     * Constructor
     * @param streamType   Specifies whether 'dataStream' refers to a stream name or source component id.
     */
    public RealtimeJoinBolt(Selector streamType) {
        selectorType = streamType;
    }


    // NOTE: streamline specific
    /**
     * Calls  RealtimeJoinBolt(Selector.STREAM)
     */
    public RealtimeJoinBolt() {
        this(Selector.STREAM);
    }

    /**
     * Specify Fields to use for join. Field name can be nested x.y.z  (assumes x & y are of type Map<> )
     * @param dataStreamField    Field to use for join on data stream.
     * @param lookupStreamField  Field to use for join on lookup stream.
     * @return
     */
    public RealtimeJoinBolt equal(String dataStreamField, String lookupStreamField) {
        FieldSelector dataField = new FieldSelector(dataStream, dataStreamField);
        FieldSelector lookupField = new FieldSelector(lookupStream, lookupStreamField);

        if( dataField.equals(lookupField) ) {
            throw new IllegalArgumentException("Both field selectors refer to same field: " + dataField.getOutputName());
        }
        joinCriteria.add(new JoinInfo(lookupField, dataField));
        return this;
    }

    // NOTE: Streamline specific convenience method. Prefixes the key names with 'streamline-event.'
    /**
     * Specify Fields to use for join. Field name can be nested x.y.z  (assumes x & y are of type Map<> )
     * @param dataStreamField    Field to use for join on data stream.
     * @param lookupStreamField  Field to use for join on lookup stream.
     * @return
     */
    public RealtimeJoinBolt streamlineEqual(String dataStreamField, String lookupStreamField) {
        return equal(EVENT_PREFIX+dataStreamField, EVENT_PREFIX+lookupStreamField);
    }


    /**
     * Introduces the buffered 'LookupStream' for inner join and retention policy
     * @param lookupStream   Name of the stream (or source component Id) to be treated as a buffered 'LookupStream'
     * @param retentionCount How many records to retain.
     * @param dropOlderDuplicates if records should be de-duped when buffered in order to retain the latest data
     * @return
     */
    public RealtimeJoinBolt innerJoin(String lookupStream, BaseWindowedBolt.Count retentionCount, boolean dropOlderDuplicates) {
        if(this.lookupStream!=null) {
            throw  new IllegalArgumentException("Cannot declare a Lookup stream more than once");
        }
        this.lookupStream = lookupStream;
        this.dropOlderDuplicates = dropOlderDuplicates;
        this.retentionCount = retentionCount.value;
        this.timeBasedRetention = false;
        this.joinType = JoinType.INNER;
        return this;
    }

    public RealtimeJoinBolt innerJoin(String lookupStream, BaseWindowedBolt.Duration retentionTime, boolean dropOlderDuplicates) {
        if (this.lookupStream!=null) {
            throw  new IllegalArgumentException("Cannot declare a Lookup stream more than once");
        }
        if (retentionTime.value<=0) {
            throw  new IllegalArgumentException("Retention time must be positive number");
        }

        this.lookupStream = lookupStream;
        this.dropOlderDuplicates = dropOlderDuplicates;
        this.retentionTime = retentionTime.value;
        this.timeBasedRetention = true;
        this.joinType = JoinType.INNER;
        return this;
    }

    public RealtimeJoinBolt leftJoin(String lookupStream, BaseWindowedBolt.Count retentionCount, boolean dropOlderDuplicates) {
        if(this.lookupStream!=null) {
            throw  new IllegalArgumentException("Cannot declare a Lookup stream more than once");
        }
        this.lookupStream = lookupStream;
        this.dropOlderDuplicates = dropOlderDuplicates;
        this.retentionCount = retentionCount.value;
        this.timeBasedRetention = false;
        this.joinType = JoinType.LEFT;
        return this;
    }

    public RealtimeJoinBolt leftJoin(String lookupStream, BaseWindowedBolt.Duration retentionTime, boolean dropOlderDuplicates) {
        if(this.lookupStream!=null) {
            throw  new IllegalArgumentException("Cannot declare a Lookup stream more than once");
        }
        if (retentionTime.value<=0) {
            throw  new IllegalArgumentException("Retention time must be positive number");
        }
        this.lookupStream = lookupStream;
        this.dropOlderDuplicates = dropOlderDuplicates;
        this.retentionTime = retentionTime.value;
        this.timeBasedRetention = true;
        this.joinType = JoinType.LEFT;
        return this;
    }

    public RealtimeJoinBolt rightJoin(String lookupStream, BaseWindowedBolt.Count retentionCount, boolean dropOlderDuplicates) {
        if(this.lookupStream!=null) {
            throw  new IllegalArgumentException("Cannot declare a Lookup stream more than once");
        }
        this.lookupStream = lookupStream;
        this.dropOlderDuplicates = dropOlderDuplicates;
        this.retentionCount = retentionCount.value;
        this.timeBasedRetention = false;
        this.joinType = JoinType.RIGHT;
        return this;
    }

    public RealtimeJoinBolt rightJoin(String lookupStream, BaseWindowedBolt.Duration retentionTime, boolean dropOlderDuplicates) {
        if(this.lookupStream!=null) {
            throw  new IllegalArgumentException("Cannot declare a Lookup stream more than once");
        }
        if (retentionTime.value<=0) {
            throw  new IllegalArgumentException("Retention time must be positive number");
        }
        this.lookupStream = lookupStream;
        this.dropOlderDuplicates = dropOlderDuplicates;
        this.retentionTime = retentionTime.value;
        this.timeBasedRetention = true;
        this.joinType = JoinType.RIGHT;
        return this;
    }

    public RealtimeJoinBolt outerJoin(String lookupStream, BaseWindowedBolt.Count retentionCount, boolean dropOlderDuplicates) {
        if(this.lookupStream!=null) {
            throw  new IllegalArgumentException("Cannot declare a Lookup stream more than once");
        }
        this.lookupStream = lookupStream;
        this.dropOlderDuplicates = dropOlderDuplicates;
        this.retentionCount = retentionCount.value;
        this.timeBasedRetention = false;
        this.joinType = JoinType.OUTER;
        return this;
    }

    public RealtimeJoinBolt outerJoin(String lookupStream, BaseWindowedBolt.Duration retentionTime, boolean dropOlderDuplicates) {
        if(this.lookupStream!=null) {
            throw  new IllegalArgumentException("Cannot declare a Lookup stream more than once");
        }
        if (retentionTime.value<=0) {
            throw  new IllegalArgumentException("Retention time must be positive number");
        }
        this.lookupStream = lookupStream;
        this.dropOlderDuplicates = dropOlderDuplicates;
        this.retentionTime = retentionTime.value;
        this.timeBasedRetention = true;
        this.joinType = JoinType.OUTER;
        return this;
    }

    public RealtimeJoinBolt dataStream(String dataStream) {
        if (this.dataStream!=null) {
            throw  new IllegalArgumentException("Cannot declare a Data stream more than once");
        }
        this.dataStream = dataStream;
        return this;
    }

    /**
     * Specify output fields
     *      e.g: .select("lookupField, stream2:dataField, field3")
     * Nested Key names are supported for nested types:
     *      e.g: .select("outerKey1.innerKey1, outerKey1.innerKey2, stream3:outerKey2.innerKey3)"
     * Inner types (non leaf) must be Map<> in order to support nested lookup using this dot notation
     * This selected fields implicitly declare the output fieldNames for the bolt based.
     * @param commaSeparatedKeys
     * @return
     */
    public RealtimeJoinBolt select(String commaSeparatedKeys) {
        String[] fieldNames = commaSeparatedKeys.split(",");

        outputFields = new FieldSelector[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            outputFields[i] = new FieldSelector(fieldNames[i]);
        }
        return this;
    }

    /** Convenience method for Streamline that prefixes each keyname with 'streamline-event.'
     *
     * @param commaSeparatedKeys
     * @return
     */
    public RealtimeJoinBolt streamlineSelect(String commaSeparatedKeys) {
        String prefixedKeys = convertToStreamLineKeys(commaSeparatedKeys);
        streamLineProjection = true;
        return  select(prefixedKeys);
    }

    public RealtimeJoinBolt withOutputStream(String streamName) {
        this.outputStream = streamName;
        return this;
    }

    @Override
    public void execute(Tuple tuple) {
        if (timeBasedRetention) {
            expireAndAckTimedOutEntries(lookupBuffer);
        }

        try {
            String streamId = getStreamSelector(tuple);
            if ( isLookupStream(streamId) ) {
                processLookupStreamTuple(tuple);
            } else if (isDataStream(streamId) ) {
                processDataStreamTuple(tuple);
            } else {
                throw new InvalidTuple("Received tuple from unexpected stream/source : " + streamId, tuple);
            }
        } catch (InvalidTuple e) {
            collector.ack(tuple);
            LOG.warn("{}. Tuple will be dropped.",  e.toString());
        }
    }

    private void processDataStreamTuple(Tuple tuple) throws InvalidTuple {
        List<TupleInfo> matches = matchWithLookupStream(tuple);
        if (matches==null || matches.isEmpty() ) {  // no match
            if (joinType== JoinType.LEFT ||  joinType== JoinType.OUTER ) {
                List<Object> outputTuple = doProjection(tuple, null);
                emit(outputTuple, tuple);
                return;
            }
            collector.ack(tuple);
        }  else {
            for (TupleInfo lookupTuple : matches) { // match found
                lookupTuple.unmatched = false;
                List<Object> outputTuple = doProjection(lookupTuple.tuple, tuple);
                emit(outputTuple, tuple, lookupTuple.tuple);
            }
            collector.ack(tuple);
        }
    }

    private void processLookupStreamTuple(Tuple tuple) throws InvalidTuple {
        String key = makeLookupTupleKey(tuple);
        if(dropOlderDuplicates)
            lookupBuffer.removeAll(key);
        lookupBuffer.put(key, new TupleInfo(tuple) );

        if(timeBasedRetention) {
            timeTracker.add(System.currentTimeMillis());
        } else {  // count based Rotation
            if (lookupBuffer.size() > retentionCount) {
                TupleInfo expired = removeHead(lookupBuffer);
                if( (joinType== JoinType.RIGHT) || (joinType== JoinType.OUTER)  ) {
                    emitUnMatchedTuples(expired);
                }
                collector.ack(expired.tuple);
            }
        }
    }

    private String makeLookupTupleKey(Tuple tuple) throws InvalidTuple {
        StringBuilder key = new StringBuilder();
        for (JoinInfo ji : joinCriteria) {
            String partialKey = findField(ji.lookupField, tuple).toString();
            if (partialKey==null)
                throw new InvalidTuple("'" +ji.lookupField + "' field is is missing in the tuple", tuple);
            key.append( partialKey );
            key.append(".");
        }
        return key.toString();
    }

    private String makeDataTupleKey(Tuple tuple)  throws InvalidTuple  {
        StringBuilder key = new StringBuilder();
        for (JoinInfo ji : joinCriteria) {
            String partialKey = findField(ji.dataField, tuple).toString();
            if (partialKey==null)
                throw new InvalidTuple("'" + ji.dataField + " field is is missing in the tuple", tuple);
            key.append( partialKey );
            key.append(".");
        }
        return key.toString();
    }

    // returns null if no match
    private List<TupleInfo> matchWithLookupStream(Tuple lookupTuple) throws InvalidTuple {
        String key = makeDataTupleKey(lookupTuple);
        return lookupBuffer.get(key);
    }

    // Removes timedout entries from lookupBuffer & timeTracker. Acks tuples being expired.
    private void expireAndAckTimedOutEntries(LinkedListMultimap<String, TupleInfo> lookupBuffer) {
        Long expirationTime = System.currentTimeMillis() - retentionTime;
        Long  insertionTime = timeTracker.peek();
        while ( insertionTime!=null  &&   expirationTime > insertionTime ) {
            TupleInfo expired = removeHead(lookupBuffer);
            timeTracker.pop();
            if ( joinType == JoinType.RIGHT || joinType == JoinType.OUTER )
                emitUnMatchedTuples(expired);
            collector.ack(expired.tuple);
            insertionTime = timeTracker.peek();
        }
    }

    private void emitUnMatchedTuples(TupleInfo expired) {
        if(expired.unmatched) {
            List<Object> outputTuple = doProjection(expired.tuple, null);
            emit(outputTuple, expired.tuple);
        }
    }

    private static TupleInfo removeHead(LinkedListMultimap<String, TupleInfo> lookupBuffer) {
        List<Map.Entry<String, TupleInfo>> entries = lookupBuffer.entries();
        return entries.remove(0).getValue();
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

    private boolean isDataStream(String streamId) {
        return streamId.equals(dataStream);
    }

    private boolean isLookupStream(String streamId) {
        return streamId.equals(lookupStream);
    }

    // Returns either the source component name or the stream name for the tuple
    private String getStreamSelector(Tuple ti) {
        switch (selectorType) {
            case STREAM:
                return ti.getSourceStreamId();
            case SOURCE:
                return ti.getSourceComponent();
            default:
                throw new RuntimeException(selectorType + " stream selector type not yet supported");
        }
    }

    // Extract the field from tuple. Can be a nested field (x.y.z)
    // returns null if not found
    protected Object findField(FieldSelector fieldSelector, Tuple tuple) {
        if (tuple==null) {
            return null;
        }
        // very stream name matches, it stream name was specified
        if ( fieldSelector.streamName!=null &&
                !fieldSelector.streamName.equalsIgnoreCase( getStreamSelector(tuple) ) ) {
            return null;
        }

        Object curr = null;
        for (int i=0; i < fieldSelector.field.length; i++) {
            if (i==0) {
                if (tuple.contains(fieldSelector.field[i]) )
                    curr = tuple.getValueByField(fieldSelector.field[i]);
                else
                    return null;
            }  else  {
                curr = ((Map) curr).get(fieldSelector.field[i]);
                if (curr==null)
                    return null;
            }
        }
        return curr;
    }

    /** Performs projection on the tuples based on 'projectionFields'
     *
     * @param tuple1   can be null
     * @param tuple2   can be null
     * @return   project fields
     */
    protected List<Object> doProjection(Tuple tuple1, Tuple tuple2) {
        if(streamLineProjection)
            return doStreamlineProjection(tuple1, tuple2);

        ArrayList<Object> result = new ArrayList<>(outputFields.length);
        for ( int i = 0; i < outputFields.length; i++ ) {
            FieldSelector outField = outputFields[i];
            Object field = findField(outField, tuple1) ;
            if (field==null)
                field = findField(outField, tuple2);
            result.add(field); // adds null if field is not found in both tuples
        }
        return result;
    }

    // NOTE: Streamline specific convenience method. Creates output tuple as a StreamlineEvent
    protected List<Object> doStreamlineProjection(Tuple tuple1, Tuple tuple2) {
//        String flattenedKey = projectionKeys[i].getOutputName();
//        String outputKeyName = dropStreamLineEventPrefix(flattenedKey); // drops the "streamline-event." prefix
        StreamlineEventImpl.Builder eventBuilder = StreamlineEventImpl.builder();

        for ( int i = 0; i < outputFields.length; i++ ) {
            FieldSelector outField = outputFields[i];

            Object field = findField(outField, tuple1) ;
            if (field==null)
                field = findField(outField, tuple2);
            String outputKeyName = dropStreamLineEventPrefix(outField.getOutputName() );
            eventBuilder.put(outputKeyName, field); // adds null if field is not found in both tuples
        }

        ArrayList<Object> resultRow = new ArrayList<>();
        StreamlineEventImpl slEvent = eventBuilder.dataSourceId("multiple sources").build();
        resultRow.add(slEvent);
        Collections.singletonList();
        return resultRow;

    }


    // Prefixes each key with 'streamline-event.' Example:
    //   arg = "stream1:key1, key2, stream2:key3.key4, key5"
    //   result  = "stream1:streamline-event.key1, streamline-event.key2, stream2:streamline-event.key3.key4, streamline-event.key5"
    private static String convertToStreamLineKeys(String commaSeparatedKeys) {
        String[] keyNames = commaSeparatedKeys.replaceAll("\\s+","").split(",");

        String[] prefixedKeys = new String[keyNames.length];

        for (int i = 0; i < keyNames.length; i++) {
            FieldSelector fs = new FieldSelector(keyNames[i]);
            if (fs.streamName==null)
                prefixedKeys[i] =  EVENT_PREFIX + String.join(".", fs.getField());
            else
                prefixedKeys[i] =  fs.streamName + ":" + EVENT_PREFIX + String.join(".", fs.getField());
        }

        return String.join(", ", prefixedKeys);
    }

    private static String dropStreamLineEventPrefix(String flattenedKey) {
        int pos = flattenedKey.indexOf(EVENT_PREFIX);
        if(pos==0)
            return flattenedKey.substring(EVENT_PREFIX.length());
        return flattenedKey.substring(0,pos) + flattenedKey.substring(pos+EVENT_PREFIX.length());
    }

}


class FieldSelector implements Serializable {
    final static long serialVersionUID = 2L;

    String streamName;    // can be null;
    String[] field;       // nested field "x.y.z"  becomes => String["x","y","z"]
    String outputName;    // either "stream1:x.y.z" or "x.y.z" depending on whether stream name is present.

    public FieldSelector(String fieldDescriptor)  {  // sample fieldDescriptor = "stream1:x.y.z"
        int pos = fieldDescriptor.indexOf(':');

        if (pos>0) {  // stream name is specified
            streamName = fieldDescriptor.substring(0,pos).trim();
            outputName = fieldDescriptor.trim();
            field =  fieldDescriptor.substring(pos+1, fieldDescriptor.length()).split("\\.");
            return;
        }

        // stream name unspecified
        streamName = null;
        if(pos==0) {
            outputName = fieldDescriptor.substring(1, fieldDescriptor.length() ).trim();

        } else if (pos<0) {
            outputName = fieldDescriptor.trim();
        }
        field =  outputName.split("\\.");
    }

    /**
     * @param stream name of stream
     * @param fieldDescriptor  Simple fieldDescriptor like "x.y.z" and w/o a 'stream1:' stream qualifier.
     */
    public FieldSelector(String stream, String fieldDescriptor)  {
        this(stream + ":" + fieldDescriptor);
        if(fieldDescriptor.indexOf(":")>=0) {
            throw new IllegalArgumentException("Not expecting stream qualifier ':' in '" + fieldDescriptor
                    + "'. Stream name '" + stream +  "' is implicit in this context");
        }
        this.streamName = stream;
    }

    public String[] getField() {
        return field;
    }

    public String getFieldName() {
        if(streamName!=null)
            return streamName + ":" + field;
        return getOutputName();
    }


    public String getOutputName() {
        return outputName;
    }

    @Override
    public String toString() {
        return outputName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        try {
            FieldSelector that = (FieldSelector) o;
            return outputName != null ? outputName.equals(that.outputName) : that.outputName == null;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return outputName != null ? outputName.hashCode() : 0;
    }
}


class JoinInfo implements Serializable {
    final static long serialVersionUID = 1L;

    FieldSelector lookupField;           // field for the current stream
    FieldSelector dataField;      // field for the other (2nd) stream


    public JoinInfo(FieldSelector lookupField, FieldSelector dataField) {
        this.lookupField = lookupField;
        this.dataField = dataField;
    }

} // class JoinInfo

class TupleInfo {
    boolean unmatched = true; // indicates if 'tuple' has been matched with at least one tuple from other stream
    Tuple tuple;

    public TupleInfo(Tuple tuple) {
        this.tuple = tuple;
    }
}

class InvalidTuple extends Exception {
    Tuple tuple;

    public InvalidTuple(String errMsg, Tuple tuple) {
        super(errMsg);
        this.tuple = tuple;
    }
}