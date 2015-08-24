package com.hortonworks.iotas.parsers.nest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.Lists;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.parser.ParseException;
import com.hortonworks.iotas.parser.Parser;
import com.hortonworks.util.ReflectionHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pbrahmbhatt on 7/30/15.
 */
public class NestParser implements Parser {

    private static ObjectMapper mapper = new ObjectMapper();

    public String version() {
        return "1";
    }

    public Schema schema() {
        return new Schema(Lists.newArrayList(
                new Schema.Field("device_id", Schema.Type.STRING),
                new Schema.Field("locale", Schema.Type.STRING),
                new Schema.Field("software_version", Schema.Type.STRING),
                new Schema.Field("structure_id", Schema.Type.STRING),
                new Schema.Field("name", Schema.Type.STRING),
                new Schema.Field("name_long", Schema.Type.STRING),
                new Schema.Field("last_connection", Schema.Type.STRING),
                new Schema.Field("is_online", Schema.Type.STRING),
                new Schema.Field("can_cool", Schema.Type.STRING),
                new Schema.Field("can_heat", Schema.Type.STRING),
                new Schema.Field("is_using_emergency_heat", Schema.Type.STRING),
                new Schema.Field("has_fan", Schema.Type.STRING),
                new Schema.Field("fan_timer_active", Schema.Type.STRING),
                new Schema.Field("fan_timer_timeout", Schema.Type.STRING),
                new Schema.Field("has_leaf", Schema.Type.STRING),
                new Schema.Field("temperature_scale", Schema.Type.STRING),
                new Schema.Field("target_temperature_f", Schema.Type.STRING),
                new Schema.Field("target_temperature_c", Schema.Type.STRING),
                new Schema.Field("target_temperature_high_f", Schema.Type.STRING),
                new Schema.Field("target_temperature_high_c", Schema.Type.STRING),
                new Schema.Field("target_temperature_low_f", Schema.Type.STRING),
                new Schema.Field("target_temperature_low_c", Schema.Type.STRING),
                new Schema.Field("away_temperature_high_f", Schema.Type.STRING),
                new Schema.Field("away_temperature_high_c", Schema.Type.STRING),
                new Schema.Field("away_temperature_low_f", Schema.Type.STRING),
                new Schema.Field("away_temperature_low_c", Schema.Type.STRING),
                new Schema.Field("hvac_mode", Schema.Type.STRING),
                new Schema.Field("ambient_temperature_f", Schema.Type.STRING),
                new Schema.Field("ambient_temperature_c", Schema.Type.STRING),
                new Schema.Field("humidity", Schema.Type.STRING),
                new Schema.Field("hvac_state", Schema.Type.STRING)));
    }

    //right now it only returns current temperature and id, where id is nest's unique Id.
    //We can probably improve it by adding more fields
    public Map<String, Object> parse(byte[] data) throws ParseException {
        try {
            Map map = new HashMap();
            JsonNode jsonNode = mapper.readTree(data);
            JsonNode thermostats = jsonNode.get("devices").get("thermostats");
            //TODO Not sure how do we want to deal with multiple thermostats in a single json response.
            //I guess we could return a list of Maps from parse method to allow for use cases like this.
            ObjectReader objectReader = mapper.readerFor(new TypeReference<Map<String, Thermostat>>() {
            });
            HashMap<String, Thermostat> thermostatMap = objectReader.readValue(thermostats);
            if(thermostatMap.size() > 1) {
                throw new ParseException("We don't deal with multiple thermo stat in a single json response yet");
            }
            Thermostat thermostat = thermostatMap.values().iterator().next();
            for(Schema.Field field: schema().getFields()) {
                String propertyName = field.getName();
                map.put(propertyName, ReflectionHelper.invokeGetter(propertyName, thermostat));
            }
            return map;
        } catch (Exception e) {
            throw new ParseException(e);
        }
    }

    public Map<String, Object> parse(String data) throws ParseException {
        return null;
    }

    public List<Object> parseFields(byte[] data) throws ParseException {
        return null;
    }

    public List<Object> parseFields(String data) throws ParseException {
        return null;
    }
}
