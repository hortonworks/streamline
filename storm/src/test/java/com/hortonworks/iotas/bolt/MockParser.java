package com.hortonworks.iotas.bolt;

import com.google.common.collect.Lists;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.parser.ParseException;
import com.hortonworks.iotas.parser.Parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * We can not use a mocking framework as these classes are loaded dynamically as part of parser bolts
 * using reflection. So we have to create mock class implementation.
 */
public class MockParser implements Parser {

    public static final Map<String, Object> PARSER_OUTPUT = new HashMap<String, Object>() {{
        put("a","b");
    }};

    public static final IotasEvent IOTAS_EVENT = new IotasEventImpl(PARSER_OUTPUT, "dsrcid");

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
        return PARSER_OUTPUT;
    }

    @Override
    public Map<String, Object> parse(String data) throws ParseException {
        return PARSER_OUTPUT;
    }

    @Override
    public List<Object> parseFields(byte[] data) throws ParseException {
        return Lists.newArrayList(PARSER_OUTPUT.values());
    }

    @Override
    public List<Object> parseFields(String data) throws ParseException {
        return Lists.newArrayList(PARSER_OUTPUT.values());
    }
}
