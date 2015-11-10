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

package com.hortonworks.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.hortonworks.bolt.rules.RulesBolt;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.layout.runtime.processor.RuleProcessorRuntime;
import com.hortonworks.iotas.layout.runtime.rule.topology.RulesTopologyTest;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

@RunWith(JMockit.class)
public class RulesBoltTest extends RulesTopologyTest {
    protected static final Logger log = LoggerFactory.getLogger(RulesBoltTest.class);

    public static final IotasEventImpl IOTAS_EVENT = new IotasEventImpl(new HashMap<String, Object>() {{
        put("temperature", 101);
        put("humidity", 51);
    }}, "dataSrcId", "23");

    public static final IotasEventImpl IOTAS_EVENT_INVALID_FIELDS = new IotasEventImpl(new HashMap<String, Object>() {{
        put("non_existent_field1", 101);
        put("non_existent_field2", 51);
        put("non_existent_field3", 23);
    }}, "dataSrcId", "23");

    private static final Values VALUES = new Values(IOTAS_EVENT);

    //TODO: Check all of this

    private @Tested
    RulesBolt rulesBolt;

    private @Injectable OutputCollector mockOutputCollector;
    private @Injectable Tuple mockTuple;
    private RuleProcessorRuntime ruleProcessorRuntime;

    @Before
    public void setup() throws Exception {
        ruleProcessorRuntime = createRulesProcessorRuntime();
        rulesBolt = (RulesBolt) createRulesBolt(ruleProcessorRuntime);
        rulesBolt.prepare(null, null, mockOutputCollector);
    }

    @Test
    public void test_validTuple_oneRuleEvaluates() throws Exception {
        new Expectations() {{
            mockTuple.getValueByField(IotasEvent.IOTAS_EVENT); returns(IOTAS_EVENT);
        }};

        callExecuteAndVerifyCollectorInteraction(true);
    }

    @Test
    public void test_invalidTuple_failsTuple() throws Exception {
        new Expectations() {{
            mockTuple.getValueByField(IotasEvent.IOTAS_EVENT); returns(null);
        }};

        callExecuteAndVerifyCollectorInteraction(false);
    }

    @Test
    public void test_tupleInvalidFields_failsTuple() throws Exception {
        new Expectations() {{
            mockTuple.getValueByField(IotasEvent.IOTAS_EVENT); returns(IOTAS_EVENT_INVALID_FIELDS);
        }};

        callExecuteAndVerifyCollectorInteraction(false);
    }

    private void callExecuteAndVerifyCollectorInteraction(final boolean isSuccess) {
        rulesBolt.execute(mockTuple);

        if(isSuccess) {
            new VerificationsInOrder() {{
                mockOutputCollector.emit(ruleProcessorRuntime.getRulesRuntime().get(0).getStreamId(), mockTuple, withAny(VALUES)); times = 0;
                mockOutputCollector.emit(ruleProcessorRuntime.getRulesRuntime().get(1).getStreamId(), mockTuple, withAny(VALUES)); times = 1;
                mockOutputCollector.ack(mockTuple); times = 1;
            }};

        } else {
            new VerificationsInOrder() {{
                mockOutputCollector.fail(mockTuple);
            }};
        }
    }

}
