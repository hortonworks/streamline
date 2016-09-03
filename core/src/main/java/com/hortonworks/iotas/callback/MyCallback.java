package com.hortonworks.iotas.callback;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.IOExceptionWithCause;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Created by Salil Kanetkar on 8/1/16.
 */
public class MyCallback {
    public static final String API_TOKEN_SMARTTHINGS = "c1bc8558-a1e3-4bf3-b07a-f8ae0f1cd697";
    public static final String API_ENDPOINT_SMARTTHINGS = "https://graph.api.smartthings.com/api/smartapps/installations/dc1bd5e0-b7fc-4bde-b1fc-4938f7d41285";
    //public static final String CLIENT_ID_NEST = "2aee5e52-0312-4414-8b0d-b2b86d767985";
    //public static final String CLIENT_SECRET_NEST = "QMXpGgnB1E4Xx3YNK3kYRr2YE";
    public static final String ACCESS_TOKEN_NEST = "c.UXX3TBLhcKCOvbXTR2tnA0avVdDhQORxJpKkRIflrEmIQT4iPJbxvMQa3WPB3M5OMCl8iuxm6EqLRX4bBGc9AkBh0PhiWj0EbDGPfUZGFdJrEafwk79sarQHVC8lq7M9CFCjbdIWd3VBPl1m";
    public static final String BASE_URL_NEST = "https://developer-api.nest.com";

    /*
    This method is used to obtain the id of the thermostat.
    It does a REST call to the NEST API to retrieve the same.
    */
    public String getNestThermostatID() throws IOException{
        String structure_url = BASE_URL_NEST + "/structures?auth=" + ACCESS_TOKEN_NEST;
        String body = new String();
        try {
            URL url = new URL(structure_url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            InputStream in = conn.getInputStream();
            String encoding = conn.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            Scanner s = new Scanner(in).useDelimiter("\\A");
            body = s.hasNext() ? s.next() : "";
            conn.disconnect();
        }
        catch(Exception e){
        }
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> nestStructure = mapper.readValue(body, Map.class);
        Map<String,Object> nestData = null;
        for (String name: nestStructure.keySet())
        {
            String key = name.toString();
            nestData = (Map<String, Object>) nestStructure.get(key);
            break; //TODO : For now, the code assumes that there is only one structure in the NEST data model
        }
        ArrayList<String> thermostatData = (ArrayList<String>) nestData.get("thermostats");
        /*
          TODO: The assumption made here is that, for every structure the callback is made to the first thermostat
          TODO: Hence, a get(0) is done in the code section below
          TODO: Ideally, the thermostat to which the callback should be made would be retrieved from the device registry
         */
        String thermostatIndex = thermostatData.get(0);
        return (thermostatIndex);
    }

    /*
    This method sets the NEST thermostats temperature using a PUT request to the NEST API.
     */
    public void putNestThermostatTemperature(String thermostat_id, String temperature){
        String testUrl = BASE_URL_NEST + "/devices/thermostats/" + thermostat_id + "/target_temperature_f?auth=" + ACCESS_TOKEN_NEST;
        try {
            URL url = new URL(testUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(temperature);
            out.flush();
            out.close();
            System.err.println(conn.getResponseCode());
        }
        catch(Exception e){
        }
    }

    /*
    This test manipulates the temperature of the NEST thermostat.
    As of now it randomly generates the temperature between (50, 90), which are the legal values.
     */
    //@Test
    public void myNESTModifier() throws IOException {
        String thermostat_id = getNestThermostatID();
        //System.out.println(thermostat_id);
        Random rand = new Random();
        int temperature = rand.nextInt((89 - 51) + 1) + 51; //Randomly generate the temperature
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        System.out.print("Enter the temperature: ");
        temperature = Integer.parseInt(br.readLine());
        //System.out.println(temperature);
        putNestThermostatTemperature(thermostat_id, Integer.toString(temperature));
    }

    /*
    This method is a GET call to obtain the current state of the SmartThings switch.
     */
    public String getSmartThingsSwitch(){
        String body = new String();
        try {
            String get_url = API_ENDPOINT_SMARTTHINGS + "/switches";
            URL url = new URL(get_url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + API_TOKEN_SMARTTHINGS);
            conn.setRequestMethod("GET");
            InputStream in = conn.getInputStream();
            String encoding = conn.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            //body = IOUtils.toString(in, encoding);
            Scanner s = new Scanner(in).useDelimiter("\\A");
            body = s.hasNext() ? s.next() : "";
            conn.disconnect();
            //System.out.println(body);
        }
        catch (Exception e){
        }
        return body;
    }

    /*
    This method is a PUT call to the SmartThings API to change the state of the switch.
     */
    public void putSmartThingsSwitch(String changeSwitchTo){
        try{
            String put_url = API_ENDPOINT_SMARTTHINGS + "/switches/" + changeSwitchTo;
            URL url = new URL(put_url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + API_TOKEN_SMARTTHINGS);
            conn.setDoOutput(true);
            conn.getResponseCode();
            conn.disconnect();
        }
        catch(Exception e){
        }
    }

    /*
    This a test which turns the SmartThings switch ON or OFF depending on its state.
    If it is ON, it would be turned OFF and vice-versa.
     */
    public void mySmartThingsModifier() throws IOException {
        String body = getSmartThingsSwitch();
        System.out.println(body);
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<Object> switchList = mapper.readValue(body, ArrayList.class);
        /*
        TODO: The code assumes that the callback is made to the first switch in the list of switches returned by the REST call
        TODO: Ideally, the information about which device to make the callback would be retrieved from
         */
        Map<String, Object> callbackSwitchInfo = (Map<String,Object>) switchList.get(0);
        String currentSwitchStatus = (String) callbackSwitchInfo.get("switch");
        if (currentSwitchStatus.equals("on"))
            putSmartThingsSwitch("off");
        else
            putSmartThingsSwitch("on");
    }

    public static void main(String[] args) throws IOException {
        //new MyCallback().myNESTModifier();
        new MyCallback().mySmartThingsModifier();
    }
}






