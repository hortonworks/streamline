package com.hortonworks.iotas.notification.notifiers.device;

import java.util.List;

import com.hortonworks.iotas.layout.design.component.IotasProcessor;
import com.hortonworks.iotas.notification.notifiers.device.DeviceRegistry;

public class DeviceRegistryProcessor extends IotasProcessor {
    List<DeviceRegistry> devices;

    public DeviceRegistryProcessor() {
    }

    public DeviceRegistryProcessor(List<DeviceRegistry> devices) {
        this.devices = devices;
    }

    public List<DeviceRegistry> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceRegistry> devices) {
        this.devices = devices;
    }

    @Override
    public String toString() {
        return "DeviceRegistryProcessor{" +
                "devices=" + devices +
                '}';
    }
}
