package com.hortonworks.iotas.notification.notifiers.config;

import com.hortonworks.iotas.notification.common.NotifierConfig;
import com.hortonworks.iotas.notification.notifiers.device.DeviceInstance;

public interface DeviceNotifierConfig extends NotifierConfig {
    /**
     * The meta data of the device
     * @return DeviceInstance
     */
    DeviceInstance getDeviceMetaData();
}
