package com.hortonworks.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a dummy bolt to just send some notifications for testing the Notifications.
 */
public class DummyRuleBolt extends BaseRichBolt {

    public static final String IOTAS_NOTIFICATION = "iotas.notification";

    private OutputCollector collector;


    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public void execute(Tuple input) {
        IotasEvent event = (IotasEvent) input.getValueByField(ParserBolt.IOTAS_EVENT);
        List<String> eventIds = Arrays.asList(event.getId());
        List<String> dataSourceIds = Arrays.asList(event.getDataSourceId());

        // Create a dummy Notification object
        Map<String, String> fieldsMap = new HashMap<>();
        fieldsMap.put("temperature", "100");
        fieldsMap.put("humidity", "100");
        Notification notification = new NotificationImpl
                .Builder(fieldsMap)
                .eventIds(eventIds)
                .dataSourceIds(dataSourceIds)
                .notifierName("console_notifier")
                .timestamp(System.currentTimeMillis())
                .ruleId("1")
                .build();
        collector.emit(new Values(notification));
        collector.ack(input);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(IOTAS_NOTIFICATION));
    }
}
