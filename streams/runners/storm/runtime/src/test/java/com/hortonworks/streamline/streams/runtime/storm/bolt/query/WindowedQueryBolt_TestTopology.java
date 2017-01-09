package com.hortonworks.streamline.streams.runtime.storm.bolt.query;

import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.generated.Nimbus;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.NimbusClient;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/* Sample topology for WindowedQueryBolt */
public class WindowedQueryBolt_TestTopology {
    private static final Logger log = LoggerFactory.getLogger(WindowedQueryBolt_TestTopology.class);
    public static final String TOPO_NAME = "joinTopology";

    public static void main(String[] args) throws Exception {

        SquaresSpout squares = new SquaresSpout();
        CubesSpout cubes = new CubesSpout();


        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("squares", squares, 1);
        builder.setSpout("cubes", cubes, 1);

        BaseWindowedBolt joiner = new WindowedQueryBolt(WindowedQueryBolt.StreamSelector.SOURCE, "cubes", "number")
                .leftJoin("squares", "number", "cubes")
                .select("number,square,cube")
                .withTumblingWindow(BaseWindowedBolt.Count.of(1000))
                ;

        builder.setBolt("joiner", joiner, 2)
                .fieldsGrouping("squares", new Fields("number") )
                .fieldsGrouping("cubes", new Fields("number") )
        ;


        builder.setBolt("fileWrite", new FileWriteBolt(), 1).shuffleGrouping("joiner");

        // - submit topo

//        LocalCluster cluster = runOnLocalCluster(TOPO_NAME, builder.createTopology() );
//        Thread.sleep(10 * 60*1000);
//        System.err.println("Shutting down");
//        cluster.killTopology(TOPO_NAME);
//        cluster.shutdown();

    } // main

    private static Nimbus.Client runOnStormCluster(Config conf, StormTopology topology) throws AlreadyAliveException, InvalidTopologyException, AuthorizationException {
        Map clusterConf = Utils.readStormConfig();
        StormSubmitter.submitTopologyWithProgressBar(TOPO_NAME, conf, topology);
        return NimbusClient.getConfiguredClient(clusterConf).getClient();
    }

    /** generates a number and its square
     */
    static class SquaresSpout  extends BaseRichSpout {
        long i = 0;
        private SpoutOutputCollector collector;

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare( new Fields("number","square") );
        }

        @Override
        public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
            this.collector = collector;
        }

        @Override
        public void nextTuple() {
            collector.emit("squareStream", new Values(i,i*i), Long.toString(i));
            ++i;
        }
    } // class SquaresSpout

    /** generates a number and its cube
     */
    static class CubesSpout  extends BaseRichSpout {
        long i = 0;
        private SpoutOutputCollector collector;

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare( new Fields("number","cube") );
        }

        @Override
        public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
            this.collector = collector;
        }

        @Override
        public void nextTuple() {
            collector.emit("cubeStream", new Values(i,i*i*i), Long.toString(i));
            ++i;
        }
    }// class Cubes Spout


    static class FileWriteBolt extends BaseRichBolt {

        File resultFile = new File("/tmp/joined.txt");
        BufferedWriter writer = null;
        @Override
        public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
            if(resultFile.exists())
                resultFile.delete();
            try {
                writer = new BufferedWriter(new FileWriter(resultFile));
            } catch (IOException e) {
                log.error("Problem opening file", e);
            }

        }

        @Override
        public void execute(Tuple input) {
            TupleImpl tuple = (TupleImpl) input;
            try {
                String line = tuple.getLong(0) + "," + tuple.getLong(1) + "," + tuple.getLong(2) + "\n";
                writer.write(line);
                System.err.print(line);
            } catch (IOException e) {
                log.error("Problem writing to file", e);
            }

        }

        @Override
        public void cleanup() {
            super.cleanup();
            try {
                writer.close();
            } catch (IOException e) {
                log.error("Problem closing file", e);
            }
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("dummy") );
        }
    }// FileWriteBolt

//    public static LocalCluster runOnLocalCluster(String topoName, StormTopology topology) {
//        LocalCluster cluster = new LocalCluster();
//        cluster.submitTopology(topoName, new Config(), topology);
//        return cluster;
//    }
}
