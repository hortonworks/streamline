package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.DataSourceSubType;
import com.hortonworks.iotas.storage.PrimaryKey;

import java.util.HashMap;
import java.util.Map;

import static com.hortonworks.iotas.common.Schema.Field;


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
public class Device implements DataSourceSubType {

    public static final String DEVICE_ID = "deviceId";
    public static final String VERSION = "version";
    public static final String DATA_SOURCE_ID = "dataSourceId";

    /**
     * NOTE: given we expect this to be part of the actual device message headers, this Id is kept as string.
     */
    private String deviceId;

    /**
     * Firmware version of the device. DeviceId + version is the primary key.
     */
    private Long version;

    /**
     * Reference to the datasource.
     */
    private Long dataSourceId;

    @JsonIgnore
    public String getNameSpace() {
        return "devices";
    }

    @JsonIgnore
    public Schema getSchema() {
        return new Schema.SchemaBuilder().fields(new Field(DEVICE_ID, Schema.Type.STRING),
                                                 new Field(DATA_SOURCE_ID, Schema.Type.LONG),
                                                 new Field(VERSION, Schema.Type.LONG)).build();
    }

    /**
     * The primary key of the device is the datasource id itself which is also a foreign key
     * reference to the parent 'DataSource'.
     */
    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field(DATA_SOURCE_ID, Schema.Type.LONG), this.dataSourceId);
        return new PrimaryKey(fieldToObjectMap);
    }

    public Map toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DEVICE_ID, this.deviceId);
        map.put(VERSION, this.version);
        map.put(DATA_SOURCE_ID, this.dataSourceId);
        return map;
    }

    public Device fromMap(Map<String, Object> map) {
        this.deviceId = (String)  map.get(DEVICE_ID);
        this.version = (Long)  map.get(VERSION);
        this.dataSourceId = (Long) map.get(DATA_SOURCE_ID);
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device)) return false;

        Device device = (Device) o;

        if (!deviceId.equals(device.deviceId)) return false;
        if (!version.equals(device.version)) return false;
        return dataSourceId.equals(device.dataSourceId);

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

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonIgnore
    public Long getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(Long dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

}
