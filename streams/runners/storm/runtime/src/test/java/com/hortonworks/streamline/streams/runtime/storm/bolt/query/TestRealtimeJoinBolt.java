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
import org.apache.storm.task.GeneralTopologyContext;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class TestRealtimeJoinBolt {

    String[] adImpressionFields = {"id", "userId", "product"};

    Object[][] adImpressions = {
            {1, 21, "book" },
            {2, 22, "watch" },
            {3, 23, "chair" },
            {4, 24, "tv" },
            {5, 25, "watch" },
            {6, 26, "camera" },
            {7, 27, "book" },
            {8, 28, "tv" },
            {9, 29, "camera" },
            {10,30, "tv" } };

    String[] orderFields = {"id", "userId", "product", "price"};

    Object[][] orders = {
            {11, 21, "book"  , 71},
            {12, 22, "watch" , 330},
            {13, 23, "chair" , 500},
            {14, 29, "tv"    , 2000},    //  matches adImpression on [product] but not on [userId & product]
            {15, 30, "watch" , 400},     //  matches adImpression on [userID] but not on [userId & product]
            {16, 31, "mattress" , 900},  //  this has no match whatsoever in adImpressions
    };



    @Test
    public void testSingleKey_InnerJoin_CountRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.Selector.STREAM)
                .dataStream("orders")
                .innerJoin("ads", new BaseWindowedBolt.Count(10), false)
                .equal(orderFields[1], adImpressionFields[1] )
                .select("orders:id,ads:userId,ads:product,orders:product,price");

        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, null, collector);

        for (Tuple tuple : adImpressionStream) {
            bolt.execute(tuple);
        }
        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }

        printResults(collector);
        Assert.assertEquals( 5, collector.actualResults.size() );
    }

    @Test
    public void testSingleKey_InnerJoin_TimeRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.Selector.STREAM)
                .dataStream("orders")
                .innerJoin("ads", new BaseWindowedBolt.Duration(2, TimeUnit.SECONDS), false)
                .equal(orderFields[1], adImpressionFields[1] )
                .select("orders:id,ads:userId,product,price");

        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, null, collector);

        for (Tuple tuple : adImpressionStream) {
            bolt.execute(tuple);
        }
        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }

        printResults(collector);
        Assert.assertEquals( 5, collector.actualResults.size() );
    }

    @Test
    public void testSingleKey_LeftJoin() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.Selector.STREAM)
                .dataStream("ads")
                .leftJoin("orders", new BaseWindowedBolt.Count(10), false)
                .equal(adImpressionFields[1],orderFields[1] )
                .select("orders:id,ads:userId,ads:product,orders:product,price");

        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, null, collector);

        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }

        for (Tuple tuple : adImpressionStream) {
            bolt.execute(tuple);
        }

        printResults(collector);
        Assert.assertEquals( adImpressionStream.size(), collector.actualResults.size() );
    }

    @Test
    public void testSingleKey_RightJoin() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.Selector.STREAM)
                .dataStream("ads")
                .rightJoin("orders", new BaseWindowedBolt.Duration(1, TimeUnit.SECONDS), false)
                .equal(adImpressionFields[1], orderFields[1])
                .select("orders:id,ads:userId,ads:product,orders:product,price");

        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, null, collector);

        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }

        // emit all ads but last one
        for (int i = 0; i < adImpressionStream.size()-1; i++) {
            bolt.execute( adImpressionStream.get(i) );
        }

        // sleep to allow expiration of all buffered orders and trigger an emit of unmatched orders on next execute()
        Thread.sleep(1_100);
        bolt.execute( adImpressionStream.get(adImpressionStream.size()-1) );

        printResults(collector);
        Assert.assertEquals( orderStream.size(), collector.actualResults.size() );
    }


    @Test
    public void testSingleKey_OuterJoin() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.Selector.STREAM)
                .dataStream("ads")
                .outerJoin("orders", new BaseWindowedBolt.Duration(1, TimeUnit.SECONDS), false)
                .equal(adImpressionFields[1], orderFields[1])
                .select(" orders:id as orderId, ads:userId  as  userId ,ads:product, orders:product, price"); // extra spaces are to test FieldDescriptor

        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, null, collector);

        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }

        // emit all ads but last one
        for (int i = 0; i < adImpressionStream.size()-1; i++) {
            bolt.execute( adImpressionStream.get(i) );
        }

        // sleep to allow expiration of all buffered orders and trigger an emit of unmatched orders on next execute()
        Thread.sleep(1_100);
        bolt.execute( adImpressionStream.get(adImpressionStream.size()-1) );

        printResults(collector);
        Assert.assertEquals( adImpressionStream.size()+2, collector.actualResults.size() );
    }

    @Test
    public void testMultiKey_InnerJoin_CountRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.Selector.STREAM)
                .dataStream("orders")
                .innerJoin("ads", new BaseWindowedBolt.Count(10), false)
                .equal(orderFields[1], adImpressionFields[1] )
                .equal(orderFields[2], adImpressionFields[2] )
                .select("orders:id,ads:userId,ads:product,orders:product,price");

        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, null, collector);

        for (Tuple tuple : adImpressionStream) {
            bolt.execute(tuple);
        }
        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }

        printResults(collector);
        Assert.assertEquals( 3, collector.actualResults.size() );
    }


    @Test
    public void testStreamline_InnerJoin_TimeRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStreamLineEventStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStreamLineEventStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.Selector.STREAM)
                .dataStream("orders")
                .innerJoin("ads", new BaseWindowedBolt.Duration(2, TimeUnit.SECONDS), false)
                .streamlineEqual(orderFields[1], adImpressionFields[1] )
                .streamlineEqual(orderFields[2], adImpressionFields[2] )
                .streamlineSelect("orders:id,ads:userId,product,price");

        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, null, collector);

        for (Tuple tuple : adImpressionStream) {
            bolt.execute(tuple);
        }
        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }

        printResults_StreamLine(collector);
        Assert.assertEquals( 3, collector.actualResults.size() );
    }


    private static ArrayList<Tuple> makeStream(String streamName, String[] fieldNames, Object[][] data) {
        ArrayList<Tuple> result = new ArrayList<>();
        MockContext mockContext = new MockContext(fieldNames);

        for (Object[] record : data) {
            TupleImpl rec = new TupleImpl(mockContext, Arrays.asList(record), 0, streamName);
            result.add( rec );
        }

        return result;
    }

    // NOTE: Streamline Specific
    private static ArrayList<Tuple> makeStreamLineEventStream (String streamName, String[] fieldNames, Object[][] records) {

        MockContext mockContext = new MockContext(new String[]{StreamlineEvent.STREAMLINE_EVENT} );
        ArrayList<Tuple> result = new ArrayList<>(records.length);

        // convert each record into a HashMap using fieldNames as keys
        for (Object[] record : records) {
            HashMap<String,Object> recordMap = new HashMap<>( fieldNames.length );
            for (int i = 0; i < fieldNames.length; i++) {
                recordMap.put(fieldNames[i], record[i]);
            }
            StreamlineEvent streamLineEvent = new StreamlineEventImpl(recordMap, "multiple sources");
            ArrayList<Object> tupleValues = new ArrayList<>(1);
            tupleValues.add(streamLineEvent);
            TupleImpl tuple = new TupleImpl(mockContext, tupleValues, 0, streamName);
            result.add( tuple );
        }

        return result;
    }


    private static void printResults(MockCollector collector) {
        System.out.println( String.join(",", collector.outputFields) );
        System.out.println("--------------------------------------------------");
        int counter=0;
        for (List<Object> rec : collector.actualResults) {
            System.out.print(++counter +  ") ");
            for (Object field : rec) {
                System.out.print(field + ", ");
            }
            System.out.println("");
        }
    }

    private static void printResults_StreamLine(MockCollector collector) {
        int counter=0;
        System.out.println( String.join(",", collector.outputFields) );
        System.out.println("--------------------------------------------------");
        for (List<Object> rec : collector.actualResults) {
            System.out.print(++counter +  ") ");
            for (Object field : rec) {
                Map<String, Object> data = ((StreamlineEvent)field);
                data.forEach((k,v)-> {
                    System.out.print(k + ":" + v + ", ");
                } );
                System.out.println();
            }
            System.out.println("");
        }
    }

    static class MockCollector extends OutputCollector {
        private final String[] outputFields;

        public ArrayList<List<Object>> actualResults = new ArrayList<>();

        public MockCollector(String[] outputFields) {
            super(null);
            this.outputFields = outputFields;
        }


        @Override
        public List<Integer> emit(Collection<Tuple> anchors, List<Object> tuple) {
            actualResults.add(tuple);
            return null;
        }

        @Override
        public List<Integer> emit(Tuple anchor, List<Object> tuple) {
            actualResults.add(tuple);
            return null;
        }

        @Override
        public void ack(Tuple input) {
            // no-op
        }
    } // class MockCollector

    static class MockContext extends GeneralTopologyContext {

        private final Fields fields;

        public MockContext(String[] fieldNames) {
            super(null, null, null, null, null, null);
            this.fields = new Fields(fieldNames);
        }

        public String getComponentId(int taskId) {
            return "component";
        }

        public Fields getComponentOutputFields(String componentId, String streamId) {
            return fields;
        }

    }
}
