package com.hortonworks.bolt;

import com.hortonworks.iotas.layout.runtime.transform.AddHeaderTransform;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;

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
            Map<String, Object> fieldsMap = new HashMap<>();
            fieldsMap.put("temperature", "100");
            fieldsMap.put("humidity", "100");

            Map<String, Object> header = new HashMap<>();
            header.put(AddHeaderTransform.HEADER_FIELD_EVENT_IDS, eventIds);
            header.put(AddHeaderTransform.HEADER_FIELD_DATASOURCE_IDS, dataSourceIds);
            header.put(AddHeaderTransform.HEADER_FIELD_NOTIFIER_NAME, "console_notifier");
            header.put(AddHeaderTransform.HEADER_FIELD_RULE_ID, "1");
            header.put(AddHeaderTransform.HEADER_FIELD_TIMESTAMP, System.currentTimeMillis());

            IotasEvent iotasEvent = new IotasEventImpl(fieldsMap, "dummyRuleBolt", header);
            collector.emit(consoleNotificationStream, new Values(iotasEvent));
        }

        // Send an email every EMAIL_NOTIFICATION_INTERVAL
        if (++count % EMAIL_NOTIFICATION_INTERVAL == 0) {
            if (!emailNotificationStream.isEmpty()) {
                Map<String, Object> fieldsMap = new HashMap<>();
                fieldsMap.put("body", "Too many notifications, count so far is " + count);
                Map<String, Object> header = new HashMap<>();
                header.put(AddHeaderTransform.HEADER_FIELD_EVENT_IDS, eventIds);
                header.put(AddHeaderTransform.HEADER_FIELD_DATASOURCE_IDS, dataSourceIds);
                header.put(AddHeaderTransform.HEADER_FIELD_NOTIFIER_NAME, "email_notifier");
                header.put(AddHeaderTransform.HEADER_FIELD_RULE_ID, "1");
                header.put(AddHeaderTransform.HEADER_FIELD_TIMESTAMP, System.currentTimeMillis());

                IotasEvent iotasEvent = new IotasEventImpl(fieldsMap, "dummyRuleBolt", header);

                collector.emit(emailNotificationStream, new Values(iotasEvent));
            }
        }
        collector.ack(input);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        if (!consoleNotificationStream.isEmpty()) {
            declarer.declareStream(consoleNotificationStream, new Fields(IotasEvent.IOTAS_EVENT));
        }
        if (!emailNotificationStream.isEmpty()) {
            declarer.declareStream(emailNotificationStream, new Fields(IotasEvent.IOTAS_EVENT));
        }
    }
}
