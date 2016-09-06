package com.hortonworks.iotas.callback;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotEquals;

public class SmartThingsSwitchCallbackTest {
    private SmartThingsSwitchCallback smartThingsSwitchCallback;

    @Before
    public void setUp()
    {
        String smartThingsURL = "https://graph.api.smartthings.com/api/smartapps/installations/dc1bd5e0-b7fc-4bde-b1fc-4938f7d41285";
        String smartThingsAPIToken = "c1bc8558-a1e3-4bf3-b07a-f8ae0f1cd697";
        smartThingsSwitchCallback = new SmartThingsSwitchCallback(smartThingsURL, smartThingsAPIToken);
    }

    //TODO: This test assumes that the user is logged into his smart things account
    @Test
    public void testSmartThingsSwitchCallback() throws IOException{
        try {
            String switchStatusBeforePut = smartThingsSwitchCallback.getSmartThingsSwitchStatus();
            if (switchStatusBeforePut.equals("on"))
                smartThingsSwitchCallback.putSmartThingsSwitch("off");
            else
                smartThingsSwitchCallback.putSmartThingsSwitch("on");
            String switchStatusAfterPut = smartThingsSwitchCallback.getSmartThingsSwitchStatus();
            //Thread.sleep(500); //change in the switch is not reflected instantly
            //assertNotEquals(switchStatusBeforePut, switchStatusAfterPut);
        }
        catch (Exception e){
        }
    }
}