package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.service.CatalogService;
import mockit.Expectations;
import mockit.Injectable;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class ParserSchemaEvolverTest {

    @Injectable
    CatalogService mockCatalogService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testEvolve() throws IOException, BadComponentConfigException {
        final ParserInfo parserInfo = buildParserInfo();

        new Expectations() {
            {
                mockCatalogService.getParserInfo(1L); times = 1;
                result = parserInfo;
            }
        };

        final ParserSchemaEvolver evolver = new ParserSchemaEvolver();
        evolver.setCatalogService(mockCatalogService);

        Map<String, Object> componentConfigMap = buildParserComponentConfig();
        String componentConfig = objectMapper.writeValueAsString(componentConfigMap);

        Map<String, Schema> schemaMap = evolver.apply(componentConfig, new Schema());
        assertEquals(schemaMap.size(), 1);

        assertOutputStreamWithSchema(schemaMap, (String) ((Map<String, Object>) componentConfigMap.get("config"))
                .get("parsedTuplesStream"), parserInfo.getSchema());
    }

    @Test(expected = BadComponentConfigException.class)
    public void testEvolveNonExistParser() throws IOException, BadComponentConfigException {
        new Expectations() {
            {
                mockCatalogService.getParserInfo(1L); times = 1;
                result = null;
            }
        };

        final ParserSchemaEvolver evolver = new ParserSchemaEvolver();
        evolver.setCatalogService(mockCatalogService);

        Map<String, Object> componentConfigMap = buildParserComponentConfig();
        String componentConfig = objectMapper.writeValueAsString(componentConfigMap);

        // expected to throw BadComponentConfigException
        evolver.apply(componentConfig, new Schema());
    }

    private void assertOutputStreamWithSchema(Map<String, Schema> schemaMap, String expectedStreamName, Schema schema) {
        assertTrue(schemaMap.containsKey(expectedStreamName));
        Schema streamSchema = schemaMap.get(expectedStreamName);
        assertEquals(schema, streamSchema);
    }

    private ParserInfo buildParserInfo() {
        ParserInfo parserInfo = new ParserInfo();
        parserInfo.setId(1L);
        parserInfo.setName("dummy");
        parserInfo.setClassName("dummy");
        parserInfo.setJarStoragePath("dummy");
        parserInfo.setTimestamp(1L);
        parserInfo.setVersion(1L);
        parserInfo.setParserSchema(buildInputSchema());
        return parserInfo;
    }

    private Map<String, Object> buildParserComponentConfig() throws IOException {
        Map<String, Object> componentConfig = Maps.newHashMap();
        componentConfig.put("uiname", "PARSER");
        componentConfig.put("type", "PARSER");
        componentConfig.put("transformationClass", "dummy");

        Map<String, Object> configMap = Maps.newHashMap();
        configMap.put("parallelism", 1);
        configMap.put("parsedTuplesStream", "parsedTuplesStream");
        configMap.put("parserId", 1);

        componentConfig.put("config", configMap);

        return componentConfig;
    }

    private Schema buildInputSchema() {
        Schema.SchemaBuilder schemaBuilder = new Schema.SchemaBuilder();

        schemaBuilder.field(new Schema.Field("field1", Schema.Type.STRING));
        schemaBuilder.field(new Schema.Field("field2", Schema.Type.LONG));
        schemaBuilder.field(new Schema.Field("field3", Schema.Type.STRING));

        return schemaBuilder.build();
    }
}
