package com.hortonworks.iotas.bolt.rules;

import com.google.common.collect.ImmutableMap;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.layout.design.component.RulesProcessorJsonBuilder;
import com.hortonworks.iotas.layout.design.rule.condition.Window;
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        doTest(readFile("/window-rule-count.json"), 0);
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
    public void testTimeBasedWindow() throws Exception {
        doTest(readFile("/window-rule-time.json"), 900);
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

    private void doTest(String rulesJson, long sleepMs) throws Exception {
        new Expectations() {{
            mockContext.getComponentOutputFields(anyString, anyString);
            result = new Fields(IotasEvent.IOTAS_EVENT);
            mockContext.getComponentId(anyInt);
            result = "componentid";
        }};
        RulesBoltDependenciesFactory factory = new RulesBoltDependenciesFactory(
                new RulesProcessorJsonBuilder(rulesJson), RulesBoltDependenciesFactory.ScriptType.SQL);
        Window windowConfig = factory.createRuleProcessorRuntime().getRulesRuntime().get(0).getRule().getWindow();
        WindowRulesBolt wb = new WindowRulesBolt(factory);
        wb.withWindowConfig(windowConfig);
        WindowedBoltExecutor wbe = new WindowedBoltExecutor(wb);
        Map<String, Object> conf = wb.getComponentConfiguration();
        conf.put("topology.message.timeout.secs", 30);
        wbe.prepare(conf, mockContext, mockCollector);
        for (int i = 1; i <= 20; i++) {
            wbe.execute(getNextTuple(i));
        }
        if (sleepMs > 0) {
            Thread.sleep(sleepMs);
        }
    }

    private String readFile(String fn) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(fn));
    }

    private Tuple getNextTuple(int i) {
        IotasEvent event = new IotasEventImpl(ImmutableMap.<String, Object>of("empid", i, "salary", i * 10), "dsrcid");
        return new TupleImpl(mockContext, new Values(event), 1, "stream");
    }

}