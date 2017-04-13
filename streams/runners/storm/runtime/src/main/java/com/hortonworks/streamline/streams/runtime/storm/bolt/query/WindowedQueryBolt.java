/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/

package com.hortonworks.streamline.streams.runtime.storm.bolt.query;


import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Window;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.bolt.JoinBolt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


/* This class adapts Storm's JoinBolt for StreamLine.
 * It  overrides some methods for convenient usage in Streamline. E.g.
 *     - Only allows joins based on stream names (not component names, etc)
 *     - Fields names provided to join(), leftJoin() & selectStreamLine() are auto prefixed with 'streamline-event.'
 *     - The 'streamline-event.' prefix is hidden from appearing in names of output fields (of projection)
 */
public class WindowedQueryBolt extends JoinBolt {

    final static String EVENT_PREFIX = StreamlineEvent.STREAMLINE_EVENT + ".";

    public WindowedQueryBolt(String streamId, String key) {
        super(Selector.STREAM, streamId, EVENT_PREFIX + key);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(outputStreamName, new Fields(StreamlineEvent.STREAMLINE_EVENT));
    }

    @Override
    public WindowedQueryBolt join(String newStream, String key, String priorStream) {
        return (WindowedQueryBolt) super.join(newStream, EVENT_PREFIX + key, priorStream);
    }

    @Override
    public WindowedQueryBolt leftJoin(String newStream, String key, String priorStream) {
        return (WindowedQueryBolt) super.leftJoin(newStream, EVENT_PREFIX + key, priorStream);
    }

    public WindowedQueryBolt selectStreamLine(String commaSeparatedKeys) {
        // prefix each key with "streamline-event."
        String prefixedKeyNames = convertToStreamLineKeys(commaSeparatedKeys);
        return (WindowedQueryBolt) select(prefixedKeyNames);
    }

    /**
     * Defines the name of the output stream
     */
    @Override
    public WindowedQueryBolt withOutputStream(String streamName) {
        return (WindowedQueryBolt) super.withOutputStream(streamName);
    }


    // Prefixes each key with 'streamline-event.' Example:
    //   arg = "stream1:key1, key2, stream2:key3.key4, key5"
    //   result  = "stream1:streamline-event.key1, streamline-event.key2, stream2:streamline-event.key3.key4, streamline-event.key5"
    private static String convertToStreamLineKeys(String commaSeparatedKeys) {
        String[] keyNames = commaSeparatedKeys.replaceAll("\\s+","").split(",");

        String[] prefixedKeys = new String[keyNames.length];

        for (int i = 0; i < keyNames.length; i++) {
            FieldSelector fs = new FieldSelector(keyNames[i]);
            if (fs.getStreamName()==null)
                prefixedKeys[i] =  EVENT_PREFIX + String.join(".", fs.getField());
            else
                prefixedKeys[i] =  fs.getStreamName() + ":" + EVENT_PREFIX + String.join(".", fs.getField());
        }

        return String.join(", ", prefixedKeys);
    }

    // Performs projection on the tuples based on the 'projectionKeys'
    @Override
    protected ArrayList<Object> doProjection(ArrayList<Tuple> tuples, FieldSelector[] projectionFields) {
        return doProjectionStreamLine(tuples, projectionFields);
    }

    // Overrides projection behavior to customize for handling of "streamline-event." prefix
    protected ArrayList<Object> doProjectionStreamLine(ArrayList<Tuple> tuplesRow, FieldSelector[] projectionKeys) {

        StreamlineEventImpl.Builder eventBuilder = StreamlineEventImpl.builder();
        // Todo: note to self: may be able to optimize this ... perhaps inner loop can be outside to avoid rescanning tuples
        for ( int i = 0; i < projectionKeys.length; i++ ) {
            String flattenedKey = projectionKeys[i].getOutputName();
            String outputKeyName = dropStreamLineEventPrefix(flattenedKey); // drops the "streamline-event." prefix
            for ( Tuple cell : tuplesRow ) {
                Object field = lookupField(projectionKeys[i], cell) ;
                if (field != null) {
                    eventBuilder.put(outputKeyName, field);
                    break;
                }
            }
        }
        ArrayList<Object> resultRow = new ArrayList<>();
        StreamlineEventImpl slEvent = eventBuilder.dataSourceId("multiple sources").build();
        resultRow.add(slEvent);
        return resultRow;
    }

    private static String dropStreamLineEventPrefix(String flattenedKey) {
        return flattenedKey.substring(flattenedKey.indexOf('.')+1);
    }

    public void withWindowConfig(Window windowConfig) throws IOException {
        if (windowConfig.getWindowLength() instanceof Window.Duration) {
            BaseWindowedBolt.Duration windowLength = new BaseWindowedBolt.Duration(((Window.Duration) windowConfig.getWindowLength()).getDurationMs(), TimeUnit.MILLISECONDS);
            if (windowConfig.getSlidingInterval() instanceof Window.Duration) {
                BaseWindowedBolt.Duration slidingInterval = new BaseWindowedBolt.Duration(((Window.Duration) windowConfig.getSlidingInterval()).getDurationMs(), TimeUnit.MILLISECONDS);
                withWindow(windowLength, slidingInterval);
            } else if (windowConfig.getSlidingInterval() instanceof Window.Count) {
                BaseWindowedBolt.Count slidingInterval = new BaseWindowedBolt.Count(((Window.Count) windowConfig.getSlidingInterval()).getCount());
                withWindow(windowLength, slidingInterval);
            } else {
                withWindow(windowLength);
            }
        } else if (windowConfig.getWindowLength() instanceof Window.Count) {
            BaseWindowedBolt.Count windowLength = new BaseWindowedBolt.Count(((Window.Count) windowConfig.getWindowLength()).getCount());
            if (windowConfig.getSlidingInterval() instanceof Window.Duration) {
                BaseWindowedBolt.Duration slidingInterval = new BaseWindowedBolt.Duration(((Window.Duration) windowConfig.getWindowLength()).getDurationMs(), TimeUnit.MILLISECONDS);
                withWindow(windowLength, slidingInterval);
            } else if (windowConfig.getSlidingInterval() instanceof Window.Count) {
                BaseWindowedBolt.Count slidingInterval = new BaseWindowedBolt.Count(((Window.Count) windowConfig.getWindowLength()).getCount());
                withWindow(windowLength, slidingInterval);
            } else {
                withWindow(windowLength);
            }
        }

        if (windowConfig.getLagMs() != 0) {
            withLag(new BaseWindowedBolt.Duration(windowConfig.getLagMs(), TimeUnit.MILLISECONDS));
        }

        if (windowConfig.getTsField() != null) {
            withTimestampField(windowConfig.getTsField());
        }
    }
}

