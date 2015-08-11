package com.hortonworks.topology;


import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import com.hortonworks.bolt.AddDeviceIdAndVersionHeaderBolt;
import com.hortonworks.bolt.ParserBolt;
import com.hortonworks.bolt.PrinterBolt;
import com.hortonworks.spout.NestSpout;

public class NestTopology {

    public static void main(String[] args) throws Exception {
        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("NestSpout", new NestSpout());

        builder.setBolt("AddDeviceIdAndVersionHeader", new AddDeviceIdAndVersionHeaderBolt()).shuffleGrouping("NestSpout");
        builder.setBolt("ParserBolt", new ParserBolt()).shuffleGrouping("AddDeviceIdAndVersionHeader");
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
