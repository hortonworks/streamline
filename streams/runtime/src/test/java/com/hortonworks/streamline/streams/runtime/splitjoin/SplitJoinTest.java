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

package com.hortonworks.streamline.streams.runtime.splitjoin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.hortonworks.streamline.common.Constants;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.common.utils.CatalogRestClient;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.layout.Transform;
import com.hortonworks.streamline.streams.layout.component.impl.splitjoin.JoinAction;
import com.hortonworks.streamline.streams.layout.component.impl.splitjoin.SplitAction;
import com.hortonworks.streamline.streams.layout.component.impl.splitjoin.StageAction;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;
import com.hortonworks.streamline.streams.layout.component.rule.action.Action;
import com.hortonworks.streamline.streams.layout.component.rule.action.transform.EnrichmentTransform;
import com.hortonworks.streamline.streams.layout.component.rule.action.transform.InmemoryTransformDataProvider;
import com.hortonworks.streamline.streams.layout.component.rule.action.transform.ProjectionTransform;
import com.hortonworks.streamline.streams.runtime.rule.action.ActionRuntimeContext;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * Tests related to split/join/stage processors.
 */
@RunWith(JMockit.class)
public class SplitJoinTest {
    private static final Logger log = LoggerFactory.getLogger(SplitJoinTest.class);

    @Test
    public void testSplitJoinProcessorsWithActionsHavingStreams() throws Exception {
        String[] outputStreams = {"stream-1", "stream-2", "stream-3"};

        final SplitAction splitAction = new SplitAction();
        splitAction.setOutputStreams(Sets.newHashSet(outputStreams));
        final JoinAction joinAction = new JoinAction();
        joinAction.setOutputStreams(Collections.singleton("output-stream"));

        runSplitJoin(splitAction, joinAction);
    }

    @Test
    public void testSplitJoinProcessorsWithRuleHavingStreams() throws Exception {
        String[] outputStreams = {"stream-1", "stream-2", "stream-3"};
        SplitJoinRule splitRule = new SplitJoinRule("split", new SplitAction(), Sets.newHashSet(outputStreams));

        SplitJoinRule joinRule = new SplitJoinRule("join", new JoinAction(), Collections.singleton("output-stream"));

        runSplitJoin(splitRule, joinRule);
    }

    static class SplitJoinRule extends Rule {

        private final Action action;

        public SplitJoinRule(String name, Action action, Set<String> outputStreams) {
            this.action = action;
            setName(name);
            setId(new Random().nextLong());
            setActions(Collections.singletonList(action));
            setStreams(outputStreams);
        }

        public Action getAction() {
            return action;
        }
    }

    @Test
    public void testCustomSplitJoinWithRules() {

        String[] outputStreams = {"stream-1", "stream-2", "stream-3"};

        final SplitAction splitAction = new SplitAction(MySplitter.class.getName());
        SplitJoinRule splitRule = new SplitJoinRule("split", splitAction, Sets.newHashSet(outputStreams));

        final JoinAction joinAction = new JoinAction(MyJoiner.class.getName());
        SplitJoinRule joinRule = new SplitJoinRule("join", joinAction, Collections.singleton("output-stream"));

        resetCounters();

        runSplitJoin(splitRule, joinRule);

        Assert.assertTrue(MySplitter.invocationCount == 1);
        Assert.assertTrue(MyJoiner.invocationCount == 1);
    }

    @Test
    public void testStageProcessorWithRules() {
        final String enrichFieldName = "foo";
        final String enrichedValue = "foo-enriched-value";

        Map<Object, Object> data = new HashMap<Object, Object>(){{put("foo-value", enrichedValue);}};
        InmemoryTransformDataProvider transformDataProvider = new InmemoryTransformDataProvider(data);
        EnrichmentTransform enrichmentTransform = new EnrichmentTransform("enricher", Collections.singletonList(enrichFieldName), transformDataProvider);
        StageAction stageAction = new StageAction(Collections.<Transform>singletonList(enrichmentTransform));
        SplitJoinRule stageRule = new SplitJoinRule("stage-1", stageAction, Collections.singleton("output-stream"));

        StageActionRuntime stageActionRuntime = new StageActionRuntime(stageAction);
        stageActionRuntime.setActionRuntimeContext(new ActionRuntimeContext(stageRule, stageAction));
        stageActionRuntime.initialize(Collections.<String, Object>emptyMap());

        final List<Result> results = stageActionRuntime.execute(createRootEvent());
        for (Result result : results) {
            for (StreamlineEvent event : result.events) {
                final Map enrichments = (Map) event.getAuxiliaryFieldsAndValues().get(EnrichmentTransform.ENRICHMENTS_FIELD_NAME);
                Assert.assertEquals(enrichments.get(enrichFieldName), enrichedValue);
            }
        }
    }

