package com.hortonworks.bolt.n11n;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.NormalizationProcessor;
import com.hortonworks.iotas.layout.design.n11n.Normalizer;
import com.hortonworks.iotas.layout.design.n11n.Transformer;
import com.hortonworks.iotas.layout.design.n11n.ValueGenerator;
import com.hortonworks.iotas.layout.runtime.n11n.NormalizationException;
import com.hortonworks.iotas.layout.runtime.n11n.NormalizationProcessorRuntime;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 */
@RunWith(JMockit.class)
public class NormalizationBoltTest {

    @Injectable
    private TopologyContext topologyContext;

    @Injectable
    private OutputCollector outputCollector;

    @Injectable
    private OutputFieldsDeclarer outputFieldsDeclarer;

    @Injectable
    private Tuple tuple;

    public static final IotasEventImpl INPUT_IOTAS_EVENT = new IotasEventImpl(new HashMap<String, Object>() {{
        put("illuminance", 70);
        put("temp", 104);
        put("foo", 100);
        put("humidity", "40h");
    }}, "ds-" + System.currentTimeMillis(), "id-"+System.currentTimeMillis() );

    public static final IotasEventImpl INVALID_INPUT_IOTAS_EVENT = new IotasEventImpl(new HashMap<String, Object>() {{
        put("illuminance", 70);
        put("tmprtr", 104);
        put("foo", 100);
        put("humidity", "40");
    }}, "ds-" + System.currentTimeMillis(), "id-"+System.currentTimeMillis() );

    private static final List<Schema.Field> OUTPUT_SCHEMA = Arrays.asList(new Schema.Field("temperature", Schema.Type.INTEGER),
            new Schema.Field("humidity", Schema.Type.STRING), new Schema.Field("illuminance", Schema.Type.INTEGER),
            new Schema.Field("new-field", Schema.Type.STRING));

    private static final IotasEvent VALID_OUTPUT_IOTAS_EVENT =
            new IotasEventImpl(new HashMap<String, Object>() {{
                put("temperature", 104);
                put("humidity", "40h");
                put("illuminance", 70);
                put("new-field", "new value");
            }}, INPUT_IOTAS_EVENT.getDataSourceId(), INPUT_IOTAS_EVENT.getId());

    @Before
    public void setup() {

    }

    @Test
    public void testNormalization() throws NormalizationException {
        NormalizationBolt normalizationBolt = buildNormalizationBolt();

        new Expectations() {{
            tuple.getValueByField(IotasEvent.IOTAS_EVENT);
            returns(INPUT_IOTAS_EVENT);
        }};

        normalizationBolt.execute(tuple);


        new Verifications(){{
            outputCollector.emit(tuple.getSourceStreamId(), tuple, new Values(VALID_OUTPUT_IOTAS_EVENT)); times=1;
            outputCollector.ack(tuple);
        }};

    }

    @Test
    public void testNormalizationWithFailure() throws NormalizationException {
        NormalizationBolt normalizationBolt = buildNormalizationBolt();

        new Expectations() {{
            tuple.getValueByField(IotasEvent.IOTAS_EVENT);
            returns(INVALID_INPUT_IOTAS_EVENT);
        }};

        normalizationBolt.execute(tuple);

        new Verifications(){{
            outputCollector.emit(withAny(""), withAny(tuple), withAny(new Values())); times=0;
            outputCollector.fail(tuple); times=1;
            outputCollector.reportError(withAny(new IllegalArgumentException())); times=1;
        }};

    }

    private NormalizationBolt buildNormalizationBolt() throws NormalizationException {
        NormalizationProcessorRuntime normalizationProcessorRuntime = buildNormalizationProcessorRuntime();
        NormalizationBolt normalizationBolt = new NormalizationBolt(normalizationProcessorRuntime);

        normalizationBolt.prepare(null, topologyContext, outputCollector);
        normalizationBolt.declareOutputFields(outputFieldsDeclarer);
        return normalizationBolt;
    }

    private NormalizationProcessorRuntime buildNormalizationProcessorRuntime() throws NormalizationException {
        List<Transformer> transformers = Collections.singletonList(new Transformer(new Schema.Field("temp", Schema.Type.INTEGER), new Schema.Field("temperature", Schema.Type.INTEGER), null));
        List<String> filters = Collections.singletonList("foo");
        List<ValueGenerator> valueGenerators = Collections.singletonList(new ValueGenerator(new Schema.Field("new-field", Schema.Type.STRING), null, "new value"));
        Normalizer normalizer = new Normalizer(transformers, filters, valueGenerators);
        NormalizationProcessor normalizationProcessor = new NormalizationProcessor(normalizer);
        normalizationProcessor.setDeclaredOutput(OUTPUT_SCHEMA);

        return new NormalizationProcessorRuntime.Builder(normalizationProcessor).build();
    }
}
