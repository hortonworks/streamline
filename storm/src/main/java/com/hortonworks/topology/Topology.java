package com.hortonworks.topology;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import com.hortonworks.bolt.AddDeviceIdAndVersionHeaderBolt;
import com.hortonworks.bolt.ParserBolt;
import com.hortonworks.bolt.PrinterBolt;
import com.hortonworks.spout.NestSpout;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;

/**
 * Created by pbrahmbhatt on 8/19/15.
 */
public class Topology {

    public static void main(String[] args) throws Exception {
        TopologyBuilder builder = new TopologyBuilder();

        final String zkUrl = "localhost:2181";
        final String topic = "nest-topic";
        final String zkRoot = "/Iotas-kafka-spout";
        final String id = "nest-kafka-spout";
        ZkHosts hosts = new ZkHosts(zkUrl);
        SpoutConfig config = new SpoutConfig(hosts, topic, zkRoot, id);
        KafkaSpout spout = new KafkaSpout(config);
        builder.setSpout("KafkaSpout", spout);
        builder.setBolt("ParserBolt", new ParserBolt()).shuffleGrouping("KafkaSpout");
        builder.setBolt("PrinterBolt", new PrinterBolt()).shuffleGrouping("ParserBolt");

        Config conf = new Config();
        conf.setDebug(true);
        conf.put(ParserBolt.CATALOG_ROOT_URL, "http://localhost:8080/api/v1/catalog");
        conf.put(ParserBolt.LOCAL_PARSER_JAR_PATH, "/tmp");

        if (args != null && args.length > 0) {
            StormSubmitter.submitTopologyWithProgressBar(args[0], conf, builder.createTopology());
        }
        else {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("NestTopology", conf, builder.createTopology());
            Thread.sleep(10000);
            //cluster.shutdown();
        }
    }
}
