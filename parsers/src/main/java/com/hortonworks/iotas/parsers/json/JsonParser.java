package com.hortonworks.iotas.parsers.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.core.JsonGenerator;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.parser.BaseParser;
import com.hortonworks.iotas.exception.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple json parser that uses {@link ObjectMapper} to parse
 * json to a Map&lt;String, Object&gt;
 */
public class JsonParser extends BaseParser {
    private static final String VERSION = "1";
    private static final Logger LOG = LoggerFactory.getLogger(JsonParser.class);

    private Schema schema = null;
    private final ObjectMapper mapper = new ObjectMapper();

    public String version() {
        return VERSION;
    }

    public Schema schema() {
        return Schema.of(new Schema.Field("id", Schema.Type.STRING),
                         new Schema.Field("payload", Schema.Type.STRING));
    }

    public Map<String, Object> parse(byte[] data) throws ParserException {
        try {
            Map map = new HashMap();
            JsonNode jsonNode = mapper.readTree(data);
            ObjectMapper mapper = new ObjectMapper();
            mapper.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
            map.put("id", jsonNode.get("id"));
            map.put("payload", mapper.writeValueAsString(jsonNode.get("payload")));
            return map;
        } catch (IOException e) {
            throw new ParserException("Error trying to parse data.", e);
        }
    }
}
