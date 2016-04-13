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

package com.hortonworks.iotas.layout.runtime.rule.topology;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import com.google.common.collect.Lists;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RulesTestSpout extends BaseRichSpout {
    protected static final Logger log = LoggerFactory.getLogger(RulesTestSpout.class);
    private final long tupleEmitInterval;

    private SpoutOutputCollector collector;

    public static final IotasEventImpl IOTAS_EVENT_1 = new IotasEventImpl(new HashMap<String, Object>() {{
        put("temperature", 101);
        put("humidity", 51);
    }}, "dataSrcId_1", "23");

    public static final IotasEventImpl IOTAS_EVENT_2 = new IotasEventImpl(new HashMap<String, Object>() {{
        put("temperature", 99);
        put("humidity", 49);
    }}, "dataSrcId_2", "24");

    private static final List<Values> LIST_VALUES = Lists.newArrayList(new Values(IOTAS_EVENT_1), new Values(IOTAS_EVENT_2));

    public RulesTestSpout() {
        this(100);
    }

    public RulesTestSpout(long tupleEmitInterval) {
        this.tupleEmitInterval = tupleEmitInterval;
    }

    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public void nextTuple() {
        Utils.sleep(tupleEmitInterval);
        final Random rand = new Random();
        final Values values = LIST_VALUES.get(rand.nextInt(LIST_VALUES.size()));
        log.debug("++++++++ Emitting Tuple: [{}]", values);
        collector.emit(values);
        Thread.yield();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        log.debug("++++++++ DECLARING OUTPUT FIELDS");
        declarer.declare(getOutputFields());
    }

    public Fields getOutputFields() {
        return new Fields(IotasEvent.IOTAS_EVENT);
    }

    @Override
    public void close() {
        super.close();
    }
}
