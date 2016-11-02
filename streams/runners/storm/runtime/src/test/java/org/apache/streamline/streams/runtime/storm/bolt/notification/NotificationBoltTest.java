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

import org.apache.streamline.common.util.ProxyUtil;
import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.catalog.CatalogRestClient;
import org.apache.streamline.streams.common.StreamlineEventImpl;
import org.apache.streamline.streams.layout.component.impl.NotificationSink;
import org.apache.streamline.streams.notification.Notification;
import org.apache.streamline.streams.notification.NotificationContext;
import org.apache.streamline.streams.notification.Notifier;
import org.apache.streamline.streams.notification.service.NotificationQueueHandler;
import org.apache.streamline.streams.notification.store.hbase.HBaseNotificationStore;
import org.apache.streamline.streams.notifiers.ConsoleNotifier;
import org.apache.streamline.streams.runtime.notification.StreamlineEventAdapter;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
@RunWith(JMockit.class)
public class NotificationBoltTest {

    private NotificationBolt bolt;

    private static final String NOTIFIER_NAME = "test_notifier";

    private static final Properties NOTIFIER_PROPS = new Properties();

    private static final Map<String, String> NOTIFIER_KV = new HashMap<>();

    @Mocked
    private CatalogRestClient catalogRestClient;

    @Mocked
    private OutputCollector collector;

    @Mocked
    ProxyUtil<Notifier> mockProxyUtil;

    @Mocked
    private Tuple tuple;

    @Mocked
    private HBaseNotificationStore hBaseNotificationStore;

    private Notifier notifier;

    @Before
    public void setUp() throws Exception {
        bolt = new NotificationBolt(new NotificationSink() {
            @Override
            public String getNotifierName() {
                return NOTIFIER_NAME;
            }

            @Override
            public String getNotifierJarFileName() {
                return NOTIFIER_NAME + ".jar";
            }

            @Override
            public String getNotifierClassName() {
                return "TestClass";
            }

            @Override
            public Map<String, Object> getNotifierProperties() {
                return new HashMap<>();
            }

            @Override
            public Map<String, Object> getNotifierFieldValues() {
                return new HashMap<>();
            }
        });

        notifier = new Notifier() {
            private NotificationContext myCtx;

            @Override
            public void open(NotificationContext ctx) {
                System.out.println("Notifier open called with ctx " + ctx);
                Assert.assertEquals(ctx.getConfig().getProperties(), NOTIFIER_PROPS);
                Assert.assertEquals(ctx.getConfig().getDefaultFieldValues(), NOTIFIER_KV);

                myCtx = ctx;
            }


            @Override
            public void notify(Notification notification) {
                System.out.println("Notifier notify called with notification " + notification);
                String temp = (String) notification.getFieldsAndValues().get("temperature");
                if (temp == null) {
                    myCtx.fail(notification.getId());
                } else {
                    myCtx.ack(notification.getId());
                }
            }

            @Override
            public void close() {
                System.out.println("Notifier Close called");
            }

            @Override
            public boolean isPull() {
                return false;
            }

            @Override
            public List<String> getFields() {
                return new ArrayList<>();
            }

            @Override
            public NotificationContext getContext() {
                return myCtx;
            }
        };

    }

    @Test(expected = IllegalArgumentException.class)
    public void testExecuteEmptyConf() throws Exception {
        bolt.prepare(new HashMap(), null, null);
    }

