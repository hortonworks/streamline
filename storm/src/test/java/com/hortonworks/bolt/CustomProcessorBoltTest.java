package com.hortonworks.bolt;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.processor.CustomProcessor;
import com.hortonworks.iotas.processor.ProcessingException;
import com.hortonworks.iotas.util.ReflectionHelper;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Tested;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(JMockit.class)
public class CustomProcessorBoltTest {

    private static final Values VALUES = new Values(MockParser.IOTAS_EVENT);
    private Schema inputSchema = new Schema.SchemaBuilder().field(new Schema.Field("A", Schema.Type.INTEGER)).build();
    private Schema outputSchema = new Schema.SchemaBuilder().field(new Schema.Field("A", Schema.Type.INTEGER)).build();
    private String outputStream = "stream";
    private Map<String, Schema> outputStreamToSchema = new HashMap<>();
    private final Fields OUTPUT_FIELDS = new Fields(IotasEvent.IOTAS_EVENT);
    private final String someString = "someString";

    private @Tested
    CustomProcessorBolt customProcessorBolt;
    private @Injectable
    OutputCollector mockOutputCollector;
    private @Injectable
    Tuple mockTuple;
    private @Injectable
    CustomProcessor customProcessor;
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
    public void testPrepare () {
        new MockUp<ReflectionHelper>() {
            @Mock
            <T> T newInstance(String className) {
                return (T) customProcessor;
            }
        };

        customProcessorBolt.customProcessorImpl(someString);
        final Map<String, Object> config = new HashMap<>();
        customProcessorBolt.config(config);
        customProcessorBolt.prepare(new HashMap(), null, null);
        new VerificationsInOrder(){{
            customProcessor.config(config);
            times = 1;
            customProcessor.initialize();
            times = 1;
        }};
    }

    @Test
    public void testExecute () throws ProcessingException {
        new MockUp<ReflectionHelper>() {
            @Mock
            <T> T newInstance(String className) {
                return (T) customProcessor;
            }
        };
        customProcessorBolt.customProcessorImpl(someString);
        customProcessorBolt.outputSchema(outputStreamToSchema);
        customProcessorBolt.inputSchema(inputSchema);
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        final IotasEvent iotasEvent = new IotasEventImpl(data, "dsrcid");
        final Result result = new Result(outputStream, Arrays.asList(iotasEvent));
        final List<Result> results = new ArrayList<>();
        results.add(result);
        new Expectations() {{
            tuple.getValueByField(IotasEvent.IOTAS_EVENT); returns(iotasEvent);
            customProcessor.process(iotasEvent); returns(results);
        }};
        customProcessorBolt.prepare(null, null, mockOutputCollector);
        customProcessorBolt.execute(tuple);
        new VerificationsInOrder(){{
            customProcessor.process(iotasEvent);
            times = 1;
            Values actualValues;
            mockOutputCollector.emit(outputStream, tuple, actualValues = withCapture());
            times = 1;
            mockOutputCollector.ack(tuple);
            times = 1;
        }};
    }

    @Test
    public void testExecuteWithProcessingException () throws ProcessingException {
        new MockUp<ReflectionHelper>() {
            @Mock
            <T> T newInstance(String className) {
                return (T) customProcessor;
            }
        };
        customProcessorBolt.customProcessorImpl(someString);
        customProcessorBolt.outputSchema(outputStreamToSchema);
        customProcessorBolt.inputSchema(inputSchema);
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        final IotasEvent iotasEvent = new IotasEventImpl(data, "dsrcid");
        final Result result = new Result(outputStream, Arrays.asList(iotasEvent));
        final List<Result> results = new ArrayList<>();
        final ProcessingException pe = new ProcessingException("Test");
        results.add(result);
        new Expectations() {{
            tuple.getValueByField(IotasEvent.IOTAS_EVENT); returns(iotasEvent);
            customProcessor.process(iotasEvent); result = pe;
        }};
        customProcessorBolt.prepare(null, null, mockOutputCollector);
        customProcessorBolt.execute(tuple);
        new VerificationsInOrder(){{
            customProcessor.process(iotasEvent);
            times = 1;
            mockOutputCollector.fail(tuple);
            times = 1;
            mockOutputCollector.reportError(pe);
        }};
    }
}
