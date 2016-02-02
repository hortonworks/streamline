package com.hortonworks.iotas.parsers.yaf;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.parser.BaseParser;
import com.hortonworks.iotas.parser.ParseException;

/**
 * Created by jsirota on 1/2/16.
 */
public class YafParser extends BaseParser {

    private static ObjectMapper mapper = new ObjectMapper();

    public String version() {
        return "1";
    }

     
    public Schema schema() {
        return new Schema(Lists.newArrayList(
                new Schema.Field("start-time", Schema.Type.STRING),
                new Schema.Field("end-time", Schema.Type.STRING),
                new Schema.Field("duration", Schema.Type.LONG),
                new Schema.Field("rtt", Schema.Type.LONG),
                new Schema.Field("proto", Schema.Type.INTEGER),
                new Schema.Field("sip", Schema.Type.STRING),
                new Schema.Field("sp", Schema.Type.INTEGER),
                new Schema.Field("dip", Schema.Type.STRING),
                new Schema.Field("dp", Schema.Type.INTEGER)
                
                //don't care about the rest of it for now
                ));
    }

    //right now it only returns current temperature and id, where id is nest's unique Id.
    //We can probably improve it by adding more fields
    public Map<String, Object> parse(byte[] data) throws ParseException {
        try {
            Map map = new HashMap();
            
            String decoded = new String(data, "UTF-8");
            
            String parts[] = decoded.split("\\|");

            int cnt = 0;
            for(Schema.Field field: schema().getFields()) {
                String propertyName = field.getName();
                map.put(propertyName, parts[cnt].trim());
                
                cnt++;
            }
            return map;
        } catch (Exception e) {
            throw new ParseException(e);
        }
    }

}
