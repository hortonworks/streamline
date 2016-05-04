package com.hortonworks.iotas.layout.runtime.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.errors.ProcessingException;
import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import com.hortonworks.iotas.layout.design.rule.Rule;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntime;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RuleProcessorRuntime}
 */
@RunWith(JMockit.class)
public class RuleProcessorRuntimeTest {
    @Mocked
    RuleProcessorRuntimeDependenciesBuilder mockBuilder;

    @Mocked
    RulesProcessor mockRulesProcessor;

    @Mocked
    RuleRuntime mockRr1;

    @Mocked
    RuleRuntime mockRr2;

    @Mocked
    Rule mockRule1;

    @Mocked
    Rule mockRule2;

    @Mocked
    IotasEvent event1;

    @Mocked
    IotasEvent event2;

    @Mocked
    IotasEvent event3;

    @Mocked
    IotasEvent event4;

    @Before
    public void setUp() {
        new Expectations() {{
            mockBuilder.getRulesProcessor();
            result = mockRulesProcessor;
            mockRulesProcessor.getId();
            result = "rp1";
            minTimes = 0;
            mockBuilder.getRulesRuntime();
            result = ImmutableList.of(mockRr1, mockRr2);
            mockRr1.getRule();
            result = mockRule1;
            mockRr2.getRule();
            result = mockRule2;
            mockRule1.getStreams();
            result = ImmutableSet.of("stream1");
            mockRule2.getStreams();
            result = ImmutableSet.of("stream1", "stream2");
            event1.getSourceStream();
            result = "stream1";
            minTimes = 0;
            event2.getSourceStream();
            result = "stream2";
            minTimes = 0;
            event3.getSourceStream();
            result = "stream3";
            minTimes = 0;
            event4.getSourceStream();
            result = "";
            minTimes = 0;
        }};
    }

    @Test
    public void testRule2Fires() throws Exception {
        RuleProcessorRuntime rpr = new RuleProcessorRuntime(mockBuilder);
        rpr.process(event2);
        new Verifications() {{
            mockRr1.evaluate(event2);
            times=0;
            mockRr2.evaluate(event2);
            times=1;
        }};
    }

    @Test
    public void testRule1AndRule2Fires() throws Exception {
        RuleProcessorRuntime rpr = new RuleProcessorRuntime(mockBuilder);
        rpr.process(event1);
        new Verifications() {{
            mockRr1.evaluate(event1);
            times=1;
            mockRr2.evaluate(event1);
            times=1;
        }};
    }

    @Test
    public void testNonMatchingStream() throws Exception {
        RuleProcessorRuntime rpr = new RuleProcessorRuntime(mockBuilder);
        rpr.process(event3);
        new Verifications() {{
            mockRr1.evaluate(event3);
            times=0;
            mockRr2.evaluate(event3);
            times=0;
        }};
    }

    @Test(expected = ProcessingException.class)
    public void testEventWithEmptyStream() throws Exception {
        RuleProcessorRuntime rpr = new RuleProcessorRuntime(mockBuilder);
        rpr.process(event4);
    }
}