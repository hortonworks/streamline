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
package com.hortonworks.streamline.streams.actions.topology.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.util.ParallelStreamUtil;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.catalog.CatalogToLayoutConverter;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCase;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCaseSink;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCaseSource;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunHistory;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.splitjoin.JoinProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * The class takes care of preparation of topology test run, and request test run to the TopologyActions instance.
 */
public class TopologyTestRunner {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyTestRunner.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StreamCatalogService catalogService;
    private final TopologyActionsService topologyActionsService;
    private final String topologyTestRunResultDir;

    private static final int FORK_JOIN_POOL_PARALLELISM = 10;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(FORK_JOIN_POOL_PARALLELISM);

    public TopologyTestRunner(StreamCatalogService catalogService, TopologyActionsService topologyActionsService, String topologyTestRunResultDir) {
        this.catalogService = catalogService;
        this.topologyActionsService = topologyActionsService;
        this.topologyTestRunResultDir = topologyTestRunResultDir;
    }

    public TopologyTestRunHistory runTest(TopologyActions topologyActions, Topology topology,
                                          String testRunInputJson) throws IOException {
        Map<String, Object> testRunInputMap = objectMapper.readValue(testRunInputJson, Map.class);

        if (!testRunInputMap.containsKey("testCaseId")) {
            throw new IllegalArgumentException("'testCaseId' needs to be presented.");
        }

        Long testCaseId = Long.valueOf(testRunInputMap.get("testCaseId").toString());
        Optional<Long> durationSecs = Optional.empty();

        if (testRunInputMap.containsKey("durationSecs")) {
            durationSecs = Optional.of(Long.valueOf(testRunInputMap.get("durationSecs").toString()));
        }

        TopologyTestRunCase testCase = loadTestRunCase(topology.getId(), testCaseId);

        if (testCase == null) {
            throw new IllegalArgumentException("test case doesn't exist");
        }

        List<StreamlineSource> sources = topology.getTopologyDag().getOutputComponents().stream()
                .filter(c -> c instanceof StreamlineSource)
                .map(c -> (StreamlineSource) c)
                .collect(toList());

        List<StreamlineSink> sinks = topology.getTopologyDag().getInputComponents().stream()
                .filter(c -> c instanceof StreamlineSink)
                .map(c -> (StreamlineSink) c)
                .collect(toList());

        List<StreamlineProcessor> processors = topology.getTopologyDag().getOutputComponents().stream()
                .filter(c -> c instanceof StreamlineProcessor)
                .map(c -> (StreamlineProcessor) c)
                .collect(toList());

        // load test case sources for all sources
        List<TopologyTestRunCaseSource> testRunCaseSources = sources.stream()
                .map(s -> catalogService.getTopologyTestRunCaseSourceBySourceId(testCaseId, Long.valueOf(s.getId())))
                .collect(toList());

        if (testRunCaseSources.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Not every source register test records.");
        }

        // load test case sources for all sinks
        List<TopologyTestRunCaseSink> testRunCaseSinks = sinks.stream()
                .map(s -> catalogService.getTopologyTestRunCaseSinkBySinkId(testCaseId, Long.valueOf(s.getId())))
                .collect(toList());

        Map<Long, Map<String, List<Map<String, Object>>>> testRecordsForEachSources = readTestRecordsFromTestCaseSources(testRunCaseSources);
        Map<Long, Integer> occurrenceForEachSources = readOccurrenceFromTestCaseSources(testRunCaseSources);
        Map<String, List<Map<String, Object>>> expectedOutputRecordsMap = readExpectedRecordsFromTestCaseSinks(sinks, testRunCaseSinks);

        String eventLogFilePath = getTopologyTestRunEventLog(topology);

        Map<String, TestRunSource> testRunSourceMap = sources.stream()
                .collect(toMap(s -> s.getName(),
                        s -> {
                            TestRunSource testRunSource = new TestRunSource(s.getOutputStreams(),
                                    testRecordsForEachSources.get(Long.valueOf(s.getId())),
                                    occurrenceForEachSources.get(Long.valueOf(s.getId())),
                                    eventLogFilePath);
                            testRunSource.setName(s.getName());
                            return testRunSource;
                        }
                ));

        Map<String, TestRunSink> testRunSinkMap = sinks.stream()
                .collect(toMap(s -> s.getName(), s -> {
                    String uuid = UUID.randomUUID().toString();
                    TestRunSink testRunSink = new TestRunSink(getTopologyTestRunResult(uuid), eventLogFilePath);
                    testRunSink.setName(s.getName());
                    return testRunSink;
                }));

        Map<String, TestRunProcessor> testRunProcessorMap = processors.stream()
                .collect(toMap(s -> s.getName(), s -> {
                    // currently only RulesProcessor and successors are candidates for windowing
                    if (s instanceof RulesProcessor) {
                        RulesProcessor rulesProcessor = (RulesProcessor) s;

                        boolean windowed = rulesProcessor.getRules().stream().anyMatch(r -> r.getWindow() != null);
                        TestRunProcessor testRunProcessor = new TestRunProcessor(s, windowed, eventLogFilePath);
                        testRunProcessor.setName(s.getName());
                        return testRunProcessor;
                    } else {
                        TestRunProcessor testRunProcessor = new TestRunProcessor(s, false, eventLogFilePath);
                        testRunProcessor.setName(s.getName());
                        return testRunProcessor;
                    }
                }));

        TopologyTestRunHistory history = initializeTopologyTestRunHistory(topology, testCase,
                expectedOutputRecordsMap, eventLogFilePath);
        catalogService.addTopologyTestRunHistory(history);

        Optional<Long> finalDurationSecs = durationSecs;
        ParallelStreamUtil.runAsync(() -> runTestInBackground(topologyActions, topology, history,
                testRunSourceMap, testRunProcessorMap, testRunSinkMap, expectedOutputRecordsMap,
                finalDurationSecs), forkJoinPool);

        return history;
    }

