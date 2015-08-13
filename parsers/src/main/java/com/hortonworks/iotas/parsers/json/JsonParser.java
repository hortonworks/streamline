package com.hortonworks.iotas.parsers.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.parser.BaseParser;
import com.hortonworks.iotas.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * A simple json parser that uses {@link ObjectMapper} to parse
 * json to a Map&lt;String, Object&gt;
 */
public class JsonParser extends BaseParser {
    private static final String VERSION = "1.0";
    private static final Logger LOG = LoggerFactory.getLogger(JsonParser.class);

    private Schema schema = null;
    private final ObjectMapper mapper = new ObjectMapper();

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    /**
     * For self describing data formats like Json we can construct the schema from
     * sample data.
     *
     * @param sampleJsonData
     * @throws ParseException
     */
    public void setSchema(String sampleJsonData) throws ParseException {
        this.schema = schemaFromSampleData(sampleJsonData);
    }

    public String version() {
        return VERSION;
    }

    public Schema schema() {
        return schema;
    }

    public Map<String, Object> parse(byte[] data) throws ParseException {
        try {
            return mapper.readValue(data, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            LOG.error("Error trying to parse data {}", new String(data, Charset.forName("UTF-8")));
            throw new ParseException("Error trying to parse data.", e);
        }
    }
}
