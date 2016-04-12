package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.storage.DataSourceSubType;
import com.hortonworks.iotas.storage.StorableKey;

/**
 * The device storage entity that will capture the actual device related information for admin.
 * Note: If you are wondering why do we not have Device extending DataSource?
 *     1) This class is the storage entity and not the actual business entity. Storage layers historically do not
 *        provide a way to reflect is-a relationships. You get around that limitation by adding a reference to the
 *        parent entity , datasourceId in our case.
 *     2) The {@code StorageManager} right now only supports operating over one {@code Storable} entity which maps to one table.
 *        Due to this restriction even if we wanted to create a Device class that extends DataSource and keep 2
 *        storage entities (in terms of RDBMS one Device object that gets stored in 2 tables, datasources and devices)
 *        it wont be supported by the manager right now.
 */
public class Device extends DataSourceSubType {
    public static final String NAME_SPACE = "devices";
    public static final String DEVICE_ID = "id";
    public static final String VERSION = "version";

    /**
     * NOTE: given we expect this to be part of the actual device message headers, this Id is kept as string.
     */
    private String deviceId;

    /**
     * Firmware version of the device. DeviceId + version has a unique constraint but is not the primary key.
     */
    private Long version;

    @JsonIgnore
    public String getNameSpace() {
        return NAME_SPACE;
    }

    @JsonIgnore
    public StorableKey getStorableKey() {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device)) return false;

        Device device = (Device) o;

        if (!dataSourceId.equals(device.dataSourceId)) return false;
        if (!deviceId.equals(device.deviceId)) return false;
        return version.equals(device.version);

    }

    @Override
    public int hashCode() {
        int result = deviceId.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + dataSourceId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Device{" +
                "deviceId='" + deviceId + '\'' +
                ", version=" + version +
                ", dataSourceId=" + dataSourceId +
                '}';
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String id) {
        this.deviceId = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonIgnore
    @Override
    public Long getId() {
        return super.getId();
    }
}
