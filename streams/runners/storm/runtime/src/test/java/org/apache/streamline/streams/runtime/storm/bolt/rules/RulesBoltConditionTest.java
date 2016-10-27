package org.apache.streamline.streams.runtime.storm.bolt.rules;

import com.google.common.collect.ImmutableMap;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.common.StreamlineEventImpl;
import org.apache.streamline.streams.layout.component.RulesProcessorJsonBuilder;
import org.apache.streamline.streams.runtime.rule.RulesDependenciesFactory;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for {@link RulesBolt}
 */
@RunWith(JMockit.class)
public class RulesBoltConditionTest {

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
    public void testSimpleCondition() throws Exception {
        doTest(readFile("/simple-rule.json"), getTuple(20));
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                System.out.println(streamId);
                System.out.println(anchor);
                System.out.println(tuples);
            }
        };
    }

    @Test
    public void testPassThrough() throws Exception {
        doTest(readFile("/passthrough-rule.json"), getTuple(20));
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                System.out.println(streamId);
                System.out.println(anchor);
                System.out.println(tuples);
            }
        };
    }

    @Test
    public void testSimpleConditionNoMatch() throws Exception {
        doTest(readFile("/simple-rule.json"), getTuple(5));
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                times=0;
            }
        };
    }

    @Test
    public void testSimpleRuleStringLiteral() throws Exception {
        StreamlineEvent event = new StreamlineEventImpl(ImmutableMap.<String, Object>of("foo", "Normal", "bar", "abc", "baz", 200), "dsrcid");
        Tuple tuple = new TupleImpl(mockContext, new Values(event), 1, "inputstream");

        doTest(readFile("/simple-rule-string-literal.json"), tuple);
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                times=0;
            }
        };
    }

    @Test
    public void testSelectFields() throws Exception {
        doTest(readFile("/simple-rule-select.json"), getTuple(20));
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                System.out.println(streamId);
                System.out.println(anchor);
                System.out.println(tuples);
            }
        };
    }

    @Test
    public void testProjectNoCondition() throws Exception {
        doTest(readFile("/simple-rule-project.json"), getTuple(20));
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                System.out.println(streamId);
                System.out.println(anchor);
                System.out.println(tuples);
            }
        };
    }

    private void doTest(String rulesJson, Tuple tuple) throws Exception {
        RulesDependenciesFactory factory = new RulesDependenciesFactory(
                new RulesProcessorJsonBuilder(rulesJson), RulesDependenciesFactory.ScriptType.SQL);
        RulesBolt rulesBolt = new RulesBolt(factory) {
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

    private Tuple getTuple(int i) {
        StreamlineEvent event = new StreamlineEventImpl(ImmutableMap.<String, Object>of("foo", i, "bar", 100, "baz", 200), "dsrcid");
        return new TupleImpl(mockContext, new Values(event), 1, "inputstream");
    }

}