    private Void runTestInBackground(TopologyActions topologyActions, Topology topology,
                                     TopologyTestRunHistory history,
                                     Map<String, TestRunSource> testRunSourceMap,
                                     Map<String, TestRunProcessor> testRunProcessorMap,
                                     Map<String, TestRunSink> testRunSinkMap,
                                     Map<String, List<Map<String, Object>>> expectedOutputRecordsMap,
                                     Optional<Long> durationSecs) throws IOException {
        TopologyLayout topologyLayout = CatalogToLayoutConverter.getTopologyLayout(topology, topology.getTopologyDag());
        try {
            topologyActionsService.setUpClusterArtifacts(topology, topologyActions);
            String mavenArtifacts = topologyActionsService.setUpExtraJars(topology, topologyActions);

            topologyActions.testRun(topologyLayout, mavenArtifacts, testRunSourceMap, testRunProcessorMap,
                    testRunSinkMap, durationSecs);

            history.finishSuccessfully();

            Map<String, List<Map<String, Object>>> actualOutputRecordsMap = parseTestRunOutputFiles(testRunSinkMap);
            history.setActualOutputRecords(objectMapper.writeValueAsString(actualOutputRecordsMap));

            if (expectedOutputRecordsMap != null) {
                boolean matched = equalsOutputRecords(expectedOutputRecordsMap, actualOutputRecordsMap);
                history.setMatched(matched);
            }
        } catch (Throwable e) {
            LOG.warn("Exception thrown while running Topology as test mode. Marking as 'failed'. topology id: {}",
                    topology.getId(), e);
            history.finishWithFailures();
        }

        catalogService.addOrUpdateTopologyTestRunHistory(history.getId(), history);

        return null;
    }

