package com.hortonworks.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import java.nio.ByteBuffer;
import java.util.Map;


public class AddDeviceIdAndVersionHeaderBolt extends BaseRichBolt {
    private OutputCollector collector;
    private static Header header = new Header("nest", 1l);

    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }

    //Just Adds a static HEADER to the message it receives.
    public void execute(Tuple input) {
        try {
            byte[] bytes = input.getBinaryByField("bytes");
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length + header.size());
            header.writeHeader(buffer);
            buffer.put(bytes);
            collector.emit(input, new Values(buffer.array()));
            collector.ack(input);
        } catch (Exception e) {
            collector.fail(input);
            collector.reportError(e);
        }
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("bytes"));

    }
}
