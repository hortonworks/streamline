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

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.runtime.utils.StreamlineEventTestUtil;
import mockit.Expectations;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.apache.storm.tuple.Values;
import com.hortonworks.streamline.streams.runtime.processor.RuleProcessorRuntime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class RulesBoltSqlTest extends RulesBoltTest {
    private static final StreamlineEvent STREAMLINE_EVENT = StreamlineEventImpl.builder()
            .fieldsAndValues(new HashMap<String, Object>() {{
                put("humidity", 51);
                put("temperature", 101);
                put("field3", 23);
                put("devicename", "nestdevice");
            }})
            .dataSourceId("dataSrcId_3")
            .build();

    private static final StreamlineEvent PROJECTED_STREAMLINE_EVENT = StreamlineEventImpl.builder()
            .fieldsAndValues(new HashMap<String, Object>() {{
                put("humidity", 51);
                put("INCR(humidity, 10)", 61);
                put("UPPER(devicename)", "NESTDEVICE");
            }})
            .dataSourceId("dataSrcId_3")
            .build();

    private static final Values STREAMLINE_EVENT_VALUES = new Values(STREAMLINE_EVENT);

    protected RuleProcessorRuntime.ScriptType getScriptType() {
        return RuleProcessorRuntime.ScriptType.SQL;
    }

    @Test
    public void test_ProjectedValues() throws Exception {
        new Expectations() {{
            mockTuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
            result = STREAMLINE_EVENT;
            mockTuple.getSourceStreamId();
            result = "default";
            mockContext.getThisComponentId();
            result = "componentid"; minTimes = 0;
        }};

        rulesBolt.execute(mockTuple);

        new VerificationsInOrder() {{
            mockOutputCollector.emit(rulesProcessor.getRules().get(0).getOutputStreamNameForAction(rulesProcessor.getRules().get(0).getActions().iterator().next()),
                                     mockTuple, STREAMLINE_EVENT_VALUES);
            times = 0; // rule 1 does not trigger

            Values actualValues;
            mockOutputCollector.emit(rulesProcessor.getRules().get(1).getOutputStreamNameForAction(rulesProcessor.getRules().get(1).getActions().iterator().next()),
                                     mockTuple, actualValues = withCapture());
            times = 1;    // rule 2 triggers
            StreamlineEventTestUtil.assertEventIsProperlyCopied((StreamlineEvent) actualValues.get(0), PROJECTED_STREAMLINE_EVENT);
            mockOutputCollector.ack(withAny(mockTuple));
            times = 1;
        }};

    }

}
