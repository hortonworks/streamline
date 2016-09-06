package com.hortonworks.iotas.callback;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

public class SmartThingsSwitchCallback {
    private String smartThingsURL;
    private String smartThingsAPIToken;

    public SmartThingsSwitchCallback(String smartThingsURL, String smartThingsAPIToken){
        this.smartThingsURL = smartThingsURL;
        this.smartThingsAPIToken = smartThingsAPIToken;
    }

    /*
    This method is a GET call to obtain the different fields of the SmartThings switch.
     */
    public String getSmartThingsSwitchInformation(){
        String body = new String();
        try {
            String get_url = smartThingsURL + "/switches";
            URL url = new URL(get_url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + smartThingsAPIToken);
            conn.setRequestMethod("GET");
            InputStream in = conn.getInputStream();
            String encoding = conn.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            Scanner s = new Scanner(in).useDelimiter("\\A");
            body = s.hasNext() ? s.next() : "";
            conn.disconnect();
        }
        catch (Exception e){
        }
        return body;
    }

    public String getSmartThingsSwitchStatus() throws IOException{
        String body = getSmartThingsSwitchInformation();
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<Object> switchList = mapper.readValue(body, ArrayList.class);
        /*
        TODO: The code assumes that the callback is made to the first switch in the list of switches returned by the REST call
        TODO: Ideally, the information about which device to make the callback would be retrieved from the device registry
         */
        Map<String, Object> callbackSwitchInfo = (Map<String,Object>) switchList.get(0);
        String currentSwitchStatus = (String) callbackSwitchInfo.get("switch");
        return currentSwitchStatus;
    }


    /*
    This method is a PUT call to the SmartThings API to change the state of the switch.
     */
    public void putSmartThingsSwitch(String changeSwitchTo){
        try {
            String put_url = smartThingsURL + "/switches/" + changeSwitchTo;
            URL url = new URL(put_url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + smartThingsAPIToken);
            conn.setDoOutput(true);
            conn.getResponseCode();
            conn.disconnect();
        }
        catch(Exception e){
        }
    }
}