    private Map<Long, Map<String, List<Map<String, Object>>>> readTestRecordsFromTestCaseSources(List<TopologyTestRunCaseSource> testRunCaseSources) {
        return testRunCaseSources.stream()
                    .collect(toMap(s -> s.getSourceId(), s -> {
                        try {
                            return objectMapper.readValue(s.getRecords(),
                                    new TypeReference<Map<String, List<Map<String, Object>>>>() {
                                    });
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }));
    }

    private Map<Long, Integer> readOccurrenceFromTestCaseSources(List<TopologyTestRunCaseSource> testRunCaseSources) {
        return testRunCaseSources.stream()
                .collect(toMap(s -> s.getSourceId(), s -> s.getOccurrence()));
    }

    private Map<String, List<Map<String, Object>>> readExpectedRecordsFromTestCaseSinks(List<StreamlineSink> sinks,
                                                                                        List<TopologyTestRunCaseSink> testRunCaseSinks) {
        Map<String, String> sinkIdToName = sinks.stream()
                .collect(toMap(s -> s.getId(), s -> s.getName()));
        return testRunCaseSinks.stream()
                .filter(Objects::nonNull)
                .collect(toMap(s -> sinkIdToName.get(String.valueOf(s.getSinkId())), s -> {
                    try {
                        String records = s.getRecords();
                        if (StringUtils.isEmpty(records)) {
                            return new ArrayList<Map<String, Object>>();
                        } else {
                            return objectMapper.readValue(records,
                                    new TypeReference<List<Map<String, Object>>>() {
                                    });
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }

    private TopologyTestRunCase loadTestRunCase(Long topologyId, Long testCaseId) {
        TopologyTestRunCase testCase = catalogService.getTopologyTestRunCase(topologyId, testCaseId);
        if (testCase == null) {
            throw new IllegalArgumentException("Given topology test case doesn't exist - topology id: " +
                    topologyId + " , id: " + testCaseId);
        }
        return testCase;
    }

    private TopologyTestRunHistory initializeTopologyTestRunHistory(Topology topology,
                                                                    TopologyTestRunCase testRunCase,
                                                                    Map<String, List<Map<String, Object>>> expectedOutputRecords,
                                                                    String eventLogFilePath)
            throws JsonProcessingException {
        Long topologyVersionId = catalogService.getCurrentVersionId(topology.getId());

        TopologyTestRunHistory history = new TopologyTestRunHistory();
        history.setTopologyId(topology.getId());
        history.setVersionId(topologyVersionId);
        history.setTestCaseId(testRunCase.getId());

        if (expectedOutputRecords != null) {
            String expectedOutputRecordsJson = objectMapper.writeValueAsString(expectedOutputRecords);
            history.setExpectedOutputRecords(expectedOutputRecordsJson);
        }

        history.setFinished(false);
        history.setStartTime(System.currentTimeMillis());
        history.setEventLogFilePath(eventLogFilePath);
        return history;
    }

    private String getTopologyTestRunResult(String uuid) {
        return topologyTestRunResultDir + File.separator + uuid;
    }

    private String getTopologyTestRunEventLog(Topology topology) {
        return topologyTestRunResultDir + File.separator + "topology-test-run-event-topology-" + topology.getId() +
                "-" + UUID.randomUUID().toString() + ".log";
    }

    private Map<String, List<Map<String, Object>>> parseTestRunOutputFiles(Map<String, TestRunSink> testRunSinkMap) {
        return testRunSinkMap.entrySet().stream()
                .collect(toMap(s -> s.getKey(), s -> {
                    String filePath = s.getValue().getOutputFilePath();

                    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                        return br.lines().map(line -> {
                            try {
                                return (Map<String, Object>) objectMapper.readValue(line,
                                        new TypeReference<Map<String, Object>>() {
                                        });
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }).collect(toList());
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException("Output file not found - file path: " + filePath, e);
                    } catch (IOException e) {
                        throw new RuntimeException("Fail to read output file - file path: " + filePath, e);
                    }
                }));
    }

    private boolean equalsOutputRecords(Map<String, List<Map<String, Object>>> expectedOutputRecordsMap,
                                        Map<String, List<Map<String, Object>>> actualOutputRecordsMap) {
        if (expectedOutputRecordsMap.size() != actualOutputRecordsMap.size()) {
            return false;
        }

        for (Map.Entry<String, List<Map<String, Object>>> expectedEntry : expectedOutputRecordsMap.entrySet()) {
            String sinkName = expectedEntry.getKey();

            if (!actualOutputRecordsMap.containsKey(sinkName)) {
                return false;
            }

            List<Map<String, Object>> expectedRecords = expectedEntry.getValue();
            List<Map<String, Object>> actualRecords = actualOutputRecordsMap.get(sinkName);

            // both should be not null
            if (expectedRecords == null || actualRecords == null) {
                return false;
            } else if (expectedRecords.size() != actualRecords.size()) {
                return false;
            } else if (!expectedRecords.containsAll(actualRecords)) {
                return false;
            }
        }

        return true;
    }

}
