package com.hortonworks.iotas.parser;

import com.hortonworks.iotas.common.Schema;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.hortonworks.iotas.common.Schema.Field;

/**
 * Provides a skeletal implementation of the {@link Parser} interface. Parser implementations
 * are expected to extend this class.
 */
public abstract class BaseParser implements Parser {

    public Map<String, Object> parse(String data) throws ParseException {
        return parse(data.getBytes(Charset.forName("UTF-8")));
    }

    public List<Object> parseFields(byte[] data) throws ParseException {
        Map<String, Object> parsedData = parse(data);
        List<Object> fields = new ArrayList<Object>();
        for (Field f : schema().getFields()) {
            fields.add(parsedData.get(f.getName()));
        }
        return fields;
    }

    public List<Object> parseFields(String data) throws ParseException {
        return parseFields(data.getBytes(Charset.forName("UTF-8")));
    }

    /**
     * Can be used to construct the schema object automatically
     * in case of schemafull data format like json.
     *
     * @param data a sample data with all the fields.
     * @return the Schema for the data
     * @throws ParseException
     */
    public final Schema schemaFromSampleData(byte[] data) throws ParseException {
        return Schema.fromMapData(parse(data));
    }

    /**
     * Can be used to construct the schema object automatically
     * in case of schemafull data format like json.
     *
     * @param data a sample data with all the fields.
     * @return the Schema for the data
     * @throws ParseException
     */
    public final Schema schemaFromSampleData(String data) throws ParseException {
        return schemaFromSampleData(data.getBytes(Charset.forName("UTF-8")));
    }

}