    private void runSplitJoin(SplitJoinRule splitRule, SplitJoinRule joinRule) {
        runSplitJoin(splitRule, joinRule, Collections.<String, Object>emptyMap());
    }

    private void runSplitJoin(SplitJoinRule splitRule, SplitJoinRule joinRule, Map<String, Object> config) {
        final SplitAction splitAction = (SplitAction) splitRule.getAction();
        SplitActionRuntime splitActionRuntime = new SplitActionRuntime(splitAction);
        splitActionRuntime.setActionRuntimeContext(new ActionRuntimeContext(splitRule, splitAction));
        splitActionRuntime.initialize(config);

        StreamlineEvent streamlineEvent = createRootEvent();
        final List<Result> results = splitActionRuntime.execute(streamlineEvent);

        JoinAction joinAction = (JoinAction) joinRule.getAction();
        JoinActionRuntime joinActionRuntime = new JoinActionRuntime(joinAction);
        joinActionRuntime.setActionRuntimeContext(new ActionRuntimeContext(joinRule, joinAction));
        joinActionRuntime.initialize(config);

        List<Result> effectiveResult = null;
        for (Result result : results) {
            for (StreamlineEvent event : result.events) {
                List<Result> processedResult = joinActionRuntime.execute(event);
                if(processedResult != null ) {
                    effectiveResult = processedResult;
                }
            }
        }

        Assert.assertNotNull(effectiveResult);
    }

    @Test
    public void testCustomSplitJoin() {

        String[] outputStreams = {"stream-1", "stream-2", "stream-3"};

        final SplitAction splitAction = new SplitAction(MySplitter.class.getName());
        splitAction.setOutputStreams(Sets.newHashSet(outputStreams));

        final JoinAction joinAction = new JoinAction(MyJoiner.class.getName());
        joinAction.setOutputStreams(Collections.singleton("output-stream"));
        resetCounters();

        runSplitJoin(splitAction, joinAction);

        Assert.assertTrue(MySplitter.invocationCount == 1);
        Assert.assertTrue(MyJoiner.invocationCount == 1);
    }

    protected void resetCounters() {
        MySplitter.invocationCount= 0;
        MyJoiner.invocationCount = 0;
    }


    protected void runSplitJoin(SplitAction splitAction, JoinAction joinAction) {
        runSplitJoin(splitAction, joinAction, Collections.<String, Object>emptyMap());
    }

