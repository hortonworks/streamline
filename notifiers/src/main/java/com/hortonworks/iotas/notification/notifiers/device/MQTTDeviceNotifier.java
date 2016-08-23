package com.hortonworks.iotas.notification.notifiers.device;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationContext;
import com.hortonworks.iotas.notification.common.Notifier;
import com.hortonworks.iotas.notification.service.NotificationServiceContext;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

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

    //TODO: What all MQTT parameters will be needed, and where would they be obtained from
    //ClientID: Would make sense to randomly generate it
    //****clean session, always true, keep alive 60 for now
    //Packet ID, randomly generate it
    //Topic Name - Comes from device registry
    //QoS - From device registry
    //****retain flag - from device registry, can assume false
    //****DUP flag - from device regsitry, can assume false
    //Payload - Will be coming from the previous bolt, most likely the RULE BOLT // OR MACHINE LEARNING BOLT
    private static final Field PROP_CLEAN_SESSION = Field("cleanSession", "true");
    private static final Field PROP_KEEP_ALIVE = Field("keepAlive", "60");
    private static final Field PROP_RETAIN_FLAG = Field("retainFlag", "false");
    private static final Field PROP_DUP_FLAG = Field("dupFlag", "false");

    //TODO: Declare MQTT client, will be used in open, notify and close method
    private NotificationContext ctx;
    //TODO: Will require something from the DeviceRegistry, will have to mock this up, may be we could call the object DeviceRegistryContext
    private Map<String, Object> payloadFields;
    private Map <String, Object> fieldsFromDeviceRegistry;
    MqttClient mqttClient;

    @Override
    public void open(NotificationContext ctx) {
        LOG.debug("MQTTDeviceNotifier open called with context {}", ctx);
        this.ctx = ctx;
        //TODO: Start an MQTT client
        //Get the payload for publish
        //this.payloadFields = getPayloadFields(); //TODO: This will be from the NotificationContext
        //this.fieldsFromDeviceRegistry = getFieldsFromDeviceRegistry(); //TODO: This will be from the deviceRegistryContext
        //this.mqttClient = getMqttClient(); //TODO: Fields from notificationContext and deviceRegistryContext will go here as parameters
    }

    @Override
    public void notify(Notification notification) {
        //TODO: the part where we would actually send the publish message to the broker
        //Check for empty fields
        //Throw mqtt exceptions
    }

    @Override
    public void close() {
        //TODO: something of the form .disconnect();, catch exceptions, log messages
            //xyz.disconnect();
    }

    @Override
    public boolean isPull() {
        return false; //TODO: Confirm with Hugo!
    }

    @Override
    public List<String> getFields() {
        return null;
        //TODO: Can return the device id, the topic, the payload, QoS, broker url, what else? Notvery
    }

    @Override
    public NotificationContext getContext() {
        return ctx;
    }
}
