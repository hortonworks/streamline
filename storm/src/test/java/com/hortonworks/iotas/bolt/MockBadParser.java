package com.hortonworks.iotas.bolt;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.exception.DataValidationException;
import com.hortonworks.iotas.parser.BaseParser;
import com.hortonworks.iotas.exception.ParserException;

import java.util.List;
import java.util.Map;


/**
 * We can not use a mocking framework as these classes are loaded dynamically as part of parser bolts
 * using reflection. So we have to create mock class implementation.
 */
public class MockBadParser extends BaseParser {
    @Override
    public String version() {
        return null;
    }

    @Override
    public Schema schema() {
        return null;
    }

    @Override
    public Map<String, Object> parse(byte[] data) throws ParserException {
        throw new ParserException("test");
    }

    @Override
    public Map<String, Object> parse(String data) throws ParserException {
        throw new ParserException("test");
    }

    @Override
    public List<?> parseFields(byte[] data) throws ParserException {
        throw new ParserException("test");
    }


    @Override
    public List<?> parseFields(String data) throws ParserException {
        throw new ParserException("test");
    }

    @Override
    public void validate(byte[] rawData) throws DataValidationException {
        throw new DataValidationException("test");
    }

    @Override
    public void validate(Map<String, Object> parsedData) throws DataValidationException {
        throw new DataValidationException("test");
    }
}
