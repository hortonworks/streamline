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
package org.apache.streamline.streams.layout.component;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * unit tests for TopologyDag
 */
public class TopologyDagTest {
    TopologyDag topology;

    @Before
    public void setUp() {
        topology = new TopologyDag();
    }

    @Test
    public void testAddEdge() throws Exception {
        Source src = new StreamlineSource(Collections.singleton(new Stream("f1", "f2")));
        Sink sink = new StreamlineSink();
        Processor processor = new StreamlineProcessor(Collections.singleton(new Stream("r1")));

        topology.add(src).add(processor).add(sink);
        topology.addEdge(src, processor);
        topology.addEdge(processor, sink);

        assertEquals(1, topology.getEdgesFrom(src).size());
        assertEquals(src, topology.getEdgesFrom(src).get(0).getFrom());
        assertEquals(processor, topology.getEdgesFrom(src).get(0).getTo());

        assertEquals(1, topology.getEdgesFrom(processor).size());
        assertEquals(processor, topology.getEdgesFrom(processor).get(0).getFrom());
        assertEquals(sink, topology.getEdgesFrom(processor).get(0).getTo());

        assertEquals(2, topology.getEdges(processor).size());

        assertEquals(0, topology.getEdgesTo(src).size());
        assertEquals(0, topology.getEdgesFrom(sink).size());
    }

    @Test
    public void testGetAllEdges() throws Exception {
        Source src = new StreamlineSource(Collections.singleton(new Stream("f1", "f2")));
        Sink sink = new StreamlineSink();
        Processor processor = new StreamlineProcessor(Collections.singleton(new Stream("r1")));

        topology.add(src).add(processor).add(sink);
        topology.addEdge(src, processor);
        topology.addEdge(processor, sink);

        assertEquals(Sets.newHashSet(src, processor), topology.getOutputComponents());
        assertEquals(Sets.newHashSet(sink, processor), topology.getInputComponents());

        assertEquals(2, topology.getAllEdges().size());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSource() throws Exception {
        Source src = new StreamlineSource(Collections.singleton(new Stream("f1", "f2")));
        Sink sink = new StreamlineSink();
        Processor processor = new StreamlineProcessor(Collections.singleton(new Stream("r1")));

        topology.addEdge(src, processor);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSink() throws Exception {
        Source src = new StreamlineSource(Collections.singleton(new Stream("f1", "f2")));
        Sink sink = new StreamlineSink();
        Processor processor = new StreamlineProcessor(Collections.singleton(new Stream("r1")));

        topology.add(src);
        topology.addEdge(src, processor);
    }

    @Test
    public void testRemoveEdge() throws Exception {
        Source src = new StreamlineSource(Collections.singleton(new Stream("f1", "f2")));
        Sink sink = new StreamlineSink();
        Processor processor = new StreamlineProcessor(Collections.singleton(new Stream("r1")));

        topology.add(src).add(processor).add(sink);
        topology.addEdge(src, processor);
        topology.addEdge(processor, sink);

        assertEquals(2, topology.getEdges(processor).size());
        topology.removeEdge(processor, sink);
        assertEquals(1, topology.getEdges(processor).size());
        assertEquals(1, topology.getEdges(src).size());
        assertEquals(0, topology.getEdges(sink).size());
    }

    @Test
    public void testAddEdgeStreamId() throws Exception {
        Stream stream1 = new Stream("f1", "f2");
        Stream stream2 = new Stream("f3", "f4");
        Stream stream3 = new Stream("r1");
        Source src = new StreamlineSource(Sets.newHashSet(stream1, stream2));
        Sink sink = new StreamlineSink();
        Processor processor = new StreamlineProcessor(Collections.singleton(stream3));

        topology.add(src).add(processor).add(sink);
        topology.addEdge(src, processor, stream2.getId());
        topology.addEdge(processor, sink);

        assertEquals(1, topology.getEdges(src).size());
        assertEquals(2, topology.getEdges(processor).size());
        assertEquals(1, topology.getEdges(sink).size());

        assertEquals(topology.getEdgesFrom(src), topology.getEdgesTo(processor));
        assertEquals(stream2, topology.getEdgesTo(processor).get(0).getStreamGroupings().iterator().next().getStream());
    }

    @Test
    public void testTopOrder() {
        Source src1 = new StreamlineSource(Collections.singleton(new Stream("f1", "f2")));
        Source src2 = new StreamlineSource(Collections.singleton(new Stream("f1", "f2")));
        Sink sink = new StreamlineSink();
        Processor processor = new StreamlineProcessor(Collections.singleton(new Stream("r1")));

        topology.add(src1).add(src2).add(processor).add(sink);
        topology.addEdge(src1, processor);
        topology.addEdge(src2, processor);
        topology.addEdge(processor, sink);

        List<Component> topOrder = topology.topOrder();
        assertArrayEquals(new Component[] {src2, src1, processor, sink}, topOrder.toArray());
    }


    @Test(expected =  IllegalStateException.class)
    public void testTopOrderCycle() {
        Source src1 = new StreamlineSource(Collections.singleton(new Stream("f1", "f2")));
        Processor processor1 = new StreamlineProcessor(Collections.singleton(new Stream("r1")));
        Processor processor2 = new StreamlineProcessor(Collections.singleton(new Stream("r1")));

        topology.add(src1).add(processor1).add(processor2);
        topology.addEdge(src1, processor1);
        topology.addEdge(processor1, processor2);
        topology.addEdge(processor2, processor1);
        List<Component> topOrder = topology.topOrder();
    }
}