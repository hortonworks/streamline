package com.hortonworks.topology;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import com.google.common.collect.Lists;
import com.hortonworks.bolt.ParserBolt;
import com.hortonworks.bolt.PrinterBolt;
import org.yaml.snakeyaml.Yaml;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class IotasTopology {
    private static final String KAFKA_SPOUT_ZK_URL = "kafka.spout.zkUrl";
    private static final String KAFKA_SPOUT_TOPIC = "kafka.spout.topic";
    private static final String KAFKA_SPOUT_ZK_ROOT = "kafka.spout.zkRoot";
    private static final String KAFKA_SPOUT_ID = "kafka.spout.id";
    private static final String PARSER_OUTPUT_FIELDS = "parser.output.fields";
    private static final String CATALOG_ROOT_URL = "catalog.root.url";
    private static final String PARSER_JAR_PATH = "parser.jar.path";

    public static void main(String[] args) throws Exception {
        Map<String, Object> configuration = getConfiguration(args[0]);

        TopologyBuilder builder = new TopologyBuilder();

        final String zkUrl = (String) configuration.get(KAFKA_SPOUT_ZK_URL);
        final String topic = (String) configuration.get(KAFKA_SPOUT_TOPIC);
        final String zkRoot = (String) configuration.get(KAFKA_SPOUT_ZK_ROOT);
        final String id = (String) configuration.get(KAFKA_SPOUT_ID);
        final Fields outputFields = new Fields((List<String>) configuration.get(PARSER_OUTPUT_FIELDS));

        ZkHosts hosts = new ZkHosts(zkUrl);
        SpoutConfig config = new SpoutConfig(hosts, topic, zkRoot, id);
        //config.ignoreZkOffsets = true;
        KafkaSpout spout = new KafkaSpout(config);

        builder.setSpout("KafkaSpout", spout);
        builder.setBolt("ParserBolt", new ParserBolt(outputFields)).shuffleGrouping("KafkaSpout");
        builder.setBolt("PrinterBolt", new PrinterBolt()).shuffleGrouping("ParserBolt");

        Config conf = new Config();
        conf.setDebug(true);
        conf.put(ParserBolt.CATALOG_ROOT_URL, configuration.get(CATALOG_ROOT_URL));
        conf.put(ParserBolt.LOCAL_PARSER_JAR_PATH, configuration.get(PARSER_JAR_PATH));

        if (args != null && args.length > 1) {
            StormSubmitter.submitTopologyWithProgressBar(args[1], conf, builder.createTopology());
        }
        else {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("NestTopology", conf, builder.createTopology());
            Thread.sleep(10000);
            //cluster.shutdown();
        }
    }

    private static Map<String, Object> getConfiguration(String arg) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream in = new FileInputStream(arg);
        return (Map<String, Object>) yaml.load(in);
    }
}
