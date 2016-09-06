package com.hortonworks.iotas.callback;

import org.junit.Test;
import java.io.IOException;
import java.util.Random;
import static org.junit.Assert.assertEquals;

public class NestThermostatCallbackTest {
    private NestThermostatCallback getNestThermostatID;
    private NestThermostatCallback putNestThermostatCallback;
    private NestThermostatCallback getNestThermostatTargetTemperatureF;

    @Test
    public void testNestThermostatCallback() throws IOException{
        getNestThermostatID = new NestThermostatCallback("https://developer-api.nest.com/structures?auth=c.UXX3TBLhcKCOvbXTR2tnA0avVdDhQORxJpKkRIflrEmIQT4iPJbxvMQa3WPB3M5OMCl8iuxm6EqLRX4bBGc9AkBh0PhiWj0EbDGPfUZGFdJrEafwk79sarQHVC8lq7M9CFCjbdIWd3VBPl1m");
        String thermostatID = getNestThermostatID.getNestThermostatID();
        Random rand = new Random();
        int temperature = rand.nextInt((90 - 50) + 1) + 51; //Randomly generate the temperature between 50 and 90
        putNestThermostatCallback = new NestThermostatCallback(("https://developer-api.nest.com/devices/thermostats/" + thermostatID + "/target_temperature_f?auth=c.UXX3TBLhcKCOvbXTR2tnA0avVdDhQORxJpKkRIflrEmIQT4iPJbxvMQa3WPB3M5OMCl8iuxm6EqLRX4bBGc9AkBh0PhiWj0EbDGPfUZGFdJrEafwk79sarQHVC8lq7M9CFCjbdIWd3VBPl1m"));
        putNestThermostatCallback.putNestThermostatTargetTemperatureF(Integer.toString(temperature));
        getNestThermostatTargetTemperatureF = new NestThermostatCallback("https://developer-api.nest.com/devices/thermostats/" + thermostatID + "/?auth=c.UXX3TBLhcKCOvbXTR2tnA0avVdDhQORxJpKkRIflrEmIQT4iPJbxvMQa3WPB3M5OMCl8iuxm6EqLRX4bBGc9AkBh0PhiWj0EbDGPfUZGFdJrEafwk79sarQHVC8lq7M9CFCjbdIWd3VBPl1m");
        int newTemperature = getNestThermostatTargetTemperatureF.getNestThermostatTargetTemperatureF();
        assertEquals(temperature, newTemperature);
    }
}