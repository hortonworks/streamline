package com.hortonworks.iotas.notification.notifiers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationContext;
import com.hortonworks.iotas.notification.notifiers.config.DeviceNotifierConfig;
import com.hortonworks.iotas.notification.notifiers.device.DeviceInstance;
import mockit.Expectations;
import mockit.integration.junit4.JMockit;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import mockit.Mocked;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.hortonworks.iotas.notification.notifiers.device.protocol.mqtt.MQTTProtocol.MQTT_BROKER_URL;
import static com.hortonworks.iotas.notification.notifiers.device.protocol.mqtt.MQTTProtocol.MQTT_TOPIC;

@RunWith(JMockit.class)
public class MQTTDeviceNotifierTest {

    MQTTDeviceNotifier mqttDeviceNotifier;

    @Mocked
    NotificationContext mockNotificationContext;

    @Mocked
    DeviceNotifierConfig mockNotifierConfig;

    @Mocked
    Notification mockNotification;

    @Mocked
    DeviceInstance deviceInstance;

    @Before
    public void setUp() throws Exception {
        mqttDeviceNotifier = new MQTTDeviceNotifier();
    }

    @Test
    public void testMQTTDeviceNotifier() throws MqttException {
        final Map<String, String> callbackOperations = new HashMap();
        callbackOperations.put("operationName", "setTemperature");
        callbackOperations.put("parameter", "temperature");
        callbackOperations.put("value", "80");

        final Properties properties = new Properties() {
            {
                setProperty(MQTT_BROKER_URL, "tcp://iot.eclipse.org:1883");
                setProperty(MQTT_TOPIC, "topic1");
            }
        };

        new Expectations(){
            {
                mockNotificationContext.getConfig();
                result = mockNotifierConfig;

                mockNotifierConfig.getProperties();
                result = new Properties();

                mockNotifierConfig.getDefaultFieldValues();
                result = callbackOperations;

                mockNotifierConfig.getDeviceMetaData();
                result = deviceInstance;

                deviceInstance.getProtocol().getProperties();
                result = properties;

                mockNotification.getId();
                times = 1;
                result = "ABC";
            }
        };
        mqttDeviceNotifier.open(mockNotificationContext);
        mqttDeviceNotifier.notify(mockNotification);
        mqttDeviceNotifier.close();
    }

    @Test(expected = NotifierRuntimeException.class)
    public void testMqttDeviceNotifierBrokerURL() {
        final Map<String, String> callbackOperations = new HashMap();
        callbackOperations.put("operationName", "setTemperature");
        callbackOperations.put("parameter", "temperature");
        callbackOperations.put("value", "80");

        final Properties properties = new Properties() {
            {
                setProperty(MQTT_BROKER_URL, "https://iot.eclipse.org:1883");
                setProperty(MQTT_TOPIC, "topic1");
            }
        };

        new Expectations(){
            {
                mockNotificationContext.getConfig();
                result = mockNotifierConfig;

                mockNotifierConfig.getProperties();
                result = new Properties();

                mockNotifierConfig.getDefaultFieldValues();
                result = callbackOperations;

                mockNotifierConfig.getDeviceMetaData();
                result = deviceInstance;

                deviceInstance.getProtocol().getProperties();
                result = properties;
            }
        };

        mqttDeviceNotifier.open(mockNotificationContext);
    }
}
