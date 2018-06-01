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
package com.hortonworks.streamline.streams.runtime.storm.bolt;

import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.runtime.CustomProcessorRuntime;
import com.hortonworks.streamline.streams.runtime.SimpleCustomProcessorRuntime;
import mockit.Mocked;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.exception.ProcessingException;
import com.hortonworks.streamline.examples.processors.ConsoleCustomProcessor;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class CustomProcessorBoltTest {
    private Schema outputSchema = schema("A");
    private String outputStream = "stream";
    private Map<String, Schema> outputStreamToSchema = new HashMap<>();
    private final Fields OUTPUT_FIELDS = new Fields(StreamlineEvent.STREAMLINE_EVENT);
    private final String someString = "someString";
    private final String stream = "stream";

    private @Tested
    CustomProcessorBolt customProcessorBolt;
    private @Injectable
    OutputCollector mockOutputCollector;
    private @Mocked
    ConsoleCustomProcessor customProcessorRuntime;
    private @Injectable
    OutputFieldsDeclarer mockOutputDeclarer;
    private @Injectable
    Tuple tuple;
    private @Injectable
    TopologyContext mockTopologyContext;


    @Before
    public void setup() throws Exception {
        outputStreamToSchema.put(outputStream, outputSchema);
        customProcessorBolt = new CustomProcessorBolt();
        //customProcessorBolt.inputSchema(inputSchema);
        //customProcessorBolt.outputSchema(outputStreamToSchema);
    }

    @Test(expected = RuntimeException.class)
    public void testDeclareOutputFieldsWithoutOutputSchema () {
        customProcessorBolt.declareOutputFields(mockOutputDeclarer);
    }

    @Test
    public void testDeclareOutputFields () {
        customProcessorBolt.outputSchema(outputStreamToSchema);
        customProcessorBolt.declareOutputFields(mockOutputDeclarer);
        new VerificationsInOrder(){{
            mockOutputDeclarer.declareStream(outputStream, withAny(OUTPUT_FIELDS));
            times = 1;
        }};
    }

    @Test(expected = RuntimeException.class)
    public void testNoCustomImplPrepare () throws Exception {
        customProcessorBolt.prepare(new HashMap(), null, null);
   }

    @Test(expected = RuntimeException.class)
    public void testInvalidCustomImplPrepare () throws Exception {
        customProcessorBolt.customProcessorImpl(someString);
        customProcessorBolt.prepare(new HashMap(), null, null);
    }

    @Test
    public void testExecuteNull() throws ProcessingException {
        customProcessorBolt.outputSchema(map(outputStream, schema("A", "B")));
        customProcessorBolt.inputSchemaMap(map("stream", map("A", "longfieldname")));
        customProcessorBolt.customProcessorImpl(TestCP.class.getName());
        customProcessorBolt.prepare(Collections.emptyMap(), mockTopologyContext, mockOutputCollector);
        StreamlineEvent event = StreamlineEventImpl.builder()
                .fieldsAndValues(map("longfieldname", "val"))
                .dataSourceId("dsrcid")
                .build();
        new Expectations() {{
            tuple.getSourceComponent();
            returns("datasource");
            tuple.getSourceStreamId();
            returns("stream");
            tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
            returns(event);
            mockTopologyContext.getThisComponentId();
            returns("foo-bar");
        }};
        customProcessorBolt.execute(tuple);
        new Verifications() {{
            Values actualValues;
            mockOutputCollector.emit(outputStream, tuple, actualValues = withCapture());
            times = 1;
            assertEquals(1, actualValues.size());
            assertTrue(actualValues.get(0) instanceof StreamlineEvent);
            StreamlineEvent output = (StreamlineEvent) actualValues.get(0);
            assertEquals(1, output.size());
            assertEquals("val", output.get("A"));
        }};

    }

    @Test
    public void testPrepare () throws ClassNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException {
        customProcessorBolt.customProcessorImpl(ConsoleCustomProcessor.class.getCanonicalName());
        final Map<String, Object> config = new HashMap<>();
        customProcessorBolt.config(config);
        new Expectations() {{
            customProcessorRuntime.initialize(withEqual(config));
            minTimes=1; maxTimes=1;
        }};
        Map<Object, Object> conf = new HashMap<>();
        customProcessorBolt.prepare(conf, null, null);
        new VerificationsInOrder(){{
            customProcessorRuntime.initialize(config);
            times = 1;
        }};
    }

    //ignoring for now after the changes introduced in PR related to ISSUE-710
    @Test
    @Ignore
    public void testExecuteSuccess () throws ProcessingException, ClassNotFoundException, MalformedURLException, InstantiationException,
            IllegalAccessException {
        testExecute(true);
    }

    //ignoring for now after the changes introduced in PR related to ISSUE-710
    @Test
    @Ignore
    public void testExecuteWithProcessingException () throws ProcessingException, ClassNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException {
        testExecute(false);
    }

    private static Schema schema(String... fields) {
        Schema.SchemaBuilder sb = new Schema.SchemaBuilder();
        return sb.fields(Arrays.stream(fields)
                .map(f -> new Schema.Field(f, Schema.Type.INTEGER))
                .collect(Collectors.toList()))
                .build();
    }

    public static class TestCP extends SimpleCustomProcessorRuntime {
        @Override
        public List<StreamlineEvent> process(StreamlineEvent event) throws ProcessingException {
            return Collections.singletonList(event);
        }
    }

    private <K, V> Map<K, V> map(K key, V val) {
        return Collections.singletonMap(key, val);
    }

    private void testExecute (boolean isSuccess) throws ProcessingException, ClassNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException {
                customProcessorBolt.customProcessorImpl(ConsoleCustomProcessor.class.getCanonicalName());
        customProcessorBolt.outputSchema(outputStreamToSchema);
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        final StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(data).dataSourceId("dsrcid").build();
        final List<StreamlineEvent> result =  Arrays.asList(event);
        final ProcessingException pe = new ProcessingException("Test");
        new Expectations() {{
            tuple.getSourceComponent();
            returns("datasource");
            tuple.getSourceStreamId();
            returns(stream);
            tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
            returns(event);
        }};
        if (!isSuccess) {
            new Expectations() {{
                customProcessorRuntime.process(event); result = pe;
            }};
        } else {
            new Expectations() {{
                customProcessorRuntime.process(event);
                returns(result);
            }};
        }
        Map<Object, Object> conf = new HashMap<>();
        customProcessorBolt.prepare(conf, null, mockOutputCollector);
        customProcessorBolt.execute(tuple);
        if (!isSuccess) {
            new VerificationsInOrder(){{
                RuntimeException e;
                customProcessorRuntime.process(event);
                times = 1;
                mockOutputCollector.fail(tuple);
                times = 1;
                mockOutputCollector.reportError(e = withCapture());
                assertTrue(e.getCause() == pe);
            }};

        } else {
            new VerificationsInOrder() {{
                tuple.getSourceComponent();
                times = 1;
                tuple.getSourceStreamId();
                times = 1;
                StreamlineEvent actual;
                customProcessorRuntime.process(actual = withCapture());
                times = 1;
                Assert.assertEquals(actual.getSourceStream(), stream);
                Assert.assertEquals(actual, event);
                Values actualValues;
                mockOutputCollector.emit(outputStream, tuple, actualValues = withCapture());
                times = 1;
                mockOutputCollector.ack(tuple);
                times = 1;
            }};
        }
    }
}
