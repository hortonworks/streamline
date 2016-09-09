package com.hortonworks.iotas.notification.notifiers.device;

public class DeviceInfo {
    public static final String ID = "id";
    public static final String MAKE = "make";
    public static final String MODEL = "model";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";

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
