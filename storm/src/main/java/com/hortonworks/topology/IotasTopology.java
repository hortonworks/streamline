package com.hortonworks.topology;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import com.google.common.collect.Lists;
import com.hortonworks.bolt.ParserBolt;
import com.hortonworks.bolt.PrinterBolt;
import com.hortonworks.hbase.ParserOutputHBaseMapper;
import org.apache.storm.hbase.bolt.HBaseBolt;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.bolt.mapper.SimpleHBaseMapper;
import org.yaml.snakeyaml.Yaml;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IotasTopology {
    private static final String KAFKA_SPOUT_ZK_URL = "kafka.spout.zkUrl";
    private static final String KAFKA_SPOUT_TOPIC = "kafka.spout.topic";
    private static final String KAFKA_SPOUT_ZK_ROOT = "kafka.spout.zkRoot";
    private static final String KAFKA_SPOUT_ID = "kafka.spout.id";
    private static final String HBASE_TABLE = "hbase.table";
    private static final String HBASE_ROW_KEY = "hbase.row.key";
    private static final String HBASE_COLUMN_FAMILY = "hbase.column.family";
    private static final String PARSER_ID = "parserId";
    private static final String CATALOG_ROOT_URL = "catalog.root.url";
    private static final String PARSER_JAR_PATH = "parser.jar.path";
    private static final String HDFS_FSURL = "hdfs.fsUrl";
    private static final String HDFS_PATH = "hdfs.path";
    private static final String HDFS_NAME = "hdfs.name";
    public static final String HBASE_CONF = "hbase.conf";

    public static void main(String[] args) throws Exception {
        Map<String, Object> configuration = getConfiguration(args[0]);

        TopologyBuilder builder = new TopologyBuilder();

        final String zkUrl = (String) configuration.get(KAFKA_SPOUT_ZK_URL);
        final String topic = (String) configuration.get(KAFKA_SPOUT_TOPIC);
        final String zkRoot = (String) configuration.get(KAFKA_SPOUT_ZK_ROOT);
        final String id = (String) configuration.get(KAFKA_SPOUT_ID);
        final Long parserId = configuration.containsKey(PARSER_ID) ? (Long) configuration.get(PARSER_ID) : null;

        ZkHosts hosts = new ZkHosts(zkUrl);
        SpoutConfig config = new SpoutConfig(hosts, topic, zkRoot, id);
        //config.ignoreZkOffsets = true;
        KafkaSpout spout = new KafkaSpout(config);

        Map hbaseConf = new HashMap();

        hbaseConf.put("hbase.root.dir", "hdfs://localhost:9000/hbase");
        HBaseMapper mapper = new ParserOutputHBaseMapper((String) configuration.get(HBASE_ROW_KEY), (String) configuration.get(HBASE_COLUMN_FAMILY));
        HBaseBolt hBaseBolt = new HBaseBolt(configuration.get(HBASE_TABLE).toString(), mapper)
                .withConfigKey(HBASE_CONF);

        ParserBolt parserBolt = new ParserBolt();
        if (parserId != null) {
            parserBolt.withParserId(parserId);
        }

        UnparsedTupleHandler unparsedTupleHandler = new
                HdfsUnparsedTupleHandler().withFsUrl((String) configuration
                .get(HDFS_FSURL)).withPath((String) configuration.get
                (HDFS_PATH)).withName((String) configuration.get(HDFS_NAME))
                .withRotationInterval(1);
        parserBolt.withUnparsedTupleHandler(unparsedTupleHandler);

        builder.setSpout("KafkaSpout", spout);
        builder.setBolt("ParserBolt", parserBolt).shuffleGrouping("KafkaSpout");
        builder.setBolt("PrinterBolt", new PrinterBolt()).shuffleGrouping("ParserBolt");
        builder.setBolt("HBaseBolt", hBaseBolt).shuffleGrouping("ParserBolt");

        Config conf = new Config();
        conf.setDebug(true);
        conf.put(ParserBolt.CATALOG_ROOT_URL, configuration.get(CATALOG_ROOT_URL));
        conf.put(ParserBolt.LOCAL_PARSER_JAR_PATH, configuration.get(PARSER_JAR_PATH));
        conf.put(HBASE_CONF, hbaseConf);

        if (args != null && args.length > 1) {
            StormSubmitter.submitTopologyWithProgressBar(args[1], conf, builder.createTopology());
        } else {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("NestTopology", conf, builder.createTopology());
        }
    }

    private static Map<String, Object> getConfiguration(String arg) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream in = new FileInputStream(arg);
        return (Map<String, Object>) yaml.load(in);
    }
}
