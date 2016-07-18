package com.hortonworks.iotas.bolt.rules;

import com.google.common.collect.ImmutableMap;
import com.hortonworks.iotas.streams.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.streams.layout.component.RulesProcessorJsonBuilder;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Window;
import com.hortonworks.iotas.layout.runtime.rule.RulesBoltDependenciesFactory;
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit test for {@link WindowRulesBolt}
 */
@RunWith(JMockit.class)
public class WindowRulesBoltTest {

    @Mocked
    OutputCollector mockCollector;

    @Mocked
    TopologyContext mockContext;

    @Test
    public void testCountBasedWindow() throws Exception {
        Assert.assertTrue(doTest(readFile("/window-rule-count.json"), 2));
        new Verifications() {
            {
                String streamId;
                Collection<Tuple> anchors;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchors = withCapture(), withCapture(tuples));
                System.out.println(tuples);
                Map<String, Object> fieldsAndValues1 = ((IotasEvent) tuples.get(0).get(0)).getFieldsAndValues();
                Assert.assertEquals("min salary is 30, max salary is 100", fieldsAndValues1.get("body"));
                Map<String, Object> fieldsAndValues2 = ((IotasEvent) tuples.get(1).get(0)).getFieldsAndValues();
                Assert.assertEquals("min salary is 110, max salary is 200", fieldsAndValues2.get("body"));
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
                System.out.println(tuples);
                Map<String, Object> fieldsAndValues1 = ((IotasEvent) tuples.get(0).get(0)).getFieldsAndValues();
                Assert.assertEquals("count is 2, min salary is 30, max salary is 40", fieldsAndValues1.get("body"));
                Map<String, Object> fieldsAndValues2 = ((IotasEvent) tuples.get(1).get(0)).getFieldsAndValues();
                Assert.assertEquals("count is 5, min salary is 50, max salary is 90", fieldsAndValues2.get("body"));
                Map<String, Object> fieldsAndValues3 = ((IotasEvent) tuples.get(2).get(0)).getFieldsAndValues();
                Assert.assertEquals("count is 1, min salary is 100, max salary is 100", fieldsAndValues3.get("body"));
                Map<String, Object> fieldsAndValues4 = ((IotasEvent) tuples.get(3).get(0)).getFieldsAndValues();
                Assert.assertEquals("count is 4, min salary is 110, max salary is 140", fieldsAndValues4.get("body"));
                Map<String, Object> fieldsAndValues5 = ((IotasEvent) tuples.get(4).get(0)).getFieldsAndValues();
                Assert.assertEquals("count is 5, min salary is 150, max salary is 190", fieldsAndValues5.get("body"));
                Map<String, Object> fieldsAndValues6 = ((IotasEvent) tuples.get(5).get(0)).getFieldsAndValues();
                Assert.assertEquals("count is 1, min salary is 200, max salary is 200", fieldsAndValues6.get("body"));
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
                System.out.println(tuples);
                Map<String, Object> fieldsAndValues1 = ((IotasEvent) tuples.get(0).get(0)).getFieldsAndValues();
                Assert.assertEquals("min salary is 30, max salary is 200", fieldsAndValues1.get("body"));
            }
        };
    }

    private boolean doTest(String rulesJson, int expectedExecuteCount) throws Exception {
        new Expectations() {{
            mockContext.getComponentOutputFields(anyString, anyString);
            result = new Fields(IotasEvent.IOTAS_EVENT);
            mockContext.getComponentId(anyInt);
            result = "componentid";
        }};
        RulesBoltDependenciesFactory factory = new RulesBoltDependenciesFactory(
                new RulesProcessorJsonBuilder(rulesJson), RulesBoltDependenciesFactory.ScriptType.SQL);
        Window windowConfig = factory.createRuleProcessorRuntime().getRulesRuntime().get(0).getRule().getWindow();
        final CountDownLatch latch = new CountDownLatch(expectedExecuteCount);
        WindowRulesBolt wb = new WindowRulesBolt(factory) {
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
            wbe.execute(getNextTuple(i));
        }
        // wait for up to 5 secs for the bolt's execute to finish
        return latch.await(5, TimeUnit.SECONDS);
    }

    private String readFile(String fn) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(fn));
    }

    private Tuple getNextTuple(int i) {
        IotasEvent event = new IotasEventImpl(ImmutableMap.<String, Object>of("empid", i, "salary", i * 10, "deptid", i/5), "dsrcid");
        return new TupleImpl(mockContext, new Values(event), 1, "stream");
    }

}