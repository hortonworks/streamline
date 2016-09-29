package com.hortonworks.iotas.notification.notifiers.config;

import com.hortonworks.iotas.notification.common.NotifierConfig;
import com.hortonworks.iotas.notification.notifiers.device.DeviceInstance;

/*
 In case of device notifications, we also the need the device instance or the information about the device. This is retrieved from the
 DeviceInstance object. Hence a new interface is created which enables this.
 */
public interface DeviceNotifierConfig extends NotifierConfig {
    /**
     * The meta data of the device
     * @return DeviceInstance
     */
    DeviceInstance getDeviceInstance();
}
