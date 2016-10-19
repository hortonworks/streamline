package org.apache.streamline.streams.runtime.storm.bolt;

import org.apache.streamline.common.Schema;
import org.apache.streamline.common.exception.ParserException;
import org.apache.streamline.registries.parser.BaseParser;
import org.apache.streamline.registries.parser.exception.DataValidationException;

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
