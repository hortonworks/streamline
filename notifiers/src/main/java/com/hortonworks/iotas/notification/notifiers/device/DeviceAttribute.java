package com.hortonworks.iotas.notification.notifiers.device;

import com.hortonworks.iotas.common.Schema;

/**
 * Represents an attribute for the device. E.g A device like 'Thermostat' could
 * have attributes like temperature, humidity, fanSpeed etc.
 */
public class DeviceAttribute {
    /**
     * This class member encapsulates the type of the attribute, its name and whether it is optional or not.
     */
    private Schema.Field field;

    public DeviceAttribute() {
    }

    public DeviceAttribute(Schema.Field field) {
        this.field = field;
    }

    public Schema.Field getField() {
        return field;
    }

    public void setField(Schema.Field field) {
        this.field = field;
    }

    @Override
    public String toString() {
        return "DeviceAttribute{" +
                "field=" + field +
                '}';
    }
}
