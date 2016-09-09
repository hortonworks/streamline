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
    private static final Field CLEAN_SESSION = new Field("cleanSession", "true");
    private static final Field KEEP_ALIVE = new Field("keepAlive", "60");
    private static final Field RETAIN_FLAG = new Field("retainFlag", "false");
    private static final Field QoS = new Field("QoS","1");
    private static final Field DUP_FLAG = new Field("dupFlag", "false"); //The eclipse library for MQTT doesn't have the method to set this property

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

    /**
     The callbackOperation map becomes the payload. It would look something like this -
     {
     "operationName" : "setTemperature",
     "parameter" : "temperature",
     "value" : "80"
     }
     */
    private Map<String,String> callbackOperation;
    private DeviceInstance deviceInstance;
    private String mqttClientId; //The unique ID used to identify a a MQTT client with the MQTT broker
    private String mqttBrokerURL;
    private String mqttTopic; //The topic to which the data will be written to, is retrived from deviceInstance
    private int mqttQoS;
    private MqttClient mqttClient;
    private MqttConnectOptions mqttOptions; //This object is used to set the various configuration properties of the MQTT Client

    /**
     * In this method, all the variables are initialized and the MQTT client is connected with the MQTT broker
     * @param context
     */
    @Override
    public void open(NotificationContext context) {
        LOG.debug("MQTTDeviceNotifier open called with context {}", context);
        try {
            this.context = context;
            mqttClientId = UUID.randomUUID().toString(); //As of now the client id is generated randomly, but in the future we need to think of scenarios where we would obtain it from the context
            configProperties = context.getConfig().getProperties();
            callbackOperation = context.getConfig().getDefaultFieldValues();
            deviceInstance = ((DeviceNotifierConfig)context.getConfig()).getDeviceInstance();
            mqttBrokerURL = setBrokerUrl();
            mqttQoS = setQoS();
            mqttTopic = (String) deviceInstance.getProtocol().getProperties().get(MQTTProtocol.MQTT_TOPIC);
            mqttOptions = getMqttConnectOptions();
            connect();
        } catch(MqttException ex) {
            throw new NotifierRuntimeException("Got exception while attempting to connect to MQTT broker", ex);
        }
    }

    /**
     * The payload is constructed using the callbackOperation map and this message is published to the broker
     * @param notification the Notification object
     */
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
        int qos = Integer.parseInt(getProperty(configProperties, QoS));
        validateQoS();
        return qos;
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
        options.setCleanSession(Boolean.valueOf(getProperty(configProperties, CLEAN_SESSION)));
        int keepAliveInterval = Integer.parseInt(getProperty(configProperties, KEEP_ALIVE));
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
            mqttMessage.setRetained(Boolean.valueOf(getProperty(configProperties, RETAIN_FLAG)));
            int qos = Integer.parseInt(getProperty(configProperties, QoS));
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
        objectOutputStream.writeObject(callbackOperation);
        payload = byteArrayOutputStream.toByteArray();
        return payload;
    }
}