    protected void runSplitJoin(SplitAction splitAction, JoinAction joinAction, Map<String, Object> config) {
        SplitActionRuntime splitActionRuntime = new SplitActionRuntime(splitAction);
        splitActionRuntime.setActionRuntimeContext(new ActionRuntimeContext(null, splitAction));
        splitActionRuntime.initialize(config);

        StreamlineEvent event = createRootEvent();
        final List<Result> results = splitActionRuntime.execute(event);

        JoinActionRuntime joinActionRuntime = new JoinActionRuntime(joinAction);
        joinActionRuntime.setActionRuntimeContext(new ActionRuntimeContext(null, joinAction));
        joinActionRuntime.initialize(config);

        List<Result> effectiveResult = null;
        for (Result result : results) {
            for (StreamlineEvent e : result.events) {
                List<Result> processedResult = joinActionRuntime.execute(e);
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
            for (StreamlineEvent event : result.events) {
                final Map enrichments = (Map) event.getAuxiliaryFieldsAndValues().get(EnrichmentTransform.ENRICHMENTS_FIELD_NAME);
                Assert.assertEquals(enrichments.get(enrichFieldName), enrichedValue);
            }
        }
    }

    private StreamlineEvent createRootEvent() {
        Map<String, Object> fieldValues = new HashMap<String, Object>(){{put("foo", "foo-value"); put("bar", "bar-"+System.currentTimeMillis());}};

        return StreamlineEventImpl.builder()
                .fieldsAndValues(fieldValues)
                .dataSourceId("ds-1")
                .sourceStream("source-stream")
                .build();
    }

    @Mocked
    CatalogRestClient mockCatalogRestClient;

    @Test
    @Ignore
    public void testCustomLoadedSplitJoinInSameClassLoader() throws Exception {

        final String splitterClassName = MySplitter.class.getName();
        final String joinerClassName = MyJoiner.class.getName();

        runCustomLoadedSplitJoin(splitterClassName, joinerClassName, createJarInputStream(splitterClassName), createJarInputStream(joinerClassName));
    }

    protected void runCustomLoadedSplitJoin(String splitterClassName, String joinerClassName,
                                            final InputStream splitJarInputStream, final InputStream joinJarInputStream)
            throws IOException {
        String[] outputStreams = {"stream-1", "stream-2", "stream-3"};

        final Long splitJarId = 1L;
        final Long joinJarId = splitJarId+1;

        try {
            new Expectations() {
                {
                    mockCatalogRestClient.getFile(splitJarId);
                    result = splitJarInputStream;

                    mockCatalogRestClient.getFile(joinJarId);
                    result = joinJarInputStream;
                }
            };

            final SplitAction splitAction = new SplitAction(splitJarId, splitterClassName);
            splitAction.setOutputStreams(Sets.newHashSet(outputStreams));
            final JoinAction joinAction = new JoinAction(joinJarId, joinerClassName);
            joinAction.setOutputStreams(Collections.singleton("output-stream"));

            Map<String, Object> config = new HashMap<>();
            config.put(Constants.CATALOG_ROOT_URL, "dummy-url");
            final Path tempDirectory = Files.createTempDirectory("sj-test");
            tempDirectory.toFile().deleteOnExit();
            config.put(Constants.LOCAL_FILES_PATH, tempDirectory);

            runSplitJoin(splitAction, joinAction, config);
        } finally {
            splitJarInputStream.close();
            joinJarInputStream.close();
        }
    }

    @Test
    @Ignore
    public void testCustomLoadedSplitJoin() throws Exception {
        final String splitterClassName = "com.hortonworks.streamline.layout.runtime.splitjoin.CustomSplitter";
        final String joinerClassName = "com.hortonworks.streamline.layout.runtime.splitjoin.CustomJoiner";

        final InputStream splitJarInputStream = this.getClass().getResourceAsStream("/custom-split-join-lib.jar");
        final InputStream joinJarInputStream = this.getClass().getResourceAsStream("/custom-split-join-lib.jar");
        runCustomLoadedSplitJoin(splitterClassName, joinerClassName, splitJarInputStream, joinJarInputStream);
    }

    private JarInputStream createJarInputStream(String... classNames) throws Exception {

        final File tempFile = Files.createTempFile(UUID.randomUUID().toString(), ".jar").toFile();
        tempFile.deleteOnExit();

        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tempFile))) {
            for (String className : classNames) {
                final String classFileName = className.replace(".", "/") + ".class";
                try (InputStream classInputStream = this.getClass().getResourceAsStream("/" + classFileName)) {
                    jarOutputStream.putNextEntry(new JarEntry(classFileName));
                    IOUtils.copy(classInputStream, jarOutputStream);
                }
            }
        }

        return new JarInputStream(new FileInputStream(tempFile));
    }

    @Test
    public void testActionsJsonSerDeser() throws Exception {
        Action[] actions = {
                new SplitAction(System.currentTimeMillis(), "com.hortonworks.streamline.sj.SplitterClass"),
                new JoinAction(System.currentTimeMillis(), "com.hortonworks.streamline.sj.JoinerClass"),
                new StageAction(Collections.<Transform>singletonList(new ProjectionTransform("projection", Collections.singleton("foo")))),
        };
        for (Action action : actions) {
            checkActionWriteReadJsons(action);
        }
    }

    protected static void checkActionWriteReadJsons(Action action) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        final String value = objectMapper.writeValueAsString(action);
        log.info("####### value = " + value);

        Class<? extends Action> actionClass = Action.class;
        Action actionRead = objectMapper.readValue(value, actionClass);
        log.info("####### actionRead = " + actionRead);
    }
}
