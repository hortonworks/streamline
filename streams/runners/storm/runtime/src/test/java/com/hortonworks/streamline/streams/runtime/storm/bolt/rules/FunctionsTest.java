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
import java.util.List;

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
    public void testFunctions() throws Exception {
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
        StreamlineEvent event = new StreamlineEventImpl(ImmutableMap.of("intfield", 100, "stringfield1", "hello", "stringfield2", "world"), "dsrcid");
        return new TupleImpl(mockContext, new Values(event), 1, "inputstream");
    }
}
