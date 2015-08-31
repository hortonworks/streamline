package com.hortonworks.bolt;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.parser.ParseException;
import com.hortonworks.iotas.parser.Parser;

import java.util.List;
import java.util.Map;


/**
 * We can not use a mocking frameowrk as these classes are loaded dynamically as part of parser bolts
 * using reflection. So we have to create mock class implementation.
 */
public class MockBadParser implements Parser {
    @Override
    public String version() {
        return null;
    }

    @Override
    public Schema schema() {
        return null;
    }

    @Override
    public Map<String, Object> parse(byte[] data) throws ParseException {
        throw new ParseException("test");
    }

    @Override
    public Map<String, Object> parse(String data) throws ParseException {
        throw new ParseException("test");
    }

    @Override
    public List<Object> parseFields(byte[] data) throws ParseException {
        throw new ParseException("test");
    }

    @Override
    public List<Object> parseFields(String data) throws ParseException {
        throw new ParseException("test");
    }
}
