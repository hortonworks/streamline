/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.iotas.bolt.rules;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.layout.runtime.processor.RuleProcessorRuntime;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntime;
import com.hortonworks.iotas.layout.runtime.rule.RulesBoltDependenciesFactory;
import com.hortonworks.iotas.layout.runtime.rule.topology.RuleProcessorMockBuilder;
import com.hortonworks.iotas.layout.runtime.rule.topology.RulesTopologyTest;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

@RunWith(JMockit.class)
public abstract class RulesBoltTest extends RulesTopologyTest {
    protected static final Logger log = LoggerFactory.getLogger(RulesBoltTest.class);

    // JUnit constructs for printing which tests are being run
    public @Rule TestName testName = new TestName();
    public @Rule TestWatcher watchman = new TestWatcher() {
        @Override
        public void starting(final Description method) {
            log.debug("RUNNING TEST [{}] ", method.getMethodName());
        }
    };

    // Only one rule triggers with these values for temperature, humidity
    private static final IotasEvent IOTAS_EVENT_MATCHES_TUPLE = new IotasEventImpl(new HashMap<String, Object>() {{
        put(RuleProcessorMockBuilder.TEMPERATURE, 101);
        put(RuleProcessorMockBuilder.HUMIDITY, 51);
    }}, "dataSrcId_1", "1");

    private static final Values IOTAS_EVENT_MATCHES_TUPLE_VALUES = new Values(IOTAS_EVENT_MATCHES_TUPLE);

    private static final IotasEvent IOTAS_EVENT_NO_MATCH_TUPLE = new IotasEventImpl(new HashMap<String, Object>() {{
        put("non_existent_field1", 101);
        put("non_existent_field2", 51);
        put("non_existent_field3", 23);
    }}, "dataSrcId_2", "2");

    // Only one rule triggers with these values for temperature, humidity. Other fields are disregarded
    private static final IotasEvent IOTAS_EVENT_MATCH_AND_NO_MATCH_TUPLE = new IotasEventImpl(new HashMap<String, Object>() {{
        put("non_existent_field1", 101);
        put(RuleProcessorMockBuilder.TEMPERATURE, 101);
        put("non_existent_field2", 51);
        put(RuleProcessorMockBuilder.HUMIDITY, 51);
        put("non_existent_field3", 23);
    }}, "dataSrcId_2", "3");

    private static final Values IOTAS_EVENT_MATCH_AND_NO_MATCH_TUPLE_VALUES = new Values(IOTAS_EVENT_MATCH_AND_NO_MATCH_TUPLE);

    protected  @Tested RulesBolt rulesBolt;
    protected  @Injectable OutputCollector mockOutputCollector;
    protected  @Injectable Tuple mockTuple;
    protected RuleProcessorRuntime ruleProcessorRuntime;

    @Before
    public void setup() throws Exception {
        RulesBoltDependenciesFactory dependenciesBuilderFactory = createDependenciesBuilderFactory(createRulesProcessorBuilder(), getScriptType());
        ruleProcessorRuntime = dependenciesBuilderFactory.createRuleProcessorRuntime();
        createAndPrepareRulesBolt(dependenciesBuilderFactory);
    }

    private void createAndPrepareRulesBolt(RulesBoltDependenciesFactory dependenciesBuilderFactory) {
        rulesBolt = (RulesBolt)createRulesBolt(dependenciesBuilderFactory);
        rulesBolt.prepare(null, null, mockOutputCollector);
    }

    //@Test
    public void test_allFieldsMatchTuple_oneRuleEvaluates_acks() throws Exception {
        new Expectations() {{
//            mockTuple.getValues();
//            result = IOTAS_EVENT_MATCHES_TUPLE_VALUES;

            mockTuple.getValueByField(IotasEvent.IOTAS_EVENT);
            result = IOTAS_EVENT_MATCHES_TUPLE;
        }};

        executeAndVerifyCollectorCallsAcks(1, IOTAS_EVENT_MATCHES_TUPLE_VALUES);
    }

    //@Test
    public void test_someFieldsMatchTuple_oneRuleEvaluates_acks() throws Exception {
        new Expectations() {{
//            mockTuple.getValues();
//            result = IOTAS_EVENT_MATCH_AND_NO_MATCH_TUPLE_VALUES;

            mockTuple.getValueByField(IotasEvent.IOTAS_EVENT);
            result = IOTAS_EVENT_MATCH_AND_NO_MATCH_TUPLE;
        }};

        executeAndVerifyCollectorCallsAcks(1, IOTAS_EVENT_MATCH_AND_NO_MATCH_TUPLE_VALUES);
    }

    @Test
    public void test_noFieldsMatchTuple_ruleDoesNotEvaluate_fails() throws Exception {
//        test_allFieldsMatchTuple_oneRuleEvaluates_acks();
        new Expectations() {{
            mockTuple.getValueByField(IotasEvent.IOTAS_EVENT);
            result = IOTAS_EVENT_NO_MATCH_TUPLE;
        }};

        executeAndVerifyCollectorCallsAcks(0, null);
    }

    @Test
    public void test_nullIotasEvent_ruleDoesNotEvaluate_acks() throws Exception {
        new Expectations() {{
            mockTuple.getValueByField(IotasEvent.IOTAS_EVENT);
            returns(null);
        }};

        executeAndVerifyCollectorCallsAcks(0, null);
    }

    private void executeAndVerifyCollectorCallsAcks(final int rule2NumTimes, final Values expectedValues) {
        rulesBolt.execute(mockTuple);

        new VerificationsInOrder() {{
            mockOutputCollector.emit(((RuleRuntime) ruleProcessorRuntime.getRulesRuntime().get(0)).getStreams().get(0),
                    mockTuple, IOTAS_EVENT_MATCHES_TUPLE_VALUES);
            times = 0;  // rule 1 does not trigger

            Values actualValues;
            mockOutputCollector.emit(((RuleRuntime) ruleProcessorRuntime.getRulesRuntime().get(1)).getStreams().get(0),
                    mockTuple, actualValues = withCapture());
            times = rule2NumTimes;    // rule 2 triggers rule2NumTimes

            Assert.assertEquals(expectedValues, actualValues);
            mockOutputCollector.ack(mockTuple);
            times = 1;
        }};
    }

    private void executeAndVerifyCollectorFails(final boolean isSuccess, final int rule2NumTimes, final Values expectedValues) {
        rulesBolt.execute(mockTuple);
        new VerificationsInOrder() {{
            mockOutputCollector.fail(mockTuple);
        }};
    }
}
