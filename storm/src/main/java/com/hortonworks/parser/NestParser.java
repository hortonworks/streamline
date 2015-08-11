package com.hortonworks.parser;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.parser.Parser;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pbrahmbhatt on 7/30/15.
 */
public class NestParser implements Parser {

    public String version() {
        return "1.1.3.8";
    }

    public Schema schema() {
        return new Schema(
                new Schema.Field("userId", Schema.Type.LONG),
                new Schema.Field("temperature", Schema.Type.LONG),
                new Schema.Field("eventTime", Schema.Type.LONG),
                new Schema.Field("longitude", Schema.Type.LONG),
                new Schema.Field("latitude", Schema.Type.LONG)
        );
    }

    public Map<String, Object> parse(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        long userId = buffer.getLong();
        long temperature = buffer.getLong();
        long eventTime = buffer.getLong();
        long longitude = buffer.getLong();
        long latitude = buffer.getLong();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("userId", userId);
        map.put("temperature", temperature);
        map.put("eventTime", eventTime);
        map.put("longitude", longitude);
        map.put("latitude", latitude);
        return map;
    }
}
