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

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.layout.runtime.rule.RuleRuntime;
import com.hortonworks.iotas.layout.runtime.rule.RulesBoltDependenciesFactory;
import mockit.Expectations;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.apache.storm.tuple.Values;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith(JMockit.class)
public class RulesBoltSqlTest extends RulesBoltTest {
    private static final IotasEvent IOTAS_EVENT = new IotasEventImpl(new HashMap<String, Object>() {{
        put("humidity", 51);
        put("temperature", 101);
        put("field3", 23);
        put("devicename", "nestdevice");
    }}, "dataSrcId_3", "3");

    private static final IotasEvent PROJECTED_IOTAS_EVENT = new IotasEventImpl(new HashMap<String, Object>() {{
        put("humidity", 51);
        put("INCR(humidity, 10)", 61);
        put("UPPER(devicename)", "NESTDEVICE");
    }}, "dataSrcId_3", "3");

    private static final Values IOTAS_EVENT_VALUES = new Values(IOTAS_EVENT);

    protected RulesBoltDependenciesFactory.ScriptType getScriptType() {
        return RulesBoltDependenciesFactory.ScriptType.SQL;
    }

    @Test
    public void test_ProjectedValues() throws Exception {
        new Expectations() {{
            mockTuple.getValueByField(IotasEvent.IOTAS_EVENT);
            result = IOTAS_EVENT;
        }};

        rulesBolt.execute(mockTuple);

        new VerificationsInOrder() {{
            mockOutputCollector.emit(ruleProcessorRuntime.getRulesRuntime().get(0).getStreams().iterator().next(),
                                     mockTuple, IOTAS_EVENT_VALUES);
            times = 0; // rule 1 does not trigger

            Values actualValues;
            mockOutputCollector.emit(ruleProcessorRuntime.getRulesRuntime().get(1).getStreams().iterator().next(),
                                     mockTuple, actualValues = withCapture());
            times = 1;    // rule 2 triggers
            Assert.assertEquals(PROJECTED_IOTAS_EVENT.getFieldsAndValues(), ((IotasEvent)actualValues.get(0)).getFieldsAndValues());
            mockOutputCollector.ack(mockTuple);
            times = 1;
        }};

    }

}
