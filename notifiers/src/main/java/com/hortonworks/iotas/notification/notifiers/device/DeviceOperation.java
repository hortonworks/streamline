package com.hortonworks.iotas.notification.notifiers.device;

/**
 * Represents an operation for the device. E.g A device like 'Thermostat' could
 * have operations like setTemperature(), setHumidity(), setFanSpeed() etc.
 */
import com.hortonworks.iotas.common.Schema;

import java.util.List;

public class DeviceOperation {
    private String name;
    private int numParameters;
    private List<Schema.Field> params;

    public DeviceOperation() {
    }

    public DeviceOperation(String name, int numParameters, List<Schema.Field> params) {
        this.name = name;
        this.numParameters = numParameters;
        this.params = params;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumParameters() {
        return numParameters;
    }

    public void setNumParameters(int numParameters) {
        this.numParameters = numParameters;
    }

    public List<Schema.Field> getParams() {
        return params;
    }

    public void setParams(List<Schema.Field> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "DeviceOperations{" +
                "name='" + name + '\'' +
                ", numParameters='" + numParameters + '\'' +
                ", params=" + params +
                '}';
    }
}
