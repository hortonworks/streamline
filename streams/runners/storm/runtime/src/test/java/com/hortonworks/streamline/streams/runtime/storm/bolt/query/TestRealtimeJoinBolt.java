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

import clojure.lang.Atom;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.runtime.storm.bolt.query.RealtimeJoinBolt.StreamKind;
import org.apache.storm.Constants;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.metric.api.IMetric;
import org.apache.storm.task.GeneralTopologyContext;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.*;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
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
            {14, 32, "tv"    , 2000},    //  matches adImpression on [product] but not on [userId & product] or [userid]
            {15, 30, "watch" , 400},     //  matches adImpression on [userID] or [product] but not on [userId & product]
            {16, 31, "mattress" , 900},  //  this has no match whatsoever in adImpressions
    };



    @Test
    public void testSingleKey_InnerJoin_CountRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.StreamKind.STREAM)
                .from("orders", 6, false )
                .innerJoin("ads", 10, false, Cmp.equal("userId", "orders:userId") )
                .select("ads:id,orders:id,ads:userId,ads:product,orders:product,price");

        MockTopologyContext context = new MockTopologyContext(bolt.getOutputFields());
        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, context, collector);

        for (Tuple tuple : adImpressionStream) {
            bolt.execute(tuple);
        }
        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }

        printResults(collector);
        Assert.assertEquals( 4, collector.actualResults.size() );
    }

    @Test
    public void testSingleKey_InnerJoin_TimeRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.StreamKind.STREAM)
                .from("orders", Duration.ofSeconds(1), false)
                .innerJoin("ads", Duration.ofSeconds(2), false, Cmp.equal("userId", "orders:userId"))
                .select("ads:id,orders:id,userId,ads:product,orders:product,price");

        MockTopologyContext context = new MockTopologyContext(bolt.getOutputFields());
        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, context, collector);

        for (Tuple tuple : adImpressionStream) {
            bolt.execute(tuple);
        }
        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }
        Thread.sleep( Duration.ofSeconds(2).toMillis() );
        bolt.execute(makeTickTuple());

        printResults(collector);
        Assert.assertEquals( 4, collector.actualResults.size() );
    }

    @Test
    public void testSingleKey_InnerJoin_TimeRetention_Unique() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        appendToStream(orderStream, orders[0], orderFields, "orders"); // add a duplicate record

        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);
        appendToStream(adImpressionStream, adImpressions[0], adImpressionFields, "ads"); // add a duplicate record

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.StreamKind.STREAM)
                .from("orders", Duration.ofSeconds(1), true)
                .innerJoin("ads", Duration.ofSeconds(2), true, Cmp.equal("userId", "orders:userId"))
                .select("ads:id,orders:id,userId,ads:product,orders:product,price");

        MockTopologyContext context = new MockTopologyContext(bolt.getOutputFields());
        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, context, collector);

        for (Tuple tuple : adImpressionStream) {
            bolt.execute(tuple);
        }
        int i=0;
        for (Tuple tuple : orderStream) {
            ++i;
            bolt.execute(tuple);
            if(i==6)
                Thread.sleep(0);
        }
        Thread.sleep( Duration.ofSeconds(2).toMillis() );
        bolt.execute(makeTickTuple());

        printResults(collector);
        Assert.assertEquals( 4, collector.actualResults.size() );
    }

    // adds rec to streamRecs
    private static void appendToStream(ArrayList<Tuple> streamRecs, Object[] rec, String[] fieldNames, String streamName) {
        streamRecs.add( new TupleImpl(new MockTopologyContext(fieldNames), Arrays.asList(rec), 0, streamName) );
    }

    @Test
    public void testSingleKey_LeftJoin_CountRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.StreamKind.STREAM)
                .from("ads", 1, false)
                .leftJoin("orders", 12, false,  Cmp.equal("userId", "ads:userId"))
                .select("ads:id, orders:id , ads:userId , ads:product , orders:product , price");

        MockTopologyContext context = new MockTopologyContext(bolt.getOutputFields());
        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, context, collector);

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
    public void testSingleKey_LeftJoin_TimeRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.StreamKind.STREAM)
                .from("ads", Duration.ofSeconds(2), false)
                .leftJoin("orders", Duration.ofSeconds(2), false,  Cmp.equal("userId", "ads:userId"))
                .select("ads:id, orders:id , ads:userId , ads:product , orders:product , price");

        MockTopologyContext context = new MockTopologyContext(bolt.getOutputFields());
        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, context, collector);

        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }

        for (Tuple tuple : adImpressionStream) {
            bolt.execute(tuple);
        }
        Thread.sleep( Duration.ofSeconds(3).toMillis() );
        bolt.execute(makeTickTuple());

        printResults(collector);
        Assert.assertEquals( adImpressionStream.size(), collector.actualResults.size() );
    }

    @Test
    public void testSingleKey_RightJoin_TimeRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.StreamKind.STREAM)
                .from("ads", Duration.ofSeconds(1), false)
                .rightJoin("orders", Duration.ofSeconds(1), false, Cmp.equal("userId", "ads:userId"))
                .select(" orders:id, ads:userId, ads:product, orders:product, price ");

        MockTopologyContext context = new MockTopologyContext(bolt.getOutputFields());
        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, context, collector);

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
        Thread.sleep( Duration.ofSeconds(2).toMillis() );
        bolt.execute(makeTickTuple());

        printResults(collector);
        Assert.assertEquals( orderStream.size(), collector.actualResults.size() );
    }


    @Test
    public void testSingleKey_OuterJoin_TimeRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.StreamKind.STREAM)
                .from("ads", Duration.ofSeconds(1), false)
                .outerJoin("orders", Duration.ofSeconds(1), false, Cmp.equal("orders:userId", "ads:userId"))
                // extra spaces in arg to select() are to test FieldDescriptor
                .select(" ads:id, orders:id as orderId, ads:userId  as  userId ,ads:product, orders:product, price ");

        MockTopologyContext context = new MockTopologyContext(bolt.getOutputFields());
        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, context, collector);

        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }

        // emit all ads but last one
        for (Tuple tuple : adImpressionStream) {
            bolt.execute(tuple);
        }
        // sleep to allow expiration of all buffered orders and trigger an emit of unmatched orders on next execute()
        Thread.sleep( Duration.ofSeconds(2).toMillis() );
        bolt.execute(makeTickTuple());


        printResults(collector);
        Assert.assertEquals( adImpressionStream.size()+2, collector.actualResults.size() );
    }

    @Test
    public void testMultiKey_InnerJoin_CountRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(RealtimeJoinBolt.StreamKind.STREAM)
                .from("orders", 10, false)
                .innerJoin("ads", 10, false, Cmp.equal("orders:userId", "ads:userId")
                                           , Cmp.ignoreCase("ads:product","orders:product"))
                .select("orders:id,ads:userId,ads:product,orders:product,price");

        MockTopologyContext context = new MockTopologyContext(bolt.getOutputFields());
        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, context, collector);

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
    public void testMultiKey_InnerJoin_TimeRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStream("ads", adImpressionFields, adImpressions);

        RealtimeJoinBolt bolt = new RealtimeJoinBolt(StreamKind.STREAM)
                .from("orders", Duration.ofSeconds(2), false)
                .innerJoin("ads", Duration.ofSeconds(2), false, Cmp.equal("orders:userId", "ads:userId")
                                                              , Cmp.ignoreCase("ads:product","orders:product") )
                .select("orders:id,ads:userId,product,price");

        MockTopologyContext context = new MockTopologyContext(bolt.getOutputFields());
        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, context, collector);

        for (Tuple tuple : adImpressionStream) {
            bolt.execute(tuple);
        }
        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }
        Thread.sleep( Duration.ofSeconds(2).toMillis() );
        bolt.execute(makeTickTuple());

        printResults(collector);
        Assert.assertEquals( 3, collector.actualResults.size() );
    }

    @Test
    public void testStreamlineMultiKey_InnerJoin_TimeRetention() throws Exception {
        ArrayList<Tuple> orderStream = makeStreamLineEventStream("orders", orderFields, orders);
        ArrayList<Tuple> adImpressionStream = makeStreamLineEventStream("ads", adImpressionFields, adImpressions);

        SLRealtimeJoinBolt bolt = new SLRealtimeJoinBolt()
                .from("orders", new BaseWindowedBolt.Duration(2, TimeUnit.SECONDS), false)
                .innerJoin("ads", new BaseWindowedBolt.Duration(2, TimeUnit.SECONDS), false,  SLCmp.equal("orders:userId", "ads:userId")
                                                              , SLCmp.ignoreCase("ads:product","orders:product") )
                .select("orders:id,ads:userId,product,price");

        MockTopologyContext context = new MockTopologyContext(bolt.getOutputFields());
        MockCollector collector = new MockCollector(bolt.getOutputFields());
        bolt.prepare(null, context, collector);

        for (Tuple tuple : adImpressionStream) {
            bolt.execute(tuple);
        }
        for (Tuple tuple : orderStream) {
            bolt.execute(tuple);
        }
        Thread.sleep( Duration.ofSeconds(2).toMillis() );
        bolt.execute(makeTickTuple());

        printResults_StreamLine(collector);
        Assert.assertEquals( 3, collector.actualResults.size() );
    }


    private static ArrayList<Tuple> makeStream(String streamName, String[] fieldNames, Object[][] data) {
        ArrayList<Tuple> result = new ArrayList<>();
        MockTopologyContext mockContext = new MockTopologyContext(fieldNames);

        for (Object[] record : data) {
            TupleImpl rec = new TupleImpl(mockContext, Arrays.asList(record), 0, streamName);
            result.add( rec );
        }

        return result;
    }

    // NOTE: Streamline Specific
    private static ArrayList<Tuple> makeStreamLineEventStream (String streamName, String[] fieldNames, Object[][] records) {

        MockTopologyContext mockContext = new MockTopologyContext(new String[]{StreamlineEvent.STREAMLINE_EVENT} );
        ArrayList<Tuple> result = new ArrayList<>(records.length);

        // convert each record into a HashMap using fieldNames as keys
        for (Object[] record : records) {
            HashMap<String,Object> recordMap = new HashMap<>( fieldNames.length );
            for (int i = 0; i < fieldNames.length; i++) {
                recordMap.put(fieldNames[i], record[i]);
            }
            StreamlineEvent streamLineEvent = StreamlineEventImpl.builder()
                    .fieldsAndValues(recordMap)
                    .dataSourceId("multiple sources")
                    .build();
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

    static class MockTopologyContext extends TopologyContext {

        private final Fields fields;
        private String srcComponentId = "component";

        public MockTopologyContext(String[] fieldNames) {
            super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            this.fields = new Fields(fieldNames);
        }

        public MockTopologyContext(String[] fieldNames, String srcComponentId) {
            super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            this.fields = new Fields(fieldNames);
            this.srcComponentId = srcComponentId;
        }

        @Override
        public String getThisComponentId() {
            return srcComponentId;
        }

        @Override
        public String getComponentId(int taskId) {
            return srcComponentId;
        }

        public Fields getComponentOutputFields(String componentId, String streamId) {
            return fields;
        }
    }

    public Tuple makeTickTuple() {
        MockTopologyContext context = new MockTopologyContext(new String[]{StreamlineEvent.STREAMLINE_EVENT}, Constants.SYSTEM_COMPONENT_ID );

        return new TupleImpl(context, new Values(1000), (int) Constants.SYSTEM_TASK_ID, Constants.SYSTEM_TICK_STREAM_ID);
    }
}
