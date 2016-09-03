package com.hortonworks.iotas.notification.notifiers.device;

import com.hortonworks.iotas.common.Schema;

public class DeviceAttribute {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceAttribute)) return false;

        DeviceAttribute that = (DeviceAttribute) o;

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
