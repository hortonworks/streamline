package com.hortonworks.parser;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.parser.ParseException;
import com.hortonworks.iotas.parser.Parser;
import org.apache.storm.guava.base.Charsets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                    new Schema.Field("temperature", Schema.Type.LONG),
                    new Schema.Field("id", Schema.Type.LONG)));
    }

    //right now it only returns current temperature and id, where id is nest's unique Id.
    //We can probably improve it by adding more fields
    public Map<String, Object> parse(byte[] data) throws ParseException {
        String msg = new String(data, Charsets.UTF_8);
        try {
            Map nestMessageMap = mapper.readValue(msg, Map.class);
            Map thermostats = (Map) ((Map)nestMessageMap.get("devices")).get("thermostats");
            if(thermostats.size() > 1) {
                throw new ParseException("cant have > 1 thermostats, found " + thermostats);
            }
            Set<Map.Entry> set = thermostats.entrySet();
            Map.Entry entry = set.iterator().next();

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", entry.getKey());
            map.put("temperature", ((Map) entry.getValue()).get("ambient_temperature_f"));
            return map;
        } catch (IOException e) {
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
