package com.hortonworks.bolt.notification;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import com.hortonworks.client.RestClient;
import com.hortonworks.iotas.catalog.NotifierInfo;
import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationContext;
import com.hortonworks.iotas.notification.common.NotificationImpl;
import com.hortonworks.iotas.notification.common.Notifier;
import com.hortonworks.iotas.notification.notifiers.ConsoleNotifier;
import com.hortonworks.iotas.notification.store.hbase.HBaseNotificationStore;
import com.hortonworks.util.ReflectionHelper;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
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
 * Created by aiyer on 9/24/15.
 */
@RunWith(JMockit.class)
public class NotificationBoltTest {

    private NotificationBolt bolt;

    private static final String NOTIFIER_NAME = "test_notifier";

    private static final Properties NOTIFIER_PROPS = new Properties();

    private static final Map<String, String> NOTIFIER_KV = new HashMap<>();

    @Mocked
    private RestClient restClient;

    @Mocked
    private OutputCollector collector;

    @Mocked
    private NotifierInfo notifierInfo;

    @Mocked
    private ReflectionHelper reflectionHelper;

    @Mocked
    private Tuple tuple;

    @Mocked
    private HBaseNotificationStore hBaseNotificationStore;

    private Notifier notifier;

    @Before
    public void setUp() throws Exception {
        bolt = new NotificationBolt(NOTIFIER_NAME);
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
                String temp = notification.getFieldsAndValues().get("temperature");
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
        };

    }

    @Test(expected = IllegalArgumentException.class)
    public void testExecuteEmptyConf() throws Exception {
        bolt.prepare(new HashMap(), null, null);
    }

    @Test
    public void testWithConsoleNotifier() throws Exception {
        Map<String, String> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("foo", "100");
        fieldsAndValues.put("bar", "200");
        final Notification notification = new NotificationImpl.Builder(fieldsAndValues).build();
        NotificationBolt consoleNotificationBolt = new NotificationBolt("console_notifier");
        final ConsoleNotifier consoleNotifier = new ConsoleNotifier();
        new Expectations() {{
            restClient.getNotifierInfo(anyString);
            result = notifierInfo;
            notifierInfo.getJarFileName();
            result = "console_notifier.jar";
            notifierInfo.getClassName();
            result = "ConsoleNotifier";
            notifierInfo.getProperties();
            result = NOTIFIER_PROPS;
            notifierInfo.getFieldValues();
            result = NOTIFIER_KV;
            ReflectionHelper.isJarInClassPath(anyString);
            result = true;
            ReflectionHelper.newInstance(anyString);
            result = consoleNotifier;
            tuple.getValueByField(anyString);
            result = notification;
        }};

        Map<String, String> stormConf = new HashMap<>();
        stormConf.put("catalog.root.url", "http://localhost:8080/api/v1/catalog");
        stormConf.put("local.notifier.jar.path", "/tmp");
        consoleNotificationBolt.prepare(stormConf, null, collector);

        consoleNotificationBolt.execute(tuple);

        new Verifications() {
            {
                restClient.getNotifierInfo("console_notifier");
                times = 1;
                collector.ack(tuple); times = 1;
                hBaseNotificationStore.store(notification);
                times = 1;
            }
        };
    }

    @Test
    public void testAck() throws Exception {

        Map<String, String> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("temperature", "100");
        final Notification notification = new NotificationImpl
                .Builder(fieldsAndValues).build();

        new Expectations() {{
            restClient.getNotifierInfo(anyString);
            result = notifierInfo;
            notifierInfo.getJarFileName();
            result = NOTIFIER_NAME + ".jar";
            notifierInfo.getClassName();
            result = "TestClass";
            notifierInfo.getProperties();
            result = NOTIFIER_PROPS;
            notifierInfo.getFieldValues();
            result = NOTIFIER_KV;
            ReflectionHelper.isJarInClassPath(anyString);
            result = true;
            ReflectionHelper.newInstance(anyString);
            result = notifier;
            tuple.getValueByField(anyString);
            result = notification;
        }};

        Map<String, String> stormConf = new HashMap<>();
        stormConf.put("catalog.root.url", "http://localhost:8080/api/v1/catalog");
        stormConf.put("local.notifier.jar.path", "/tmp");
        bolt.prepare(stormConf, null, collector);

        bolt.execute(tuple);

        new Verifications() {
            {
                restClient.getNotifierInfo(NOTIFIER_NAME);
                times = 1;
                hBaseNotificationStore.store(notification);
                times = 1;
                collector.ack(tuple); times = 1;
                collector.fail(tuple); times = 0;
            }
        };
    }

    @Test
    public void testFail() throws Exception {

        Map<String, String> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put("foobar", "100");
        final Notification notification = new NotificationImpl
                .Builder(fieldsAndValues).build();

        new Expectations() {{
            restClient.getNotifierInfo(anyString);
            result = notifierInfo;
            notifierInfo.getJarFileName();
            result = NOTIFIER_NAME;
            notifierInfo.getClassName();
            result = "TestClass";
            notifierInfo.getProperties();
            result = NOTIFIER_PROPS;
            notifierInfo.getFieldValues();
            result = NOTIFIER_KV;
            ReflectionHelper.isJarInClassPath(anyString);
            result = true;
            ReflectionHelper.newInstance(anyString);
            result = notifier;
            tuple.getValueByField(anyString);
            result = notification;
        }};

        Map<String, String> stormConf = new HashMap<>();
        stormConf.put("catalog.root.url", "http://localhost:8080/api/v1/catalog");
        stormConf.put("local.notifier.jar.path", "/tmp");
        bolt.prepare(stormConf, null, collector);

        bolt.execute(tuple);

        new Verifications() {
            {
                restClient.getNotifierInfo(NOTIFIER_NAME);
                times = 1;
                hBaseNotificationStore.store(notification);
                times = 1;
                collector.ack(tuple); times = 0;
                collector.fail(tuple); times = 1;
            }
        };
    }

}
