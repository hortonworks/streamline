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
import com.hortonworks.iotas.client.CatalogRestClient;
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
import com.hortonworks.iotas.util.CoreUtils;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
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
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * Tests related to split/join/stage processors.
 */
@RunWith(JMockit.class)
public class SplitJoinTest {

    @Test
    public void testSplitJoinProcessors() throws Exception {
        String[] outputStreams = {"stream-1", "stream-2", "stream-3"};

        final SplitAction splitAction = new SplitAction();
        splitAction.setOutputStreams(Sets.newHashSet(outputStreams));
        final JoinAction joinAction = new JoinAction();
        joinAction.setOutputStreams(Collections.singleton("output-stream"));

        runSplitJoin(splitAction, joinAction);
    }

    @Test
    public void testCustomSplitJoin() {

        String[] outputStreams = {"stream-1", "stream-2", "stream-3"};

        final SplitAction splitAction = new SplitAction(MySplitter.class.getName());
        splitAction.setOutputStreams(Sets.newHashSet(outputStreams));

        final JoinAction joinAction = new JoinAction(MyJoiner.class.getName());
        joinAction.setOutputStreams(Collections.singleton("output-stream"));
        MySplitter.invocationCount= 0;

        runSplitJoin(splitAction, joinAction);

        Assert.assertTrue(MySplitter.invocationCount == 1);
        Assert.assertTrue(MyJoiner.invocationCount == 1);
    }

    protected void runSplitJoin(SplitAction splitAction, JoinAction joinAction) {
        runSplitJoin(splitAction, joinAction, Collections.<String, Object>emptyMap());
    }
    protected void runSplitJoin(SplitAction splitAction, JoinAction joinAction, Map<String, Object> config) {
        SplitActionRuntime splitActionRuntime = new SplitActionRuntime(splitAction);
        splitActionRuntime.setActionRuntimeContext(new ActionRuntimeContext(null, splitAction));
        splitActionRuntime.initialize(config);

        IotasEvent iotasEvent = createRootEvent();
        final List<Result> results = splitActionRuntime.execute(iotasEvent);

        JoinActionRuntime joinActionRuntime = new JoinActionRuntime(joinAction);
        joinActionRuntime.setActionRuntimeContext(new ActionRuntimeContext(null, joinAction));
        joinActionRuntime.initialize(config);

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

    @Mocked
    CatalogRestClient mockCatalogRestClient;

    @Test
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
            config.put(CoreUtils.CATALOG_ROOT_URL, "dummy-url");
            final Path tempDirectory = Files.createTempDirectory("sj-test");
            tempDirectory.toFile().deleteOnExit();
            config.put(CoreUtils.LOCAL_FILES_PATH, tempDirectory);

            runSplitJoin(splitAction, joinAction, config);
        } finally {
            splitJarInputStream.close();
            joinJarInputStream.close();
        }
    }

    @Test
    public void testCustomLoadedSplitJoin() throws Exception {
        final String splitterClassName = "com.hortonworks.iotas.layout.runtime.splitjoin.CustomSplitter";
        final String joinerClassName = "com.hortonworks.iotas.layout.runtime.splitjoin.CustomJoiner";

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
}
