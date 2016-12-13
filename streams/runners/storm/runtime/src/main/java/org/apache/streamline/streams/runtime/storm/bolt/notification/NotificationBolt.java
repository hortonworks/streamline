/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.streamline.streams.runtime.storm.bolt.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.layout.component.impl.NotificationSink;
import org.apache.streamline.streams.notification.Notification;
import org.apache.streamline.streams.notification.NotifierConfig;
import org.apache.streamline.streams.notification.common.NotifierConfigImpl;
import org.apache.streamline.streams.notification.service.NotificationService;
import org.apache.streamline.streams.notification.service.NotificationServiceImpl;
import org.apache.streamline.streams.notification.store.NotificationStore;
import org.apache.streamline.streams.runtime.notification.StreamlineEventAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This storm bolt receives tuples from rule engine
 * and uses notification service to send out notifications.
 */
public class NotificationBolt extends BaseRichBolt {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationBolt.class);

    private static final String CATALOG_ROOT_URL = "catalog.root.url";
    public static final String LOCAL_NOTIFIER_JAR_PATH = "local.notifier.jar.path";

    private static final String NOTIFICATION_STORE_CONFIG_KEY = "notification.store.conf";
    private static final String NOTIFICATION_SERVICE_CONFIG_KEY = "notification.conf";
    private NotificationService notificationService;
    private BoltNotificationContext notificationContext;
    private final NotificationSink notificationSink;
    private String notificationStoreClazz = "";

    /**
     * The serialized JSON for the notification sink which is the design time component
     * in the streamline topology. The notifier jars and classes are assumed to be in the class path.
     *
     * @param notificationSinkJson
     */
    public NotificationBolt(String notificationSinkJson) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            notificationSink = mapper.readValue(notificationSinkJson, NotificationSink.class);
        } catch (IOException e) {
            LOG.error("Error during deserialization of JSON string: {}", notificationSinkJson, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * The notification sink which is the design time component in the streamline topology.
     * The notifier jars and classes are assumed to be in the class path.
     *
     * @param notificationSink
     */
    public NotificationBolt(NotificationSink notificationSink) {
        this.notificationSink = notificationSink;
    }

    public NotificationBolt withNotificationStoreClass(String clazz) {
        notificationStoreClazz = clazz;
        return this;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        if (!stormConf.containsKey(CATALOG_ROOT_URL)) {
            throw new IllegalArgumentException("conf must contain " + CATALOG_ROOT_URL);
        }

        Map<String, Object> notificationConf = null;
        if (stormConf.get(NOTIFICATION_SERVICE_CONFIG_KEY) != null) {
            notificationConf = (Map<String, Object>) stormConf.get(NOTIFICATION_SERVICE_CONFIG_KEY);
        } else {
            notificationConf = Collections.emptyMap();
        }

        NotificationStore notificationStore = null;
        try {
            if (!StringUtils.isEmpty(notificationStoreClazz)) {
                Class<?> clazz = Class.forName(notificationStoreClazz);
                notificationStore = (NotificationStore) clazz.newInstance();
                Map<String, Object> config = (Map<String, Object>) stormConf.get(NOTIFICATION_STORE_CONFIG_KEY);
                if (config == null) {
                    config = Collections.emptyMap();
                }
                notificationStore.init(config);
            }
        } catch (ClassNotFoundException | InstantiationException| IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }

        notificationService = new NotificationServiceImpl(notificationConf, notificationStore);

        String jarPath = "";
        if (stormConf.containsKey(LOCAL_NOTIFIER_JAR_PATH)) {
            jarPath = String.format("%s%s%s", stormConf.get(LOCAL_NOTIFIER_JAR_PATH).toString(),
                    File.separator, notificationSink.getNotifierJarFileName());
        }

        Properties props = new Properties();
        props.putAll(convertMapValuesToString(notificationSink.getNotifierProperties()));
        NotifierConfig notifierConfig = new NotifierConfigImpl(props,
                convertMapValuesToString(notificationSink.getNotifierFieldValues()),
                notificationSink.getNotifierClassName(), jarPath);

        notificationContext = new BoltNotificationContext(collector, notifierConfig);
        notificationService.register(notificationSink.getNotifierName(), notificationContext);
    }

    @Override
    public void execute(Tuple tuple) {
        Notification notification = new StreamlineEventAdapter((StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT));
        notificationContext.track(notification.getId(), tuple);
        // send to notifier
        notificationService.notify(notificationSink.getNotifierName(), notification);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }

    @Override
    public void cleanup() {
        notificationService.close();
    }

    private Map<String, String> convertMapValuesToString(Map<String, Object> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            Object val = e.getValue();
            if (val != null) {
                result.put(e.getKey(), val.toString());
            }
        }
        return result;
    }

}
