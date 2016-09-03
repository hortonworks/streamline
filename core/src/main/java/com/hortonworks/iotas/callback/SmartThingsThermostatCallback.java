package com.hortonworks.iotas.callback;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by Salil Kanetkar on 8/16/16.
 */
public class SmartThingsThermostatCallback {
    private String smartThingsURL;
    private String smartThingsAPIToken;

    public SmartThingsThermostatCallback(String smartThingsURL, String smartThingsAPIToken){
        this.smartThingsURL = smartThingsURL;
        this.smartThingsAPIToken = smartThingsAPIToken;
    }

    public String getSmartThingsThermostatInformation(){
        String body = new String();
        try {
            String get_url = smartThingsURL + "/thermostat";
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

    public int getSmartThingsThermostatHeatingSetpoint() throws IOException {
        String body = getSmartThingsThermostatInformation();
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> thermostatInfo = mapper.readValue(body,Map.class);
        int currentHeatingSetpoint = (int) thermostatInfo.get("heating_setpoint");
        return currentHeatingSetpoint;
    }

    public void putSmartThingsThermostatHeatingSetpoint(int newHeatingSetpoint){
        try {
            String put_url = smartThingsURL + "/thermostat/" + Integer.toString(newHeatingSetpoint);
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

    public void process() throws IOException{
        int currentHeatingSetpoint = getSmartThingsThermostatHeatingSetpoint();
        putSmartThingsThermostatHeatingSetpoint(currentHeatingSetpoint - 1);
    }
}