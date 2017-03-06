/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.runtime.storm.testing;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.runtime.storm.bolt.BaseTickTupleAwareRichBolt;
import com.hortonworks.streamline.streams.runtime.transform.AddHeaderTransformRuntime;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.TupleUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a dummy bolt to just send some notifications for testing the Notifications.
 */
public class NotificationsTestBolt extends BaseTickTupleAwareRichBolt {

    public static final String STREAMLINE_NOTIFICATION = "streamline.notification";
    private static final int EMAIL_NOTIFICATION_INTERVAL = 500; // after every 500 notifications

    private OutputCollector collector;

    private int count;
    private String emailNotificationStream = "";
    private String consoleNotificationStream = "";

    public NotificationsTestBolt withEmailNotificationStream(String stream) {
        this.emailNotificationStream = stream;
        return this;
    }

    public NotificationsTestBolt withConsoleNotificationStream(String stream) {
        this.consoleNotificationStream = stream;
        return this;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }

    @Override
    protected void process(Tuple input) {
        StreamlineEvent event = (StreamlineEvent) input.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        List<String> eventIds = Arrays.asList(event.getId());
        List<String> dataSourceIds = Arrays.asList(event.getDataSourceId());


        if (!consoleNotificationStream.isEmpty()) {
            // Create a dummy Notification object
            Map<String, Object> fieldsMap = new HashMap<>();
            fieldsMap.put("temperature", "100");
            fieldsMap.put("humidity", "100");

            Map<String, Object> header = new HashMap<>();
            header.put(AddHeaderTransformRuntime.HEADER_FIELD_EVENT_IDS, eventIds);
            header.put(AddHeaderTransformRuntime.HEADER_FIELD_DATASOURCE_IDS, dataSourceIds);
            header.put(AddHeaderTransformRuntime.HEADER_FIELD_NOTIFIER_NAME, "console_notifier");
            header.put(AddHeaderTransformRuntime.HEADER_FIELD_RULE_ID, 1L);
            header.put(AddHeaderTransformRuntime.HEADER_FIELD_TIMESTAMP, System.currentTimeMillis());

            StreamlineEvent streamlineEvent = new StreamlineEventImpl(fieldsMap, "notificationsTestBolt", header);
            collector.emit(consoleNotificationStream, new Values(streamlineEvent));
        }

        // Send an email every EMAIL_NOTIFICATION_INTERVAL
        if (++count % EMAIL_NOTIFICATION_INTERVAL == 0) {
            if (!emailNotificationStream.isEmpty()) {
                Map<String, Object> fieldsMap = new HashMap<>();
                fieldsMap.put("body", "Too many notifications, count so far is " + count);
                Map<String, Object> header = new HashMap<>();
                header.put(AddHeaderTransformRuntime.HEADER_FIELD_EVENT_IDS, eventIds);
                header.put(AddHeaderTransformRuntime.HEADER_FIELD_DATASOURCE_IDS, dataSourceIds);
                header.put(AddHeaderTransformRuntime.HEADER_FIELD_NOTIFIER_NAME, "email_notifier");
                header.put(AddHeaderTransformRuntime.HEADER_FIELD_RULE_ID, 1L);
                header.put(AddHeaderTransformRuntime.HEADER_FIELD_TIMESTAMP, System.currentTimeMillis());

                StreamlineEvent streamlineEvent = new StreamlineEventImpl(fieldsMap, "notificationsTestBolt", header);

                collector.emit(emailNotificationStream, new Values(streamlineEvent));
            }
        }
        collector.ack(input);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        if (!consoleNotificationStream.isEmpty()) {
            declarer.declareStream(consoleNotificationStream, new Fields(StreamlineEvent.STREAMLINE_EVENT));
        }
        if (!emailNotificationStream.isEmpty()) {
            declarer.declareStream(emailNotificationStream, new Fields(StreamlineEvent.STREAMLINE_EVENT));
        }
    }
}
