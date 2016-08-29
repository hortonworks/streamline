package com.hortonworks.iotas.notification.notifiers.device;

public class DeviceIdentificationField {
    private String id;
    private String make;
    private String model;
    private String name;
    private String description;

    //for jackson
    public DeviceIdentificationField(){
    }

    public DeviceIdentificationField(String id, String make, String model, String name, String description) {
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
        if (!(o instanceof DeviceIdentificationField)) return false;

        DeviceIdentificationField that = (DeviceIdentificationField) o;

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
        return "DeviceIdentificationField{" +
                "id='" + id + '\'' +
                ", make='" + make + '\'' +
                ", model='" + model + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
