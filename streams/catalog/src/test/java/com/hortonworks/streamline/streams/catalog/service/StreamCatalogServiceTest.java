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
package com.hortonworks.streamline.streams.catalog.service;

import com.hortonworks.registries.common.util.FileStorage;
import com.hortonworks.streamline.registries.model.client.MLModelRegistryClient;
import com.hortonworks.registries.storage.StorableKey;
import com.hortonworks.registries.storage.StorageManager;
import com.hortonworks.streamline.streams.catalog.Projection;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyVersion;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class StreamCatalogServiceTest {

    @Tested
    private StreamCatalogService streamCatalogService;

    @Injectable
    private StorageManager dao;

    @Injectable
    private FileStorage fileStorage;

    @Injectable
    private MLModelRegistryClient modelRegistryClient;

    @Injectable
    private Map<String, Object> configuration;

    @Test
    public void testListTopologiesProvidesVersionedTimestamp_STREAMLINE_526() {
        List<Topology> topologies = new ArrayList<>();
        topologies.add(createTopology(1L));
        topologies.add(createTopology(2L));
        topologies.add(createTopology(3L));

        List<TopologyVersion> versions = topologies.stream()
                .map(x -> createTopologyVersionInfo(x.getId(), x.getId()))
                .collect(Collectors.toList());

        new Expectations() {{
            dao.find(withEqual(new Topology().getNameSpace()), withAny(new ArrayList<>()));
            result = topologies;

            dao.find(withEqual(new TopologyVersion().getNameSpace()), withAny(new ArrayList<>()));
            result = versions;

            dao.get(withEqual(new StorableKey(versions.get(0).getNameSpace(), versions.get(0).getPrimaryKey())));
            result = versions.get(0);

            dao.get(withEqual(new StorableKey(versions.get(1).getNameSpace(), versions.get(1).getPrimaryKey())));
            result = versions.get(1);

            dao.get(withEqual(new StorableKey(versions.get(2).getNameSpace(), versions.get(2).getPrimaryKey())));
            result = versions.get(2);
        }};

        Collection<Topology> result = streamCatalogService.listTopologies();
        assertTrue(result.size() > 0);
        assertFalse(result.stream().anyMatch(x -> x.getVersionTimestamp() == null));
    }

    private Topology createTopology(long id) {
        Topology topology = new Topology();
        topology.setId(id);
        topology.setName("name" + id);
        topology.setVersionId(id);
        // not set timestamp
        return topology;
    }

    private Topology createTopology(String name) {
        Topology topology = new Topology();
        topology.setName(name);
        return topology;
    }

    private TopologyVersion createTopologyVersionInfo(Long id, Long topologyId) {
        TopologyVersion topologyVersion = new TopologyVersion();
        topologyVersion.setId(id);
        topologyVersion.setName("name" + id);
        topologyVersion.setTopologyId(topologyId);
        topologyVersion.setDescription("description" + id);
        topologyVersion.setTimestamp(System.currentTimeMillis());
        return topologyVersion;
    }

    @Test
    public void testGetCloneSuffix() {
        assertEquals("foo-clone", streamCatalogService.getNextCloneName("foo"));
        assertEquals("foo-clone2", streamCatalogService.getNextCloneName("foo-clone"));
        assertEquals("foo-clone3", streamCatalogService.getNextCloneName("foo-clone2"));
    }

    @Test
    public void testGetLatestCloneName() {
        List<Topology> topologies = new ArrayList<>();
        topologies.add(createTopology("foo"));
        topologies.add(createTopology("foo-clone"));
        topologies.add(createTopology("foo-clone2"));
        topologies.add(createTopology("foo-clone9"));
        topologies.add(createTopology("foo-clone10"));
        topologies.add(createTopology("bar"));
        assertEquals("foo-clone10", streamCatalogService.getLatestCloneName("foo", topologies).get());
    }

    @Test
    public void testConvertNested() {
        List<String> streams = Collections.singletonList("kafka_stream_1");
        String res = streamCatalogService.convertNested(streams, "f1.g.h = 'A' and  kafka_stream_1.f2[5].j = 100");
        System.out.println(res);
        assertEquals("f1['g']['h'] = 'A' and  kafka_stream_1.f2[5]['j'] = 100", res);

        res = streamCatalogService.convertNested(streams, "kafka_stream_1.f2.x.y = 100");
        assertEquals("kafka_stream_1.f2['x']['y'] = 100", res);

        res = streamCatalogService.convertNested(streams, "f1.f2.x.y = 100");
        assertEquals("f1['f2']['x']['y'] = 100", res);
    }

    @Test
    public void testgetSqlString() {
        List<String> streams = Collections.singletonList("kafka_stream_1");
        List<String> gbk = Collections.singletonList("a.b.c");
        List<Projection> projections = Collections.singletonList(new Projection("f1.g.h", null, null, null));
        String sql = streamCatalogService.getSqlString(streams, projections, "f1.a.b = kafka_stream_1.g.h", gbk);
        assertEquals("SELECT f1['g']['h'] FROM kafka_stream_1 WHERE f1['a']['b'] = kafka_stream_1.g['h'] GROUP BY a['b']['c']", sql);
        projections = Collections.singletonList(new Projection(null, "foo", Arrays.asList("a.b", "kafka_stream_1.c.d"), "res"));
        sql = streamCatalogService.getSqlString(streams, projections, "f1.a.b = kafka_stream_1.g.h", gbk);
        assertEquals("SELECT foo(a['b'],kafka_stream_1.c['d']) AS \"res\" FROM kafka_stream_1 WHERE f1['a']['b'] = kafka_stream_1.g['h'] GROUP BY a['b']['c']", sql);
    }

    @Test
    public void testtranslateFunctionsProjection() {
        String result;
        Map<String, String> table = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        table.put("FOO", "FOO_FN");
        table.put("foo2", "foo2_fn");

        result = streamCatalogService.translateFunctions("select foo(arg1), FOO(arg2), FOO (arg3) from stream",
                table);
        Assert.assertEquals("select FOO_FN(arg1), FOO_FN(arg2), FOO_FN (arg3) from stream", result);

        result = streamCatalogService.translateFunctions("select foo2(arg1, arg2), bar(arg2), BAZ(arg3, arg4) from stream",
                table);
        Assert.assertEquals("select foo2_fn(arg1, arg2), bar(arg2), BAZ(arg3, arg4) from stream", result);

    }

    @Test
    public void testtranslateFunctionsCondition() {
        String result;
        Map<String, String> table = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        table.put("FOO", "FOO_FN");
        table.put("foo2", "foo2_fn");
        table.put("UPPER", "UPPER");

        result = streamCatalogService.translateFunctions("select foo, foo1, foo2 from stream where FOO(a, b) = 'c'",
                table);
        Assert.assertEquals("select foo, foo1, foo2 from stream where FOO_FN(a, b) = 'c'", result);

        result = streamCatalogService.translateFunctions("select * from stream where FOO(a) = 'b' AND UPPER(a) = 'C'",
                table);
        Assert.assertEquals("select * from stream where FOO_FN(a) = 'b' AND UPPER(a) = 'C'", result);

    }
}