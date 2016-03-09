package com.hortonworks.iotas.parsers.nest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.parser.BaseParser;
import com.hortonworks.iotas.exception.ParserException;
import com.hortonworks.iotas.util.ReflectionHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by pbrahmbhatt on 7/30/15.
 */
public class NestParser extends BaseParser {

    private static ObjectMapper mapper = new ObjectMapper();
    private final String BATTERY_STATE = "battery_state";
    public String version() {
        return "1";
    }

    public Schema schema() {
        return Schema.of(
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
                new Schema.Field("hvac_state", Schema.Type.STRING),
                new Schema.Field(BATTERY_STATE, Schema.Type.INTEGER));
    }

    //right now it only returns current temperature and id, where id is nest's unique Id.
    //We can probably improve it by adding more fields
    public Map<String, Object> parse(byte[] data) throws ParserException {
        try {
            Map map = new HashMap();
            JsonNode jsonNode = mapper.readTree(data);
            JsonNode thermostats = jsonNode.get("devices").get("thermostats");
            Random random = new Random(System.currentTimeMillis());
            //TODO Not sure how do we want to deal with multiple thermostats in a single json response.
            //I guess we could return a list of Maps from parse method to allow for use cases like this.
            ObjectReader objectReader = mapper.readerFor(new TypeReference<Map<String, Thermostat>>() {/* no-op class*/ } );
            HashMap<String, Thermostat> thermostatMap = objectReader.readValue(thermostats);
            if(thermostatMap.size() > 1) {
                throw new ParserException("We don't deal with multiple thermo stat in a single json response yet");
            }
            Thermostat thermostat = thermostatMap.values().iterator().next();
            for(Schema.Field field: schema().getFields()) {
                String propertyName = field.getName();
                if (BATTERY_STATE.equals(propertyName)) {
                    map.put(propertyName, random.nextInt(20) + 1);
                }  else {
                    map.put(propertyName, ReflectionHelper.invokeGetter(propertyName, thermostat));
                }
            }
            return map;
        } catch (Exception e) {
            throw new ParserException(e);
        }
    }

}
