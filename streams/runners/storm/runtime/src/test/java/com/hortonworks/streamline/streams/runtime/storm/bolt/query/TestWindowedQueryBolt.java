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

import com.hortonworks.streamline.streams.layout.component.rule.expression.Window;
import org.apache.storm.task.GeneralTopologyContext;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.windowing.TupleWindow;
import org.apache.storm.windowing.TupleWindowImpl;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

public class TestWindowedQueryBolt {
    private static final String SL_PREFIX = StreamlineEvent.STREAMLINE_EVENT + ".";
    String[] userFields = {"userId", "name", "city"};
    Object[][] users = {
            {1, "roshan", "san jose" },
            {2, "harsha", "santa clara" },
            {3, "siva",   "dublin" },
            {4, "hugo",   "san mateo" },
            {5, "suresh", "sunnyvale" },
            {6, "guru",   "palo alto" },
            {7, "arun",   "bengaluru"},
            {8, "satish", "mumbai" },
            {9, "mani",   "bengaluru" },
            {10,"priyank","seattle" }
    };

    String [] cityFields = {"cityId","cityName","country"};
    Object[][] cities = {
            {1, "san jose", "US"},
            {2, "santa clara", "US"},
            {3, "dublin", "US" },
            {4, "san mateo", "US" },
            {5, "sunnyvale", "US" },
            {6, "palo alto", "US" },
            {7, "bengaluru", "India"},
            {8, "mumbai", "India"},
            {9, "chennai", "India"}
    };


    @Test
    public void testNestedKeys_trivial() throws Exception {
        ArrayList<Tuple> userStream = makeStreamLineEventStream("users", userFields, users);
        TupleWindow window = makeTupleWindow(userStream);
        WindowedQueryBolt bolt = new WindowedQueryBolt("users", SL_PREFIX + "userId")
                .selectStreamLine("name,users:city, users:city as cityagain");
        MockTopologyContext context = new MockTopologyContext(new String[]{StreamlineEvent.STREAMLINE_EVENT});
        MockCollector collector = new MockCollector();
        bolt.prepare(null, context, collector);
        bolt.execute(window);
        printResults_StreamLine(collector);
        Assert.assertEquals( userStream.size(), collector.actualResults.size() );
    }


    @Test
    public void testNestedKeys_StreamLine() throws Exception {
        ArrayList<Tuple> userStream = makeStreamLineEventStream("users", userFields, users);
        ArrayList<Tuple> cityStream = makeStreamLineEventStream("cities", cityFields, cities);
        TupleWindow window = makeTupleWindow(userStream, cityStream);
        WindowedQueryBolt bolt = new WindowedQueryBolt("users", "city")
                .join("cities", "cityName", "users")
                .selectStreamLine("name, users:city as city, cities:country");
        MockTopologyContext context = new MockTopologyContext(new String[]{StreamlineEvent.STREAMLINE_EVENT});
        MockCollector collector = new MockCollector();
        bolt.prepare(null, context, collector);
        bolt.execute(window);
        printResults_StreamLine(collector);
        Assert.assertEquals( cityStream.size(), collector.actualResults.size() );
    }

    @Test
    public void testTimeStampExtraction() throws Exception {
        ArrayList<Tuple> usersAndCities  = makeStreamLineEventStream("users", userFields, users);
        usersAndCities.addAll( makeStreamLineEventStream("cities", cityFields, cities) );

        // treating 'userId' & 'cityId' as timestamp fields
        SLMultistreamTimestampExtractor tsExtractor = new SLMultistreamTimestampExtractor(Arrays.asList("users:userId", "cities:cityId"));

        for (Tuple userOrCity : usersAndCities) {
            long l = tsExtractor.extractTimestamp( userOrCity );
            Assert.assertTrue( l > 0 && l <= 10 );
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testTimeStampExtraction_ValidationFailure() throws Exception {
        ArrayList<Tuple> usersAndCities  = makeStreamLineEventStream("users", userFields, users);
        usersAndCities.addAll( makeStreamLineEventStream("cities", cityFields, cities) );

        SLMultistreamTimestampExtractor tsExtractor = new SLMultistreamTimestampExtractor(Collections.singletonList("users:userId")); // missing info on city stream

        for (Tuple userOrCity : usersAndCities) {
            long l = tsExtractor.extractTimestamp( userOrCity ); // should throw IllegalArgumentException
            Assert.assertTrue( l > 0 && l <= 10 );
        }
    }


    @Test
    public void testJsonWindowConfig_EventTimeWindow() throws IOException {
        String windowConfigJson =
            "{\"windowLength\" : {\"class\":\".Window$Duration\", \"durationMs\":2000},\n" +
            " \"slidingInterval\":{\"class\":\".Window$Duration\", \"durationMs\":2000},\n" +
            " \"tsFields\": [\"s1:tsField1\" , \"s2:tsField2\", \"s3:field3.innerTsField\"],\n" +
            " \"lagMs\": 1000 ,\n" +
            " \"lateStream\": \"optionalLateStream\"}";

        WindowedQueryBolt bolt = new WindowedQueryBolt("users", "city")
            .join("cities", "cityName", "users")
            .selectStreamLine("name, users:city as city, cities:country");

        bolt.withWindowConfig( new Window(windowConfigJson));
    }

    @Test
    public void testJsonWindowConfig_CountedWindow() throws IOException {
        String windowConfigJson =
            "{\"windowLength\" : {\"class\":\".Window$Count\", \"count\":2000},\n" +
                " \"slidingInterval\":{\"class\":\".Window$Count\", \"count\":2000},\n" +
                " \"tsFields\": [\"s1:tsField1\" , \"s2:tsField2\", \"s3:field3.innerTsField\"]\n" +
             "}";

        WindowedQueryBolt bolt = new WindowedQueryBolt("users", "city")
            .join("cities", "cityName", "users")
            .selectStreamLine("name, users:city as city, cities:country");

        bolt.withWindowConfig( new Window(windowConfigJson));
    }


    private static void printResults_StreamLine(MockCollector collector) {
        int counter=0;
        for (List<Object> rec : collector.actualResults) {
            System.out.print(++counter +  ") ");
            for (Object field : rec) {
                Map<String, Object> data = ((StreamlineEvent)field);
                data.forEach((k,v) -> {
                    System.out.print(k + "=" + v + ", ");
                } );
                System.out.println();
            }
            System.out.println("");
        }
    }


    private static TupleWindow makeTupleWindow(ArrayList<Tuple> stream) {
        return new TupleWindowImpl(stream, null, null);
    }


    private static TupleWindow makeTupleWindow(ArrayList<Tuple>... streams) {
        ArrayList<Tuple> combined = null;
        for (int i = 0; i < streams.length; i++) {
            if(i==0) {
                combined = new ArrayList<>(streams[0]);
            } else {
                combined.addAll(streams[i]);
            }
        }
        return new TupleWindowImpl(combined, null, null);
    }



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

    static class MockCollector extends OutputCollector {
        public ArrayList<List<Object> > actualResults = new ArrayList<>();

        public MockCollector() {
            super(null);
        }

        @Override
        public List<Integer> emit(Collection<Tuple> anchors, List<Object> tuple) {
            actualResults.add(tuple);
            return null;
        }

        @Override
        public List<Integer> emit(String streamId, Collection<Tuple> anchors, List<Object> tuple) {
            return emit(anchors, tuple);
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

}
