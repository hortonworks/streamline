package com.hortonworks.iotas.callback;

import org.junit.Test;
import java.io.IOException;
import java.util.Random;
import static org.junit.Assert.assertEquals;

public class NestThermostatCallbackTest {
    private NestThermostatCallback getID;
    private NestThermostatCallback callback;
    private NestThermostatCallback getTemperatureF;
    private static final String API_KEY = "c.UXX3TBLhcKCOvbXTR2tnA0avVdDhQORxJpKkRIflrEmIQT4iPJbxvMQa3WPB3M5OMCl8iuxm6EqLRX4bBGc9AkBh0PhiWj0EbDGPfUZGFdJrEafwk79sarQHVC8lq7M9CFCjbdIWd3VBPl1m";

    @Test
    public void testNestThermostatCallback() throws IOException{
        getID = new NestThermostatCallback("https://developer-api.nest.com/structures?auth=" + API_KEY);
        String thermostatID = getID.getID();
        Random rand = new Random();
        int temperature = rand.nextInt((90 - 50) + 1) + 51; //Randomly generate the temperature between 50 and 90
        callback = new NestThermostatCallback(("https://developer-api.nest.com/devices/thermostats/" + thermostatID + "/target_temperature_f?auth=" + API_KEY));
        callback.setTemperatureF(Integer.toString(temperature));
        getTemperatureF = new NestThermostatCallback("https://developer-api.nest.com/devices/thermostats/" + thermostatID + "/?auth=" + API_KEY);
        int newTemperature = getTemperatureF.getTemperatureF();
        assertEquals(temperature, newTemperature);
    }
}

/*
NOTE: The test can fail because the API key has expired or the REST call to the NEST api did not work properly.
Another reason is that, it takes a few milliseconds for the callback to get reflected in the NEST data model.
So even if the temperature has been changed successfully, the old temperature might be returned. Run the test again.
 */