/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import com.hortonworks.iotas.common.IotasEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 *
 */
public abstract class AbstractProcessorBolt extends BaseRichBolt {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractProcessorBolt.class);

    protected Map stormConf;
    protected TopologyContext context;
    protected OutputCollector collector;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.stormConf = stormConf;
        this.context = context;
        this.collector = collector;
    }

    @Override
    public void execute(Tuple inputTuple) {
        try {
            Object iotasEvent = inputTuple.getValueByField(IotasEvent.IOTAS_EVENT);
            LOG.debug("Executing IotasEvent: [{}] with tuple: [{}]", iotasEvent, inputTuple);

            if(iotasEvent instanceof IotasEvent) {
                process(inputTuple, (IotasEvent) iotasEvent);
            } else {
                LOG.debug("Received invalid input tuple:[{}] with iotas event:[{}] and it is not processed.", inputTuple, iotasEvent);
            }

            collector.ack(inputTuple);
        } catch(Exception e) {
            LOG.error("Error occurred while processing the tuple", e);
            collector.fail(inputTuple);
            collector.reportError(e);
        }
    }

    protected abstract void process(Tuple inputTuple, IotasEvent iotasEvent) throws Exception;

}
