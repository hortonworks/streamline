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
package com.hortonworks.streamline.streams.runtime.storm.bolt.rules;

import com.google.common.collect.ImmutableMap;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
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
import com.hortonworks.streamline.streams.runtime.processor.RuleProcessorRuntime;
import org.junit.Assert;
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
            result = "1-componentid";
            mockContext.getThisComponentId();
            result = "1-componentid"; minTimes = 0;
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
        StreamlineEvent event = StreamlineEventImpl.builder()
                .fieldsAndValues(ImmutableMap.<String, Object>of("foo", "Normal", "bar", "abc", "baz", 200))
                .dataSourceId("dsrcid")
                .build();
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

    @Test
    public void testComplex1() throws Exception {
        doTest(readFile("/streamline-complex-condition.json"), getWeather("SFO", 0, 0));
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                Assert.assertEquals("outputstream", streamId);
                Assert.assertEquals("CITY SFO TEMPERATURE 0 HUMIDITY 0", ((StreamlineEvent)tuples.get(0).get(0)).get("body"));
            }
        };
    }

    @Test
    public void testComplex2() throws Exception {
        doTest(readFile("/streamline-complex-condition.json"), getWeather("FOO", 50, 60));
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                Assert.assertEquals("outputstream", streamId);
                Assert.assertEquals("CITY FOO TEMPERATURE 50 HUMIDITY 60", ((StreamlineEvent)tuples.get(0).get(0)).get("body"));
            }
        };
    }

    @Test
    public void testComplex3() throws Exception {
        doTest(readFile("/streamline-complex-condition.json"), getWeather("FOO", 10, 10));
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

    private Tuple getTuple(int i) {
        StreamlineEvent event = StreamlineEventImpl.builder()
                .fieldsAndValues(ImmutableMap.of("foo", i, "bar", 100, "baz", 200))
                .dataSourceId("dsrcid")
                .build();
        return new TupleImpl(mockContext, new Values(event), 1, "inputstream");
    }

    private Tuple getWeather(String city, long temperature, long humidity) {
        StreamlineEvent event = StreamlineEventImpl.builder()
                .fieldsAndValues(ImmutableMap.of("city", city,
                        "temperature", temperature,
                        "humidity", humidity))
                .dataSourceId("dsrcid")
                .build();
        return new TupleImpl(mockContext, new Values(event), 1, "inputstream");
    }

}