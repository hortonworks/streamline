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
package com.hortonworks.streamline.streams.runtime.storm.bolt.normalization;

import com.hortonworks.streamline.streams.layout.component.impl.normalization.NormalizationProcessor;
import com.hortonworks.streamline.streams.runtime.storm.layout.runtime.rule.topology.RulesTestSinkBolt;
import com.hortonworks.streamline.streams.runtime.storm.layout.runtime.rule.topology.RulesTestSpout;
import org.apache.storm.Config;
import org.apache.storm.ILocalCluster;
import org.apache.storm.LocalCluster;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.topology.TopologyBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to run storm topology containing NormalizationProcessor.
 */
@Ignore
public class NormalizationTopologyTest {
    private static final Logger log = LoggerFactory.getLogger(NormalizationTopologyTest.class);

    private static final String OUTPUT_STREAM_ID = "normalized-output";
    private static final String RULES_TEST_SPOUT = "rules-test-spout";
    private static final String NORMALIZATION_BOLT = "split-bolt";
    private static final String SINK_BOLT = "sink-bolt";

    @Test
    public void testBulkNormalizationTopology() throws Exception {
        testNormalizationTopology(NormalizationBoltTest.createBulkNormalizationProcessor(OUTPUT_STREAM_ID));
    }

    @Test
    public void testFieldBasedNormalizationTopology() throws Exception {
        testNormalizationTopology(NormalizationBoltTest.createFieldBasedNormalizationProcessor(OUTPUT_STREAM_ID));
    }


    public void testNormalizationTopology(NormalizationProcessor normalizationProcessor) throws Exception {

        final Config config = new Config();
        config.setDebug(true);
        final String topologyName = "SplitJoinTopologyTest";
        final StormTopology topology = createTopology(normalizationProcessor);
        log.info("Created topology with name: [{}] and topology: [{}]", topologyName, topology);

        ILocalCluster localCluster = new LocalCluster();
        log.info("Submitting topology: [{}]", topologyName);
        localCluster.submitTopology(topologyName, config, topology);
        Thread.sleep(2000);
        localCluster.shutdown();
    }


    protected StormTopology createTopology(NormalizationProcessor normalizationProcessor) {
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout(RULES_TEST_SPOUT, new RulesTestSpout(2000));
        builder.setBolt(NORMALIZATION_BOLT, new NormalizationBolt(normalizationProcessor)).shuffleGrouping(RULES_TEST_SPOUT, NormalizationProcessor.DEFAULT_STREAM_ID);
        builder.setBolt(SINK_BOLT, new RulesTestSinkBolt()).shuffleGrouping(NORMALIZATION_BOLT, OUTPUT_STREAM_ID);
        return builder.createTopology();
    }

}
