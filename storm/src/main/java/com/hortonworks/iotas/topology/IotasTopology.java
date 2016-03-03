package com.hortonworks.iotas.topology;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.topology.TopologyBuilder;
import com.hortonworks.iotas.bolt.ParserBolt;
import com.hortonworks.iotas.bolt.PrinterBolt;
import com.hortonworks.iotas.hbase.ParserOutputHBaseMapper;
import com.hortonworks.iotas.hdfs.IdentityHdfsRecordFormat;
import org.apache.storm.hbase.bolt.HBaseBolt;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.TimedRotationPolicy;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.yaml.snakeyaml.Yaml;
import org.apache.storm.kafka.KafkaSpout;
import org.apache.storm.kafka.SpoutConfig;
import org.apache.storm.kafka.ZkHosts;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static com.hortonworks.iotas.bolt.ParserBolt.PARSED_TUPLES_STREAM;
import static com.hortonworks.iotas.bolt.ParserBolt.FAILED_TO_PARSE_TUPLES_STREAM;

public class IotasTopology {
    private static final String KAFKA_SPOUT_ZK_URL = "kafka.spout.zkUrl";
    private static final String KAFKA_SPOUT_TOPIC = "kafka.spout.topic";
    private static final String KAFKA_SPOUT_ZK_ROOT = "kafka.spout.zkRoot";
    private static final String KAFKA_SPOUT_ID = "kafka.spout.id";
    private static final String HBASE_TABLE = "hbase.table";
    private static final String HBASE_ROW_KEY = "hbase.row.key";
    private static final String HBASE_COLUMN_FAMILY = "hbase.column.family";
    private static final String HBASE_ROOT_DIR = "hbase.root.dir";
    private static final String PARSER_ID = "parserId";
    private static final String CATALOG_ROOT_URL = "catalog.root.url";
    private static final String PARSER_JAR_PATH = "parser.jar.path";
    private static final String HDFS_FSURL = "hdfs.fsUrl";
    private static final String HDFS_PATH = "hdfs.path";
    private static final String HDFS_NAME = "hdfs.name";
    private static final String HDFS_ROTATION_INTERVAL = "hdfs" +
            ".rotationInterval";
    private static final String HDFS_SYNC_POLICY_COUNT = "hdfs.syncPolicyCount";
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

        hbaseConf.put("hbase.rootdir", configuration.get(HBASE_ROOT_DIR));
        HBaseMapper mapper = new ParserOutputHBaseMapper((String) configuration.get(HBASE_COLUMN_FAMILY));
        HBaseBolt hBaseBolt = new HBaseBolt(configuration.get(HBASE_TABLE).toString(), mapper)
                .withConfigKey(HBASE_CONF);

        ParserBolt parserBolt = new ParserBolt();
        if (parserId != null) {
            parserBolt.withParserId(parserId);
        }
        parserBolt.withParsedTuplesStreamId(PARSED_TUPLES_STREAM);
        parserBolt.withUnparsedTuplesStreamId(FAILED_TO_PARSE_TUPLES_STREAM);

        String fsUrl = (String) configuration.get(HDFS_FSURL);
        String path =  (String) configuration.get(HDFS_PATH);
        String name = (String) configuration.get(HDFS_NAME);
        Integer rotationInterval = (Integer) configuration.get
                (HDFS_ROTATION_INTERVAL);
        Integer syncPolicyCount = (Integer) configuration.get
                (HDFS_SYNC_POLICY_COUNT);

        RecordFormat recordFormat = new IdentityHdfsRecordFormat();
        SyncPolicy syncPolicy = new CountSyncPolicy(syncPolicyCount);
        FileRotationPolicy fileRotationPolicy = new TimedRotationPolicy
                (rotationInterval, TimedRotationPolicy.TimeUnit.SECONDS);
        FileNameFormat fileNameFormat = new DefaultFileNameFormat().withPath
                (path).withPrefix(name);
        HdfsBolt hdfsBolt = new HdfsBolt();
        hdfsBolt.withFsUrl(fsUrl).withFileNameFormat(fileNameFormat)
                .withRecordFormat(recordFormat).withRotationPolicy
                (fileRotationPolicy).withSyncPolicy(syncPolicy);

        builder.setSpout("KafkaSpout", spout);
        builder.setBolt("ParserBolt", parserBolt).shuffleGrouping("KafkaSpout");
        builder.setBolt("PrinterBolt", new PrinterBolt()).shuffleGrouping
                ("ParserBolt", PARSED_TUPLES_STREAM);
        builder.setBolt("HBaseBolt", hBaseBolt).shuffleGrouping("ParserBolt",
                PARSED_TUPLES_STREAM);
        builder.setBolt("HdfsBolt", hdfsBolt).shuffleGrouping("ParserBolt",
                FAILED_TO_PARSE_TUPLES_STREAM);

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
