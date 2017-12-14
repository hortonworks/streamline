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

package com.hortonworks.streamline.streams.runtime.storm.event.correlation;

import mockit.Deencapsulation;
import mockit.integration.junit4.JMockit;
import org.apache.storm.tuple.Values;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(JMockit.class)
public class EventCorrelatingWindowedOutputCollectorTest extends EventCorrelatingOutputCollectorTest {

    @Override
    protected EventCorrelatingOutputCollector getSystemUnderTest() {
        EventCorrelatingWindowedOutputCollector sut = new EventCorrelatingWindowedOutputCollector(mockedTopologyContext, mockedOutputCollector);
        Deencapsulation.setField(sut, "eventCorrelationInjector", mockStormEventCorrelationInjector);
        return sut;
    }

    @Override
    public void emitWithoutAnchor() throws Exception {
        setupExpectationsForEventCorrelationInjector();

        EventCorrelatingOutputCollector sut = getSystemUnderTest();

        String testStreamId = "testStreamId";
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);

        // String streamId, List<Object> tuple
        try {
            sut.emit(testStreamId, tuple);
            Assert.fail("Should throw UnsupportedOperationException.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // List<Object> tuple
        try {
            sut.emit(tuple);
            Assert.fail("Should throw UnsupportedOperationException.");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Override
    public void emitDirectWithoutAnchor() throws Exception {
        setupExpectationsForEventCorrelationInjector();

        EventCorrelatingOutputCollector sut = getSystemUnderTest();

        int testTaskId = TASK_1;
        String testStreamId = "testStreamId";
        final Values tuple = new Values(INPUT_STREAMLINE_EVENT);

        // int taskId, String streamId, List<Object> tuple
        try {
            sut.emitDirect(testTaskId, testStreamId, tuple);
            Assert.fail("Should throw UnsupportedOperationException.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // int taskId, List<Object> tuple
        try {
            sut.emitDirect(testTaskId, tuple);
            Assert.fail("Should throw UnsupportedOperationException.");
        } catch (UnsupportedOperationException e) {
            // expected
        }

    }
}