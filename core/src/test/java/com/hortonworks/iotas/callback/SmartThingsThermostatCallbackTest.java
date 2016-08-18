package com.hortonworks.iotas.callback;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Salil Kanetkar on 8/16/16.
 */
public class SmartThingsThermostatCallbackTest {
    private  SmartThingsThermostatCallback smartThingsThermostatCallback;

    @Before
    public void setUp()
    {
        String smartThingsURL = "https://graph.api.smartthings.com/api/smartapps/installations/8b09fe00-4e13-4c67-b8a1-9f6d9f1a7534";
        String smartThingsAPIToken = "c6654175-2931-4881-b862-05cb031bbb57";
        smartThingsThermostatCallback = new SmartThingsThermostatCallback(smartThingsURL, smartThingsAPIToken);
    }

    @Test
    public void testSmartThingsSwitchCallback() throws IOException {
        try {
            int currentThermostatHeatingSetpoint = smartThingsThermostatCallback.getSmartThingsThermostatHeatingSetpoint();
            smartThingsThermostatCallback.process();
            Thread.sleep(1000);
            int newThermostatHeatingSetpoint = smartThingsThermostatCallback.getSmartThingsThermostatHeatingSetpoint();
            assert((currentThermostatHeatingSetpoint - newThermostatHeatingSetpoint) == 1);
        }
        catch (Exception e){
        }
    }
}
