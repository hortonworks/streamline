package com.hortonworks.iotas.spout;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

import java.util.Map;
import java.util.Random;

/**
 * Created by pbrahmbhatt on 7/30/15.
 */
public class NestSpout extends BaseRichSpout {
    private SpoutOutputCollector _collector;

    private transient Random random;

    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        _collector = collector;
        random = new Random();
    }

    @Override
    public void nextTuple() {
        Utils.sleep(100);
        long userId = random.nextLong();
        long temperature = random.nextLong() % 150l;
        long evenTime = System.currentTimeMillis();
        long longitude = random.nextLong();
        long latitude = random.nextLong();

        _collector.emit(new Values(new NestMessage(userId, temperature, evenTime, longitude, latitude).serialize()));
    }

    @Override
    public void ack(Object id) {
    }

    @Override
    public void fail(Object id) {
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("bytes"));
    }
}
