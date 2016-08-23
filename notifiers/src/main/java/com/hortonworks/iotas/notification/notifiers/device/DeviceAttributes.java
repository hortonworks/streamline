package com.hortonworks.iotas.notification.notifiers.device;

import com.hortonworks.iotas.common.Schema;

public class DeviceAttributes {
    private Schema.Field field;

    public DeviceAttributes() {
    }

    public DeviceAttributes(Schema.Field field) {
        this.field = field;
    }

    public Schema.Field getField() {
        return field;
    }

    public void setField(Schema.Field field) {
        this.field = field;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceAttributes)) return false;

        DeviceAttributes that = (DeviceAttributes) o;

        return field != null ? field.equals(that.field) : that.field == null;

    }

    @Override
    public int hashCode() {
        return field != null ? field.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "DeviceAttributes{" +
                "field=" + field +
                '}';
    }
}
