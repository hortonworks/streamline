package com.hortonworks.streamline.streams.runtime.storm.bolt.rules;

import com.google.common.collect.ImmutableMap;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.runtime.processor.RuleProcessorRuntime;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.IOUtils;
import org.apache.storm.Config;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.tuple.Values;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit test to check the scalar functions that are shipped with streamline
 */
@RunWith(JMockit.class)
public class FunctionsTest {
    @Mocked
    OutputCollector mockCollector;

    @Mocked
    TopologyContext mockContext;

    @Before
    public void setUp() {
        new Expectations() {{
            mockContext.getComponentOutputFields(anyString, anyString);
            result = new Fields(StreamlineEvent.STREAMLINE_EVENT);
            mockContext.getComponentId(anyInt);
            result = "componentid";
        }};
    }

    @Test
    public void testStringFunctions1() throws Exception {
        doTest(readFile("/streamline-udf.json"), getTuple());
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                Assert.assertEquals(1, tuples.size());
                Assert.assertEquals("CONCAT helloworld IDENTITY hello UPPER HELLO LOWER hello INITCAP Hello CHAR_LENGTH 5",
                        ((StreamlineEvent)(tuples.get(0).get(0))).get("body"));
            }
        };
    }

    @Test
    public void testStringFunctions2() throws Exception {
        doTest(readFile("/streamline-udf2.json"), getTuple());
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                Assert.assertEquals(1, tuples.size());
                Assert.assertEquals("SUBSTRING ello SUBSTRING2 hell POSITION 4 POSITION2 4 TRIM space LTRIM space  RTRIM  space OVERLAY abba OVERLAY2 abbaa",
                        ((StreamlineEvent)(tuples.get(0).get(0))).get("body"));
            }
        };
    }

    @Test
    public void testStringFunctions3() throws Exception {
        doTest(readFile("/streamline-udf3.json"), getTuple());
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                Assert.assertEquals(1, tuples.size());
                Assert.assertEquals("TRIM2 space LTRIM2 space  RTRIM2  space",
                        ((StreamlineEvent)(tuples.get(0).get(0))).get("body"));
            }
        };
    }

    @Test
    public void testMathFunctions1() throws Exception {
        doTest(readFile("/streamline-udf-math1.json"), getTuple());
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                Assert.assertEquals(1, tuples.size());
                Assert.assertEquals("POWER 1024.0 ABS 1.0 MOD 0 SQRT 1.4142135623730951 LN 0.6931471805599453" +
                                " LOG10 0.3010299956639812 EXP 7.38905609893065 CEIL 2.0 FLOOR 1.0",
                        ((StreamlineEvent)(tuples.get(0).get(0))).get("body"));
            }
        };
    }

    @Test
    public void testMathFunctions2() throws Exception {
        doTest(readFile("/streamline-udf-math2.json"), getTuple());
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                Assert.assertEquals(1, tuples.size());
                String res[] = (((StreamlineEvent)(tuples.get(0).get(0))).get("body")).toString().split(" ");
                Assert.assertEquals("RAND", res[0]);
                Double rand = Double.valueOf(res[1]);
                Assert.assertTrue(rand >= 0.0 && rand <= 1.0);
                Assert.assertEquals("RAND_INTEGER", res[2]);
                Integer rand_int = Integer.valueOf(res[3]);
                Assert.assertTrue(rand_int >= 0 && rand_int <= 100);
            }
        };
    }

    private void doTest(String rulesJson, Tuple tuple) throws Exception {
        RulesBolt rulesBolt = new RulesBolt(rulesJson, RuleProcessorRuntime.ScriptType.SQL) {
            @Override
            public void execute(Tuple input) {
                super.execute(input);
            }
        };
        rulesBolt.prepare(new Config(), mockContext, mockCollector);
        rulesBolt.execute(tuple);
    }

    private String readFile(String fn) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(fn));
    }

    private Tuple getTuple() {
        Map<String, Object> map = new HashMap<>();
        map.put("intfield", 2);
        map.put("stringfield1", "hello");
        map.put("stringfield2", "world");
        map.put("stringfield3", " space ");
        map.put("stringfield4", "aaaa");
        map.put("negativefield", -1.0);
        map.put("doublefield", 1.41);
        StreamlineEvent event = new StreamlineEventImpl(map, "dsrcid");
        return new TupleImpl(mockContext, new Values(event), 1, "inputstream");
    }

}
