package com.hortonworks.iotas.notification.notifiers.device;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationContext;
import com.hortonworks.iotas.notification.common.Notifier;
import com.hortonworks.iotas.notification.notifiers.NotifierRuntimeException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.*;

public class MQTTDeviceNotifier implements Notifier{
    private static final Logger LOG = LoggerFactory.getLogger(MQTTDeviceNotifier.class);

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

    private static Field Field(String key, String defaultVal) {
        return new Field(key, defaultVal);
    }

    //Configuration properties
    private static final Field PROP_CLEAN_SESSION = Field("cleanSession", "true");
    private static final Field PROP_KEEP_ALIVE = Field("keepAlive", "60");
    private static final Field PROP_RETAIN_FLAG = Field("retainFlag", "false");
    private static final Field PROP_QoS = Field("QoS","1");
    private static final Field PROP_DUP_FLAG = Field("dupFlag", "false");

    MqttClient mqttClient;
    MqttConnectOptions options;
    private NotificationContext ctx;
    private String clientId;
    private String broker;
    private String topic;
    private Map <String, Object> fieldsFromDeviceRegistry;

    @Override
    public void open(NotificationContext ctx) {
        LOG.debug("MQTTDeviceNotifier open called with context {}", ctx);
        clientId = UUID.randomUUID().toString();
        getDeviceRegistryDataWrap();
        this.ctx = ctx;
        try {
            LOG.debug("Connecting to broker {}", broker);
            mqttClient = new MqttClient(broker,clientId);
            options = getMqttConnectOptions(ctx.getConfig().getProperties());
            mqttClient.connect(options);
            LOG.debug("Connected to broker {}", broker);
        } catch(MqttException | IllegalArgumentException ex) {
            LOG.error("Got exception "+ ex);
            throw new NotifierRuntimeException(ex);
        }
    }

    @Override
    public void notify(Notification notification) {
        try {
            String notificationId = notification.getId();
            if (notificationId == null) {
                throw new NotifierRuntimeException("Id is null for notification " + notification);
            }
            MqttMessage mqttMessage = getMqttMessage(ctx.getConfig().getProperties());
            mqttClient.publish(topic, mqttMessage);
            LOG.debug("MQTT message published");
        } catch(MqttException | NullPointerException | IllegalStateException ex) {
            LOG.error("Got exception "+ ex);
            throw new NotifierRuntimeException(ex);
        }
    }

    @Override
    public void close() {
        try {
            mqttClient.disconnect();
            LOG.debug("Disconnected from broker {}", broker);
        } catch (MqttException ex) {
            LOG.error("Got exception "+ ex);
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
        return ctx;
    }

    public String getProperty(Properties properties, Field field) {
        return properties.getProperty(field.key, field.defaultVal);
    }

    public MqttConnectOptions getMqttConnectOptions(Properties properties) {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(Boolean.valueOf(getProperty(properties,PROP_CLEAN_SESSION)));
            options.setKeepAliveInterval(Integer.parseInt(getProperty(properties,PROP_KEEP_ALIVE)));
            return options;
        } catch(IllegalArgumentException ex) {
            LOG.error("Got exception "+ ex);
            throw new NotifierRuntimeException(ex);
        }
    }

    public MqttMessage getMqttMessage(Properties properties) {
        try {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setRetained(Boolean.valueOf(getProperty(properties,PROP_RETAIN_FLAG)));
            mqttMessage.setQos(Integer.parseInt(getProperty(properties,PROP_QoS)));
            mqttMessage.setPayload(getMqttMessagePayload());
            return mqttMessage;
        } catch(IllegalStateException | NullPointerException | IllegalArgumentException ex) {
            LOG.error("Got exception "+ ex);
            throw new NotifierRuntimeException(ex);
        }
    }

    public byte[] getMqttMessagePayload() {
        byte[] payload = {};
        Map<String, Object> payloadMap = getPayloadMap();
        try {
            LOG.debug("Generating the payload for MQTT packet");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutput objectOutput = new ObjectOutputStream(byteArrayOutputStream);
            objectOutput.writeObject(payloadMap);
            payload = byteArrayOutputStream.toByteArray();

        } catch (Exception ex) {
            LOG.error("Got exception "+ ex);
            throw new NotifierRuntimeException(ex);
        }
        return payload;
    }

    public Map<String,Object> getPayloadMap() {
        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("deviceId",fieldsFromDeviceRegistry.get("id"));
        payloadMap.put("deviceName",fieldsFromDeviceRegistry.get("name"));
        payloadMap.put("deviceModel",fieldsFromDeviceRegistry.get("model"));
        payloadMap.put("deviceMake",fieldsFromDeviceRegistry.get("make"));
        payloadMap.put("operation",getOperationField());
        return payloadMap;
    }

    public Object getOperationField() {
        Map<String, Object> operationMap = new LinkedHashMap<>();
        operationMap.put("name","setTemperature");
        operationMap.put("param","temperature");
        Random randomTemperature = new Random();
        operationMap.put("value",randomTemperature.nextInt(100));
        return operationMap;
    }

    public void getDeviceRegistryDataWrap() {
        try {
            getDeviceRegistryData();
        } catch (Exception ex) {
            LOG.error("Got exception "+ ex);
            throw new NotifierRuntimeException(ex);
        }
    }

    public void getDeviceRegistryData() throws Exception{
        LOG.debug("Retrieving metadata from devie registry");
        DeviceRegistryProcessor mockDeviceRegistryProcessor = new DeviceRegistryProcessorMockBuilder(1, 1, 1, 1).build();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        String mockDeviceRegistryProcessorJson = mapper.writeValueAsString(mockDeviceRegistryProcessor);
        Map<String,Object> deviceRegistryData = mapper.readValue(mockDeviceRegistryProcessorJson, Map.class);
        List<Object> listDeviceRegistry = (List<Object>) deviceRegistryData.get("devices");
        fieldsFromDeviceRegistry = (Map<String, Object>) listDeviceRegistry.get(0);
        topic = (String) fieldsFromDeviceRegistry.get("topic");
        broker = (String) fieldsFromDeviceRegistry.get("mqttBroker");
    }
}