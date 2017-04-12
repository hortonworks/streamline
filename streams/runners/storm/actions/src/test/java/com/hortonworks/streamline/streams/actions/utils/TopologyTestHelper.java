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
package com.hortonworks.streamline.streams.actions.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

public class TopologyTestHelper {

    private TopologyTestHelper() {
    }

    public static Topology createTopology(Long id, Long versionId, Long namespaceId) {
        Topology topology = new Topology();
        topology.setId(id);
        topology.setName("testTopology_" + id);
        topology.setNamespaceId(namespaceId);
        topology.setVersionId(versionId);
        topology.setVersionTimestamp(System.currentTimeMillis());
        return topology;
    }

    public static StreamlineSource createStreamlineSource(String id) {
        Stream stream = createDefaultStream();
        StreamlineSource source = new StreamlineSource(Sets.newHashSet(stream));
        source.setId(id);
        source.setName("testSource_" + id);
        source.setConfig(new Config());
        source.setTransformationClass("dummyTransformation");
        return source;
    }

    public static StreamlineProcessor createStreamlineProcessor(String id) {
        Stream stream = createDefaultStream();
        StreamlineProcessor processor = new StreamlineProcessor(Sets.newHashSet(stream));
        processor.setId(id);
        processor.setName("testProcessor_" + id);
        processor.setConfig(new Config());
        processor.setTransformationClass("dummyTransformation");
        return processor;
    }

    public static RulesProcessor createRulesProcessor(String id) {
        RulesProcessor processor = new RulesProcessor();
        processor.setId(id);
        processor.setName("testRuleProcessor_" + id);
        processor.setConfig(new Config());
        processor.setTransformationClass("dummyTransformation");
        processor.setProcessAll(true);
        processor.setRules(Collections.emptyList());
        return processor;
    }

    public static StreamlineSink createStreamlineSink(String id) {
        StreamlineSink sink = new StreamlineSink();
        sink.setId(id);
        sink.setName("testSink_" + id);
        sink.setConfig(new Config());
        sink.setTransformationClass("dummyTransformation");
        return sink;
    }

    public static List<Map<String, Object>> createTestRecords() {
        List<Map<String, Object>> testRecords = new ArrayList<>();

        Map<String, Object> testRecord1 = new HashMap<>();
        testRecord1.put("A", 1);
        testRecord1.put("B", 2);

        Map<String, Object> testRecord2 = new HashMap<>();
        testRecord2.put("A", 1);
        testRecord2.put("B", 2);

        testRecords.add(testRecord1);
        testRecords.add(testRecord2);

        return testRecords;
    }

    public static Stream createDefaultStream() {
        Schema.Field field = Schema.Field.of("A", Schema.Type.INTEGER);
        Schema.Field field2 = Schema.Field.of("B", Schema.Type.INTEGER);
        return new Stream("default", Lists.newArrayList(field, field2));
    }

    public static Map<String, List<Map<String, Object>>> createTestOutputRecords(Set<String> sinkNames) {
        return sinkNames.stream()
                .collect(toMap(s -> s, s -> createTestRecords()));
    }

}
