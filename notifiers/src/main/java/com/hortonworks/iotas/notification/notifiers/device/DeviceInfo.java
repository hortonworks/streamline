package com.hortonworks.iotas.notification.notifiers.device;

public class DeviceInfo {
    public static final String DEVICE_ID = "id";
    public static final String DEVICE_MAKE = "make";
    public static final String DEVICE_MODEL = "model";
    public static final String DEVICE_NAME = "name";
    public static final String DEVICE_DESCRIPTION = "description";

    /**
     * A unique alphanumeric id to uniquely identify a device.
     */
    private String id;

    /**
     * The make of the device
     */
    private String make;

    /**
     * The model of the device
     */
    private String model;

    /**
     * The name of the device, like 'Thermostat' or 'Switch'
     */
    private String name;

    /**
     * The description provided about the IoT device
     */
    private String description;

    //for jackson
    public DeviceInfo(){
    }

    public DeviceInfo(String id, String make, String model, String name, String description) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceInfo)) return false;

        DeviceInfo that = (DeviceInfo) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (make != null ? !make.equals(that.make) : that.make != null) return false;
        if (model != null ? !model.equals(that.model) : that.model != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return description != null ? description.equals(that.description) : that.description == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (make != null ? make.hashCode() : 0);
        result = 31 * result + (model != null ? model.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "id='" + id + '\'' +
                ", make='" + make + '\'' +
                ", model='" + model + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
