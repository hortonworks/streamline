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

package com.hortonworks.streamline.streams.runtime.storm.bolt;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.runtime.storm.event.correlation.EventCorrelatingOutputCollector;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.utils.TupleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 *
 */
public abstract class AbstractProcessorBolt extends BaseTickTupleAwareRichBolt {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractProcessorBolt.class);

    protected Map stormConf;
    protected TopologyContext context;
    protected OutputCollector collector;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.stormConf = stormConf;
        this.context = context;
        this.collector = new EventCorrelatingOutputCollector(context, collector);
    }

    @Override
    protected void process(Tuple tuple) {
        try {
            Object event = tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
            LOG.debug("Executing StreamlineEvent: [{}] with tuple: [{}]", event, tuple);

            if(event instanceof StreamlineEvent) {
                process(tuple, (StreamlineEvent) event);
            } else {
                LOG.debug("Received invalid input tuple:[{}] with streamline event:[{}] and it is not processed.", tuple, event);
            }

            collector.ack(tuple);
        } catch(Exception e) {
            LOG.error("Error occurred while processing the tuple", e);
            collector.fail(tuple);
            collector.reportError(e);
        }
    }

    protected abstract void process(Tuple inputTuple, StreamlineEvent event) throws Exception;

}