    @Test
    public void testWithConsoleNotifier() throws Exception {
        Map<String, Object> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("foo", "100");
        fieldsAndValues.put("bar", "200");
        final StreamlineEvent event = new StreamlineEventImpl(fieldsAndValues, "srcid");
        NotificationBolt consoleNotificationBolt = new NotificationBolt(new NotificationSink() {
            @Override
            public String getNotifierName() {
                return "console_notifier";
            }

            @Override
            public String getNotifierJarFileName() {
                return "console_notifier.jar";
            }

            @Override
            public String getNotifierClassName() {
                return "ConsoleNotifier";
            }

            @Override
            public Map<String, Object> getNotifierProperties() {
                return new HashMap<>();
            }

            @Override
            public Map<String, Object> getNotifierFieldValues() {
                return new HashMap<>();
            }
        });

        final ConsoleNotifier consoleNotifier = new ConsoleNotifier();
        final Notification notification = new StreamlineEventAdapter(event);
        new MockUp<NotificationQueueHandler>() {
            @Mock
            public void enqueue(Notifier notifier, Notification notification1) {
                //System.out.println("Mocked enqueue");
                notifier.notify(notification);
            }
        };
        new Expectations() {{
            mockProxyUtil.loadClassFromJar("/tmp/console_notifier.jar", "ConsoleNotifier");
            result = consoleNotifier;
            tuple.getValueByField(anyString);
            result = event;
        }};

        Map<String, String> stormConf = new HashMap<>();
        stormConf.put("catalog.root.url", "http://localhost:8080/api/v1/catalog");
        stormConf.put("local.notifier.jar.path", "/tmp");
        consoleNotificationBolt.prepare(stormConf, null, collector);
        consoleNotificationBolt.execute(tuple);
        new Verifications() {
            {
                collector.ack(tuple);
                times = 1;
                hBaseNotificationStore.store(notification);
                times = 1;
            }
        };
    }

    @Test
    public void testAck() throws Exception {

        Map<String, Object> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("temperature", "100");
        final StreamlineEvent event = new StreamlineEventImpl(fieldsAndValues, "srcid");
        final Notification notification = new StreamlineEventAdapter(event);
        new MockUp<NotificationQueueHandler>() {
            @Mock
            public void enqueue(Notifier notifier, Notification notification1) {
                notifier.notify(notification);
            }
        };
        new Expectations() {{
            mockProxyUtil.loadClassFromJar(anyString, "TestClass");
            result = notifier;
            tuple.getValueByField(anyString);
            result = event;
        }};

        Map<String, String> stormConf = new HashMap<>();
        stormConf.put("catalog.root.url", "http://localhost:8080/api/v1/catalog");
        stormConf.put("local.notifier.jar.path", "/tmp");
        bolt.prepare(stormConf, null, collector);

        bolt.execute(tuple);
        new Verifications() {
            {
                hBaseNotificationStore.store(notification);
                times = 1;
                hBaseNotificationStore.updateNotificationStatus(notification.getId(), Notification.Status.DELIVERED);
                times = 1;
                collector.ack(tuple);
                times = 1;
                collector.fail(tuple);
                times = 0;
            }
        };
    }

    @Test
    public void testFail() throws Exception {

        Map<String, Object> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("foobar", "100");
        final StreamlineEvent event = new StreamlineEventImpl(fieldsAndValues, "srcid");
        final Notification notification = new StreamlineEventAdapter(event);

        new MockUp<NotificationQueueHandler>() {
            @Mock
            public void enqueue(Notifier notifier, Notification notification1) {
                notifier.notify(notification);
            }
            @Mock
            public void resubmit(String notificationId) {
                notifier.notify(notification);
            }
        };

        new Expectations() {{
            mockProxyUtil.loadClassFromJar(anyString, "TestClass");
            result = notifier;
            tuple.getValueByField(anyString);
            result = event;
        }};

        Map<String, String> stormConf = new HashMap<>();
        stormConf.put("catalog.root.url", "http://localhost:8080/api/v1/catalog");
        stormConf.put("local.notifier.jar.path", "/tmp");
        bolt.prepare(stormConf, null, collector);

        bolt.execute(tuple);

        new Verifications() {
            {
                hBaseNotificationStore.store(notification);
                times = 1;
                hBaseNotificationStore.updateNotificationStatus(notification.getId(), Notification.Status.FAILED);
                times = 1;
                collector.ack(tuple);
                times = 0;
                collector.fail(tuple);
                times = 1;
            }
        };
    }

}
