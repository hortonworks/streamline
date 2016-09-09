package com.hortonworks.iotas.callback;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotEquals;

public class SmartThingsSwitchCallbackTest {
    private SmartThingsSwitchCallback switchCallback;

    @Before
    public void setUp()
    {
        String smartThingsURL = "https://graph.api.smartthings.com/api/smartapps/installations/dc1bd5e0-b7fc-4bde-b1fc-4938f7d41285";
        String smartThingsAPIToken = "c1bc8558-a1e3-4bf3-b07a-f8ae0f1cd697";
        switchCallback = new SmartThingsSwitchCallback(smartThingsURL, smartThingsAPIToken);
    }

    //TODO: This test assumes that the user is logged into his smart things account
    @Test
    public void testSmartThingsSwitchCallback() throws IOException{
        try {
            String switchStatusBeforeSet = switchCallback.getSwitchStatus();
            if (switchStatusBeforeSet.equals("on"))
                switchCallback.setSwitch("off");
            else
                switchCallback.setSwitch("on");
            String switchStatusAfterSet = switchCallback.getSwitchStatus();
            //Thread.sleep(500); //change in the switch is not reflected instantly, hence the sleep()
            //assertNotEquals(switchStatusBeforeSet, switchStatusAfterSet);
        }
        catch (Exception e){
        }
    }
}

/*
NOTE: The best way to see if the test works is by seeing the physical device. Since we have a SmartThings switch, the LED goes ON/OFF according to the callback made.
YOu need to connect the HUB to a power outlet and insert an ethernet cable in it.
It would take 4-5 minutes for the HUB to become active.
Then connect the switch to a outlet and run the test.
Another way of seeing of the callback has worked is using the virtual devices provided on the SmartThings platform.
 */