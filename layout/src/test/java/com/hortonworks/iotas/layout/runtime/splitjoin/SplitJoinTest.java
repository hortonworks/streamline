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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.layout.runtime.splitjoin;

import com.google.common.collect.Sets;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.layout.design.splitjoin.JoinAction;
import com.hortonworks.iotas.layout.design.splitjoin.SplitAction;
import com.hortonworks.iotas.layout.design.splitjoin.StageAction;
import com.hortonworks.iotas.layout.design.transform.EnrichmentTransform;
import com.hortonworks.iotas.layout.design.transform.InmemoryTransformDataProvider;
import com.hortonworks.iotas.layout.design.transform.Transform;
import com.hortonworks.iotas.layout.runtime.rule.action.ActionRuntimeContext;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tests related to split/join/stage processors.
 */
public class SplitJoinTest {
    private static final Logger log = LoggerFactory.getLogger(SplitJoinTest.class);

    @Test
    public void testSplitJoinProcessors() throws Exception {
        String[] outputStreams = {"stream-1", "stream-2", "stream-3"};

        final SplitAction splitAction = new SplitAction();
        splitAction.setOutputStreams(Sets.newHashSet(outputStreams));
        final JoinAction joinAction = new JoinAction();
        joinAction.setOutputStreams(Collections.singleton("output-stream"));

        runSplitJoin(splitAction, joinAction);
    }

    public static class MySplitter extends DefaultSplitter {
        public static int invocationCount = 0;

        public MySplitter() {
        }

        @Override
        public List<Result> splitEvent(IotasEvent inputEvent, Set<String> outputStreams) {
            log.info("##########MySplitter.splitEvent");
            invocationCount++;
            return super.splitEvent(inputEvent, outputStreams);
        }
    }

    public static class MyJoiner extends DefaultJoiner {
        public static int invocationCount = 0;

        public MyJoiner() {
        }

        @Override
        public IotasEvent join(EventGroup eventGroup) {
            log.info("##########MyJoiner.join");
            invocationCount++;
            return super.join(eventGroup);
        }
    }

    @Test
    public void testCustomSplitJoin() {

        String[] outputStreams = {"stream-1", "stream-2", "stream-3"};

        final SplitAction splitAction = new SplitAction(null, MySplitter.class.getName());
        splitAction.setOutputStreams(Sets.newHashSet(outputStreams));

        final JoinAction joinAction = new JoinAction(null, MyJoiner.class.getName());
        joinAction.setOutputStreams(Collections.singleton("output-stream"));
        MySplitter.invocationCount= 0;

        runSplitJoin(splitAction, joinAction);

        Assert.assertTrue(MySplitter.invocationCount == 1);
        Assert.assertTrue(MyJoiner.invocationCount == 1);
    }

    protected void runSplitJoin(SplitAction splitAction, JoinAction joinAction) {
        SplitActionRuntime splitActionRuntime = new SplitActionRuntime(splitAction);
        splitActionRuntime.setActionRuntimeContext(new ActionRuntimeContext(null, splitAction));
        splitActionRuntime.initialize(Collections.<String, Object>emptyMap());

        IotasEvent iotasEvent = createRootEvent();
        final List<Result> results = splitActionRuntime.execute(iotasEvent);

        JoinActionRuntime joinActionRuntime = new JoinActionRuntime(joinAction);
        joinActionRuntime.setActionRuntimeContext(new ActionRuntimeContext(null, joinAction));
        joinActionRuntime.initialize(Collections.<String, Object>emptyMap());

        List<Result> effectiveResult = null;
        for (Result result : results) {
            for (IotasEvent event : result.events) {
                List<Result> processedResult = joinActionRuntime.execute(event);
                if(processedResult != null ) {
                    effectiveResult = processedResult;
                }
            }
        }

        Assert.assertNotNull(effectiveResult);
    }

    @Test
    public void testStageProcessor() {
        final String enrichFieldName = "foo";
        final String enrichedValue = "foo-enriched-value";

        Map<Object, Object> data = new HashMap<Object, Object>(){{put("foo-value", enrichedValue);}};
        InmemoryTransformDataProvider transformDataProvider = new InmemoryTransformDataProvider(data);
        EnrichmentTransform enrichmentTransform = new EnrichmentTransform("enricher", Collections.singletonList(enrichFieldName), transformDataProvider);
        StageAction stageAction = new StageAction(Collections.<Transform>singletonList(enrichmentTransform));
        stageAction.setOutputStreams(Collections.singleton("output-stream"));

        StageActionRuntime stageActionRuntime = new StageActionRuntime(stageAction);
        stageActionRuntime.setActionRuntimeContext(new ActionRuntimeContext(null, stageAction));
        stageActionRuntime.initialize(Collections.<String, Object>emptyMap());

        final List<Result> results = stageActionRuntime.execute(createRootEvent());
        for (Result result : results) {
            for (IotasEvent event : result.events) {
                final Map enrichments = (Map) event.getAuxiliaryFieldsAndValues().get(EnrichmentTransform.ENRICHMENTS_FIELD_NAME);
                Assert.assertEquals(enrichments.get(enrichFieldName), enrichedValue);
            }
        }
    }

    private IotasEvent createRootEvent() {
        Map<String, Object> fieldValues = new HashMap<String, Object>(){{put("foo", "foo-value"); put("bar", "bar-"+System.currentTimeMillis());}};

        return new IotasEventImpl(fieldValues, "ds-1", UUID.randomUUID().toString(), Collections.<String, Object>emptyMap(), "source-stream");
    }
}
