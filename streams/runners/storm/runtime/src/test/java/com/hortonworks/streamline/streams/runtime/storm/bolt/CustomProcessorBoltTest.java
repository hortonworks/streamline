package com.hortonworks.streamline.streams.runtime.storm.bolt;

import mockit.Mocked;
import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.exception.ProcessingException;
import com.hortonworks.streamline.examples.processors.ConsoleCustomProcessor;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(JMockit.class)
public class CustomProcessorBoltTest {
    private Schema inputSchema = new Schema.SchemaBuilder().field(new Schema.Field("A", Schema.Type.INTEGER)).build();
    private Schema outputSchema = new Schema.SchemaBuilder().field(new Schema.Field("A", Schema.Type.INTEGER)).build();
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
    public void testPrepare () throws ClassNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException {
        customProcessorBolt.customProcessorImpl(ConsoleCustomProcessor.class.getCanonicalName());
        final Map<String, Object> config = new HashMap<>();
        customProcessorBolt.config(config);
        new Expectations() {{
            customProcessorRuntime.initialize(withEqual(config));
            minTimes=1; maxTimes=1;
        }};
        Map conf = new HashMap<>();
        customProcessorBolt.prepare(conf, null, null);
        new VerificationsInOrder(){{
            customProcessorRuntime.initialize(config);
            times = 1;
        }};
    }

    @Test
    public void testExecuteSuccess () throws ProcessingException, ClassNotFoundException, MalformedURLException, InstantiationException,
            IllegalAccessException {
        testExecute(true);
    }

    @Test
    public void testExecuteWithProcessingException () throws ProcessingException, ClassNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException {
        testExecute(false);
    }

    private void testExecute (boolean isSuccess) throws ProcessingException, ClassNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException {
                customProcessorBolt.customProcessorImpl(ConsoleCustomProcessor.class.getCanonicalName());
        customProcessorBolt.outputSchema(outputStreamToSchema);
        customProcessorBolt.inputSchema(inputSchema);
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        final StreamlineEvent event = new StreamlineEventImpl(data, "dsrcid");
        final Result result = new Result(outputStream, Arrays.asList(event));
        final List<Result> results = new ArrayList<>();
        results.add(result);
        final ProcessingException pe = new ProcessingException("Test");
        new Expectations() {{
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
                returns(results);
            }};
        }
        Map conf = new HashMap<>();
        customProcessorBolt.prepare(conf, null, mockOutputCollector);
        customProcessorBolt.execute(tuple);
        if (!isSuccess) {
            new VerificationsInOrder(){{
                customProcessorRuntime.process(event);
                times = 1;
                mockOutputCollector.fail(tuple);
                times = 1;
                mockOutputCollector.reportError(pe);
            }};
        } else {
            new VerificationsInOrder() {{
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
