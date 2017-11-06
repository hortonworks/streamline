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
import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Window;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.IOUtils;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.WindowedBoltExecutor;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;
import com.hortonworks.streamline.streams.runtime.processor.RuleProcessorRuntime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Unit test for {@link WindowRulesBolt}
 */
@RunWith(JMockit.class)
public class WindowRulesBoltTest {

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
            mockContext.getThisComponentId();
            result = "componentid"; minTimes = 0;
        }};
    }

    @Test
    public void testAggFunctions() throws Exception {
        Assert.assertTrue(doTest(readFile("/streamline-udaf.json"), 1));
        new Verifications() {
            {
                String streamId;
                Collection<Tuple> anchors;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchors = withCapture(), withCapture(tuples));
                Assert.assertEquals("outputstream", streamId);
                Map<String, Object> fieldsAndValues1 = ((StreamlineEvent) tuples.get(0).get(0));
                Assert.assertEquals("STDDEV 59.16079783099616 STDDEVP 57.66281297335398 VARIANCE 3500.0 VARIANCEP 3325.0 " +
                        "MEAN 105.0 NUMBERSUM 2100 LONGCOUNT 20 MIN 10 MAX 200", fieldsAndValues1.get("body"));
            }
        };
    }

    @Test
    public void testCountBasedWindow() throws Exception {
        Assert.assertTrue(doTest(readFile("/window-rule-count.json"), 2));
        new Verifications() {
            {
                String streamId;
                Collection<Tuple> anchors;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchors = withCapture(), withCapture(tuples));
                Assert.assertEquals("outputstream", streamId);
                Map<String, Object> fieldsAndValues1 = ((StreamlineEvent) tuples.get(0).get(0));
                Assert.assertEquals("min salary is 30, max salary is 100", fieldsAndValues1.get("body"));
                Map<String, Object> fieldsAndValues2 = ((StreamlineEvent) tuples.get(1).get(0));
                Assert.assertEquals("min salary is 110, max salary is 200", fieldsAndValues2.get("body"));
            }
        };
    }

    @Test
    public void testCountBasedWindowFilterAll() throws Exception {
        Assert.assertTrue(doTest(readFile("/window-rule-groupby-filter.json"), 1));
        new Verifications() {
            {
                String streamId;
                Collection<Tuple> anchors;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchors = withCapture(), withCapture(tuples));
                times=0;
            }
        };
    }

    @Test
    public void testCountBasedWindowWithGroupby() throws Exception {
        Assert.assertTrue(doTest(readFile("/window-rule-count-withgroupby.json"), 2));
        new Verifications() {
            {
                String streamId;
                Collection<Tuple> anchors;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchors = withCapture(), withCapture(tuples));
                Map<String, Object> fieldsAndValues1 = ((StreamlineEvent) tuples.get(0).get(0));
                Assert.assertEquals("count is 2, min salary is 30, max salary is 40", fieldsAndValues1.get("body"));
                Map<String, Object> fieldsAndValues2 = ((StreamlineEvent) tuples.get(1).get(0));
                Assert.assertEquals("count is 5, min salary is 50, max salary is 90", fieldsAndValues2.get("body"));
                Map<String, Object> fieldsAndValues3 = ((StreamlineEvent) tuples.get(2).get(0));
                Assert.assertEquals("count is 1, min salary is 100, max salary is 100", fieldsAndValues3.get("body"));
                Map<String, Object> fieldsAndValues4 = ((StreamlineEvent) tuples.get(3).get(0));
                Assert.assertEquals("count is 4, min salary is 110, max salary is 140", fieldsAndValues4.get("body"));
                Map<String, Object> fieldsAndValues5 = ((StreamlineEvent) tuples.get(4).get(0));
                Assert.assertEquals("count is 5, min salary is 150, max salary is 190", fieldsAndValues5.get("body"));
                Map<String, Object> fieldsAndValues6 = ((StreamlineEvent) tuples.get(5).get(0));
                Assert.assertEquals("count is 1, min salary is 200, max salary is 200", fieldsAndValues6.get("body"));
            }
        };
    }

    @Test
    public void testCountBasedWindowWithGroupbyUnordered() throws Exception {
        String rulesJson = readFile("/window-rule-groupby-unordered.json");
        RulesProcessor rulesProcessor = Utils.createObjectFromJson(rulesJson, RulesProcessor.class);
        Window windowConfig = rulesProcessor.getRules().get(0).getWindow();
        WindowRulesBolt wb = new WindowRulesBolt(rulesJson, RuleProcessorRuntime.ScriptType.SQL);
        wb.withWindowConfig(windowConfig);
        WindowedBoltExecutor wbe = new WindowedBoltExecutor(wb);
        Map<String, Object> conf = wb.getComponentConfiguration();
        wbe.prepare(conf, mockContext, mockCollector);
        wbe.execute(getNextTuple(10));
        wbe.execute(getNextTuple(15));
        wbe.execute(getNextTuple(11));
        wbe.execute(getNextTuple(16));
        new Verifications() {
            {
                String streamId;
                Collection<Tuple> anchors;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchors = withCapture(), withCapture(tuples));
                Assert.assertEquals(2, tuples.size());
                Map<String, Object> fieldsAndValues = ((StreamlineEvent) tuples.get(0).get(0));
                Assert.assertEquals(2, fieldsAndValues.get("deptid"));
                Assert.assertEquals(110, fieldsAndValues.get("salary_MAX"));
                fieldsAndValues = ((StreamlineEvent) tuples.get(1).get(0));
                Assert.assertEquals(3, fieldsAndValues.get("deptid"));
                Assert.assertEquals(160, fieldsAndValues.get("salary_MAX"));
            }
        };
    }

    @Test
    public void testTimeBasedWindow() throws Exception {
        Assert.assertTrue(doTest(readFile("/window-rule-time.json"), 1));
        new Verifications() {
            {
                String streamId;
                Collection<Tuple> anchors;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchors = withCapture(), withCapture(tuples));
                Map<String, Object> fieldsAndValues1 = ((StreamlineEvent) tuples.get(0).get(0));
                Assert.assertEquals("min salary is 30, max salary is 200", fieldsAndValues1.get("body"));
            }
        };
    }

    @Test
    public void testTimeBasedWindowEmptyCondition() throws Exception {
        Assert.assertTrue(doTest(readFile("/window-rule-empty-condition.json"), 1));
        new Verifications() {
            {
                String streamId;
                Collection<Tuple> anchors;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchors = withCapture(), withCapture(tuples));
                Assert.assertEquals("outputstream", streamId);
                Map<String, Object> fieldsAndValues1 = ((StreamlineEvent) tuples.get(0).get(0));
                Assert.assertEquals(0, fieldsAndValues1.get("deptid"));
                Assert.assertEquals(40, fieldsAndValues1.get("salary_MAX"));
            }
        };
    }

    @Test
    public void testSum() throws Exception {
        Assert.assertTrue(doTest(readFile("/window-rule-sum.json"), 1, this::getTupleForSum));
        new Verifications() {
            {
                String streamId;
                Collection<Tuple> anchors;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchors = withCapture(), withCapture(tuples));
                Assert.assertEquals("outputstream", streamId);
                Map<String, Object> fieldsAndValues1 = ((StreamlineEvent) tuples.get(0).get(0));
                Assert.assertEquals("longSum is 55, doubleSum is 55.0, intSum is 55", fieldsAndValues1.get("body"));
                Map<String, Object> fieldsAndValues2 = ((StreamlineEvent) tuples.get(1).get(0));
                Assert.assertEquals("longSum is 155, doubleSum is 155.0, intSum is 155", fieldsAndValues2.get("body"));
            }
        };
    }

    private boolean doTest(String rulesJson, int expectedExecuteCount) throws Exception {
        return doTest(rulesJson, expectedExecuteCount, this::getNextTuple);
    }

    private boolean doTest(String rulesJson, int expectedExecuteCount, Function<Integer, Tuple> tupleGen) throws Exception {
        RulesProcessor rulesProcessor = Utils.createObjectFromJson(rulesJson, RulesProcessor.class);
        Window windowConfig = rulesProcessor.getRules().get(0).getWindow();
        final CountDownLatch latch = new CountDownLatch(expectedExecuteCount);
        WindowRulesBolt wb = new WindowRulesBolt(rulesJson, RuleProcessorRuntime.ScriptType.SQL) {
            @Override
            public void execute(TupleWindow inputWindow) {
                super.execute(inputWindow);
                latch.countDown();
            }
        };
        wb.withWindowConfig(windowConfig);
        WindowedBoltExecutor wbe = new WindowedBoltExecutor(wb);
        Map<String, Object> conf = wb.getComponentConfiguration();
        conf.put("topology.message.timeout.secs", 30);
        wbe.prepare(conf, mockContext, mockCollector);
        Thread.sleep(100);
        for (int i = 1; i <= 20; i++) {
            wbe.execute(tupleGen.apply(i));
        }
        // wait for up to 5 secs for the bolt's execute to finish
        return latch.await(5, TimeUnit.SECONDS);
    }

    private String readFile(String fn) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(fn));
    }

    private Tuple getNextTuple(int i) {
        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(
                ImmutableMap.of("empid", i, "salary", i * 10, "deptid", i/5)
        ).dataSourceId("dsrcid").build();
        return new TupleImpl(mockContext, new Values(event), 1, "inputstream");
    }

    private Tuple getTupleForSum(int i) {
        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(
                ImmutableMap.of("id", 1, "intField", i, "longField", (long) i, "doubleField", (double)i)
        ).dataSourceId("dsrcid").build();
        return new TupleImpl(mockContext, new Values(event), 1, "inputstream");
    }
}