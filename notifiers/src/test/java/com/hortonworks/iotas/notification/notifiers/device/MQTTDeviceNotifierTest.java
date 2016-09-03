package com.hortonworks.iotas.notification.notifiers.device;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationContext;
import com.hortonworks.iotas.notification.common.NotifierConfig;
import mockit.Expectations;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import mockit.Mocked;

import java.util.Properties;

@RunWith(JMockit.class)
public class MQTTDeviceNotifierTest {

    MQTTDeviceNotifier mqttDeviceNotifier;

    @Mocked
    NotificationContext mockNotificationContext;

    @Mocked
    NotifierConfig mockNotifierConfig;

    @Mocked
    Notification mockNotification;

    @Before
    public void setUp() throws Exception {
        mqttDeviceNotifier = new MQTTDeviceNotifier();
    }

    @Test
    public void testMQTTDeviceNotifier() {

        new Expectations(){
            {
                mockNotificationContext.getConfig();
                result = mockNotifierConfig;
                mockNotifierConfig.getProperties();
                result = new Properties();
                mockNotification.getId();
                times = 1;
                result = "ABC";

            }
        };
        mqttDeviceNotifier.open(mockNotificationContext);
        mqttDeviceNotifier.notify(mockNotification);
        //mqttDeviceNotifier.close();
    }
}
