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

package com.hortonworks.iotas.bolt.notification;

import com.hortonworks.iotas.streams.catalog.NotifierInfo;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import com.hortonworks.iotas.client.CatalogRestClient;
import com.hortonworks.iotas.streams.IotasEvent;
import com.hortonworks.iotas.streams.notification.Notification;
import com.hortonworks.iotas.streams.notification.NotifierConfig;
import com.hortonworks.iotas.streams.notification.common.NotifierConfigImpl;
import com.hortonworks.iotas.streams.notification.service.NotificationService;
import com.hortonworks.iotas.streams.notification.service.NotificationServiceImpl;
import com.hortonworks.iotas.streams.notification.store.hbase.HBaseNotificationStore;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * This storm bolt receives tuples from rule engine
 * and uses notification service to send out notifications.
 */
public class NotificationBolt extends BaseRichBolt {
    private static final String CATALOG_ROOT_URL = "catalog.root.url";
    public static final String LOCAL_NOTIFIER_JAR_PATH = "local.notifier.jar.path";

    private static final String NOTIFICATION_SERVICE_CONFIG_KEY = "notification.conf";
    private NotificationService notificationService;
    private BoltNotificationContext notificationContext;
    private CatalogRestClient catalogRestClient;
    private String hbaseConfigKey = "hbase.conf";

    private final String notifierName;

    /**
     * <p>
     *     The notifier name that this bolt handles. The notifier name should uniquely identify
     *     a notifier instance that the user configured via dashboard (e.g. email_notifier_1).
     * </p>
     * @param notifierName The notifier name associated with this bolt.
     */
    public NotificationBolt(String notifierName) {
        this.notifierName = notifierName;
    }

    public NotificationBolt withHBaseConfigKey(String key) {
        this.hbaseConfigKey = key;
        return this;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        if (!stormConf.containsKey(CATALOG_ROOT_URL)) {
            throw new IllegalArgumentException("conf must contain " + CATALOG_ROOT_URL);
        }
        Map<String, String> hbaseConf = (Map<String, String>)stormConf.get(this.hbaseConfigKey);
        Map<String, Object> notificationConf = null;
        if (stormConf.get(NOTIFICATION_SERVICE_CONFIG_KEY) != null) {
            notificationConf = (Map<String, Object>) stormConf.get(NOTIFICATION_SERVICE_CONFIG_KEY);
        } else {
            notificationConf = Collections.emptyMap();
        }
        notificationService = new NotificationServiceImpl(notificationConf, new HBaseNotificationStore(hbaseConf));
        catalogRestClient = new CatalogRestClient(stormConf.get(CATALOG_ROOT_URL).toString());
        NotifierInfo notifierInfo = catalogRestClient.getNotifierInfo(this.notifierName);

        String jarPath = String.format("%s%s%s", stormConf.get(LOCAL_NOTIFIER_JAR_PATH).toString(),
                                       File.separator, notifierInfo.getJarFileName());

        Properties props = new Properties();
        props.putAll(notifierInfo.getProperties());
        NotifierConfig notifierConfig = new NotifierConfigImpl(props, notifierInfo.getFieldValues(),
                                                               notifierInfo.getClassName(), jarPath);

        notificationContext = new BoltNotificationContext(collector, notifierConfig);
        notificationService.register(this.notifierName, notificationContext);
    }

    @Override
    public void execute(Tuple tuple) {
        Notification notification = new IotasEventAdapter((IotasEvent) tuple.getValueByField(IotasEvent.IOTAS_EVENT));
        notificationContext.track(notification.getId(), tuple);
        // send to notifier
        notificationService.notify(this.notifierName, notification);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }

    @Override
    public void cleanup() {
        notificationService.close();
    }

}
