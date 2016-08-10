package com.hortonworks.iotas.callback;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

public class NestThermostatCallback {
    private String nestURL;

    public NestThermostatCallback(String nestURL) {
        this.nestURL = nestURL;
    }

    /*
      This method is used to obtain information from the nest data model.
      It does a REST call to the NEST API to retrieve the same.
    */
    public Map<String, Object> getNestInformation() throws IOException {
        String body = new String();
        try {
            URL url = new URL(nestURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            InputStream in = conn.getInputStream();
            String encoding = conn.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            Scanner s = new Scanner(in).useDelimiter("\\A");
            body = s.hasNext() ? s.next() : "";
            conn.disconnect();
        } catch (Exception e) {
        }
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> nestInformation = mapper.readValue(body, Map.class);
        return nestInformation;
    }


    /*
     This method retrieves the NEST thermostat ID
    */
    public String getNestThermostatID() throws IOException {
        Map<String, Object> nestStructure = getNestInformation();
        Map<String, Object> nestData = null;
        for (String name : nestStructure.keySet()) {
            String key = name.toString();
            nestData = (Map<String, Object>) nestStructure.get(key);
            break; //TODO : For now, the code assumes that there is only one structure in the NEST data model
        }
        ArrayList<String> thermostatData = (ArrayList<String>) nestData.get("thermostats");
        /*
          TODO: The assumption made here is that, for every structure the callback is made to the first thermostat
          Hence, a get(0) is done in the code section below
          Ideally, the thermostat to which the callback should be made would be retrieved from the device registry
         */
        String thermostatIndex = thermostatData.get(0);
        return (thermostatIndex);
    }


    /*
      This method retrieves the NEST thermostats temperature in Degree F
    */
    public int getNestThermostatTargetTemperatureF() throws IOException{
        Map<String, Object> thermostatStructure = getNestInformation();
        int temperature = (int) thermostatStructure.get("target_temperature_f");
        return temperature;
    }


    /*
      This method sets the NEST thermostats temperature using a PUT request to the NEST API.
    */
    public void putNestThermostatTargetTemperatureF(String temperature) {
        try {
            URL url = new URL(nestURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(temperature);
            out.flush();
            out.close();
            int responseCode = conn.getResponseCode();
        } catch (Exception e) {
        }
    }
}