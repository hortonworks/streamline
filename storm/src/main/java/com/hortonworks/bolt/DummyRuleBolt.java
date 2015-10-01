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
    private static int EMAIL_NOTIFICATION_INTERVAL = 500; // after every 500 notifications

    private OutputCollector collector;

    private int count = 0;
    private String emailNotificationStream = "";
    private String consoleNotificationStream = "";

    public DummyRuleBolt withEmailNotificationStream(String stream) {
        this.emailNotificationStream = stream;
        return this;
    }

    public DummyRuleBolt withConsoleNotificationStream(String stream) {
        this.consoleNotificationStream = stream;
        return this;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public void execute(Tuple input) {
        IotasEvent event = (IotasEvent) input.getValueByField(IotasEvent.IOTAS_EVENT);
        List<String> eventIds = Arrays.asList(event.getId());
        List<String> dataSourceIds = Arrays.asList(event.getDataSourceId());

        if (!consoleNotificationStream.isEmpty()) {
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
            collector.emit(consoleNotificationStream, new Values(notification));
        }

        // Send an email every EMAIL_NOTIFICATION_INTERVAL
        if (++count % EMAIL_NOTIFICATION_INTERVAL == 0) {
            if (!emailNotificationStream.isEmpty()) {
                Map<String, String> fieldsMap = new HashMap<>();
                fieldsMap.put("body", "Too many notifications, count so far is " + count);
                Notification emailNotification = new NotificationImpl
                        .Builder(fieldsMap)
                        .eventIds(eventIds)
                        .dataSourceIds(dataSourceIds)
                        .notifierName("email_notifier")
                        .timestamp(System.currentTimeMillis())
                        .ruleId("2")
                        .build();
                collector.emit(emailNotificationStream, new Values(emailNotification));
            }
        }
        collector.ack(input);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        if (!consoleNotificationStream.isEmpty()) {
            declarer.declareStream(consoleNotificationStream, new Fields(IOTAS_NOTIFICATION));
        }
        if (!emailNotificationStream.isEmpty()) {
            declarer.declareStream(emailNotificationStream, new Fields(IOTAS_NOTIFICATION));
        }
    }
}
