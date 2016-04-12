package com.hortonworks.iotas.parser;

import com.google.common.base.Charsets;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.exception.DataValidationException;
import com.hortonworks.iotas.exception.ParserException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hortonworks.iotas.common.Schema.Field;

/**
 * Provides a skeletal implementation of the {@link Parser} interface. Parser implementations
 * are expected to extend this class.
 */
public abstract class BaseParser implements Parser {

    public Map<String, Object> parse(String data) throws ParserException {
        return parse(data.getBytes(Charsets.UTF_8));
    }

    public List<?> parseFields(byte[] data) throws ParserException {
        Map<String, Object> parsedData = parse(data);
        List<Object> fields = new ArrayList<Object>();
        for (Field f : schema().getFields()) {
            fields.add(parsedData.get(f.getName()));
        }
        return fields;
    }

    public List<?> parseFields(String data) throws ParserException {
        return parseFields(data.getBytes(Charsets.UTF_8));
    }

    // The default implementation does not do any validation for raw bytes
    @Override
    public void validate(byte[] rawData) throws DataValidationException {
    }

    // The default implementation does not do any validation for parsed input
    @Override
    public void validate(Map<String, Object> parsedData) throws DataValidationException {
    }

    /**
     * Can be used to construct the schema object automatically
     * in case of schemaful data format like json.
     *
     * @param data a sample data with all the fields.
     * @return the Schema for the data
     * @throws ParserException
     */
    public final Schema schemaFromSampleData(byte[] data) throws ParserException {
        return Schema.fromMapData(parse(data));
    }

    /**
     * Can be used to construct the schema object automatically
     * in case of schemaful data format like json.
     *
     * @param data a sample data with all the fields.
     * @return the Schema for the data
     * @throws ParserException
     */
    public final Schema schemaFromSampleData(String data) throws ParserException {
        return schemaFromSampleData(data.getBytes(Charsets.UTF_8));
    }

}
