package com.hortonworks.iotas.notification.notifiers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationContext;
import com.hortonworks.iotas.notification.common.Notifier;
import com.hortonworks.iotas.notification.notifiers.config.DeviceNotifierConfig;
import com.hortonworks.iotas.notification.notifiers.device.DeviceInstance;
import com.hortonworks.iotas.notification.notifiers.device.protocol.mqtt.MQTTProtocol;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

import static com.hortonworks.iotas.notification.notifiers.device.protocol.mqtt.MQTTProtocol.MQTT_BROKER_URL;

public class MQTTDeviceNotifier implements Notifier {
    private static final Logger LOG = LoggerFactory.getLogger(MQTTDeviceNotifier.class);

    //Configuration properties
    private static final Field PROP_CLEAN_SESSION = new Field("cleanSession", "true");
    private static final Field PROP_KEEP_ALIVE = new Field("keepAlive", "60");
    private static final Field PROP_RETAIN_FLAG = new Field("retainFlag", "false");
    private static final Field PROP_QoS = new Field("QoS","1");
    private static final Field PROP_DUP_FLAG = new Field("dupFlag", "false");

    /**
     * A wrapper class to hold a key and its default value
     */
    private static class Field {
        Field(String key, String defaultVal) {
            this.key = key;
            this.defaultVal = defaultVal;
        }
        String key;
        String defaultVal;
    }

    private NotificationContext context;
    private Properties configProperties;
    private Map<String,String> callbackOperations;
    private DeviceInstance deviceInstance;
    private String mqttClientId;
    private String mqttBrokerURL;
    private String mqttTopic;
    private int mqttQoS;
    private MqttClient mqttClient;
    private MqttConnectOptions mqttOptions;

    @Override
    public void open(NotificationContext context) {
        LOG.debug("MQTTDeviceNotifier open called with context {}", context);
        try {
            this.context = context;
            mqttClientId = UUID.randomUUID().toString();
            configProperties = context.getConfig().getProperties();
            callbackOperations = context.getConfig().getDefaultFieldValues();
            deviceInstance = ((DeviceNotifierConfig)context.getConfig()).getDeviceMetaData();
            mqttBrokerURL = setBrokerUrl();
            mqttQoS = setQoS();
            mqttTopic = (String) deviceInstance.getProtocol().getProperties().get(MQTTProtocol.MQTT_TOPIC);
            mqttOptions = getMqttConnectOptions();
            connect();
        } catch(MqttException ex) {
            throw new NotifierRuntimeException("Got exception while attempting to connect to MQTT broker", ex);
        }
    }

    @Override
    public void notify(Notification notification) {
        try {
            String notificationId = notification.getId();
            if (notificationId == null) {
                throw new NotifierRuntimeException("Id is null for notification " + notification);
            }
            MqttMessage mqttMessage = getMqttMessage();
            mqttClient.publish(mqttTopic, mqttMessage);
            LOG.debug("MQTT message published");
        } catch(MqttException ex) {
            LOG.error("Got exception while sending MQTT message to the broker", ex);
            throw new NotifierRuntimeException(ex);
        }
    }

    @Override
    public void close() {
        try {
            mqttClient.disconnect();
            LOG.debug("Disconnected from broker {}", mqttBrokerURL);
        } catch (MqttException ex) {
            LOG.error("Got exception while disconnecting from MQTT broker", ex);
            throw new NotifierRuntimeException(ex);
        }
    }

    @Override
    public boolean isPull() {
        return false;
    }

    @Override
    public List<String> getFields() {
        return Collections.emptyList();
    }

    @Override
    public NotificationContext getContext() {
        return context;
    }

    public String setBrokerUrl() {
        String url = (String) deviceInstance.getProtocol().getProperties().get(MQTT_BROKER_URL);
        validateUrl(url);
        return url;
    }

    private void validateUrl(String mqttBrokerURL) {
        if (!(mqttBrokerURL.startsWith("tcp://") || mqttBrokerURL.startsWith("ssl://") || mqttBrokerURL.startsWith("local://"))) {
            throw new NotifierRuntimeException("MQTT Broker URL should start with either \"tcp://\", \"ssl://\" or \"local://\"");
        }
    }

    private int setQoS() {
        int QoS = Integer.parseInt(getProperty(configProperties, PROP_QoS));
        validateQoS();
        return QoS;
    }

    private void validateQoS() {
        if (mqttQoS < 0 || mqttQoS > 2) {
            throw new NotifierRuntimeException("QoS value should be 0,1 or 2");
        }
    }

    public String getProperty(Properties properties, Field field) {
        return properties.getProperty(field.key, field.defaultVal);
    }

    /*
        This method sets the various MQTT connect mqttOptions before a MQTT client connects to a
        MQTT Broker. Options include CleanSession, KeepAliveInterval etc.
   */
    private MqttConnectOptions getMqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(Boolean.valueOf(getProperty(configProperties,PROP_CLEAN_SESSION)));
        int keepAliveInterval = Integer.parseInt(getProperty(configProperties,PROP_KEEP_ALIVE));
        if(keepAliveInterval < 0 || keepAliveInterval > 65535) {
            throw new NotifierRuntimeException("Keep alive interval should be between 0 and 65535, both inclusive");
        }
        options.setKeepAliveInterval(keepAliveInterval);
        return options;
    }

    /**
     * Connects the MqttClient to the MQTT Broker
     * @throws MqttException
     */
    private void connect() throws MqttException {
        mqttClient = new MqttClient(mqttBrokerURL, mqttClientId);
        LOG.debug("Connecting to broker {}", mqttBrokerURL);
        mqttClient.connect(mqttOptions);
        LOG.debug("Connected to broker {}", mqttBrokerURL);
    }

    /**
     * Constructs the MQTT message that will be sent to the broker. Various mqttOptions of the message like RetainFlag, QoS are set here.
     * The payload of the message, i.e. the operation which should be performed is also built here.
     * @return MqttMessage
     */
    public MqttMessage getMqttMessage(){
        try {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setRetained(Boolean.valueOf(getProperty(configProperties,PROP_RETAIN_FLAG)));
            int qos = Integer.parseInt(getProperty(configProperties,PROP_QoS));
            if(qos < 0 || qos > 2) {
                throw new NotifierRuntimeException("QoS value should be 0,1 or 2");
            }
            mqttMessage.setQos(qos);
            byte[] payload = new byte[0];
            try {
                payload = getMqttMessagePayload();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(payload == null) {
                throw new NotifierRuntimeException("Payload is null!");
            }
            mqttMessage.setPayload(payload);
            return mqttMessage;
        } catch(IllegalStateException ex) {
            LOG.error("Got exception "+ ex);
            throw new NotifierRuntimeException(ex);
        }
    }

    /**
     * Builds the payload of the MQTT message. MQTTMessage expects the payload to be a byte array.
     * This method gets the payload and converts it into a byte array.
     * @return byte array of payload
     */
    public byte[] getMqttMessagePayload() throws IOException {
        byte[] payload = {};
        LOG.debug("Generating the payload for MQTT packet");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(callbackOperations);
        payload = byteArrayOutputStream.toByteArray();
        return payload;
    }
}