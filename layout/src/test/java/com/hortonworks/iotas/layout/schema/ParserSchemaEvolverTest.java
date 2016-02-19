package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import mockit.Expectations;
import mockit.Injectable;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class ParserSchemaEvolverTest {

    @Injectable
    CatalogService mockCatalogService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testEvolveWithParserId() throws IOException, BadComponentConfigException {
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

        Set<Stream> streams = evolver.apply(componentConfig, new Stream(IotasEventImpl.DEFAULT_SOURCE_STREAM, ""));
        assertEquals(streams.size(), 1);

        String expectedStreamName = (String) ((Map<String, Object>) componentConfigMap.get("config"))
                .get(TopologyLayoutConstants.JSON_KEY_PARSED_TUPLES_STREAM);
        assertTrue(streams.contains(new Stream(expectedStreamName, parserInfo.getParserSchema())));
    }

    @Test(expected = BadComponentConfigException.class)
    public void testEvolveParserIdProvidedButNonExistParser() throws IOException, BadComponentConfigException {
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
        evolver.apply(componentConfig, new Stream(""));
    }

    @Test
    public void testEvolveDataSourceIdProvided() throws Exception {
        final List<CatalogService.QueryParam> queryParams = Lists.newArrayList(
                new CatalogService.QueryParam("dataSourceId", "1"));
        final Collection<DataFeed> expectedDataFeeds = Lists.newArrayList(buildDataFeed());
        final ParserInfo parserInfo = buildParserInfo();

        new Expectations() {
            {
                mockCatalogService.listDataFeeds(queryParams); times = 1;
                result = expectedDataFeeds;
            }
            {
                mockCatalogService.getParserInfo(1L); times = 1;
                result = parserInfo;
            }
        };

        final ParserSchemaEvolver evolver = new ParserSchemaEvolver();
        evolver.setCatalogService(mockCatalogService);

        Map<String, Object> componentConfigMap = buildParserComponentConfigForDataSourceId();
        String componentConfig = objectMapper.writeValueAsString(componentConfigMap);

        Set<Stream> streams = evolver.apply(componentConfig, new Stream(IotasEventImpl.DEFAULT_SOURCE_STREAM, ""));
        assertEquals(streams.size(), 1);

        String expectedStreamName = (String) ((Map<String, Object>) componentConfigMap.get("config"))
                .get(TopologyLayoutConstants.JSON_KEY_PARSED_TUPLES_STREAM);
        assertTrue(streams.contains(new Stream(expectedStreamName, parserInfo.getParserSchema())));
    }

    @Test(expected = BadComponentConfigException.class)
    public void testEvolveDataSourceIdProvidedButNonExistDataFeed() throws Exception {
        final List<CatalogService.QueryParam> queryParams = Lists.newArrayList(
                new CatalogService.QueryParam("dataSourceId", "1"));
        final Collection<DataFeed> expectedDataFeeds = Lists.newArrayList();

        new Expectations() {
            {
                mockCatalogService.listDataFeeds(queryParams); times = 1;
                result = expectedDataFeeds;
            }
        };

        final ParserSchemaEvolver evolver = new ParserSchemaEvolver();
        evolver.setCatalogService(mockCatalogService);

        Map<String, Object> componentConfigMap = buildParserComponentConfigForDataSourceId();
        String componentConfig = objectMapper.writeValueAsString(componentConfigMap);

        // expected to throw BadComponentConfigException
        evolver.apply(componentConfig, new Stream(""));
    }


    @Test(expected = BadComponentConfigException.class)
    public void testEvolveDataSourceIdProvidedButNonExistParser() throws Exception {
        final List<CatalogService.QueryParam> queryParams = Lists.newArrayList(
                new CatalogService.QueryParam("dataSourceId", "1"));
        final Collection<DataFeed> expectedDataFeeds = Lists.newArrayList(buildDataFeed());

        new Expectations() {
            {
                mockCatalogService.listDataFeeds(queryParams); times = 1;
                result = expectedDataFeeds;
            }
            {
                mockCatalogService.getParserInfo(1L); times = 1;
                result = null;
            }
        };

        final ParserSchemaEvolver evolver = new ParserSchemaEvolver();
        evolver.setCatalogService(mockCatalogService);

        Map<String, Object> componentConfigMap = buildParserComponentConfigForDataSourceId();
        String componentConfig = objectMapper.writeValueAsString(componentConfigMap);

        // expected to throw BadComponentConfigException
        evolver.apply(componentConfig, new Stream(""));
    }

    private DataFeed buildDataFeed() {
        DataFeed dataFeed = new DataFeed();
        dataFeed.setId(1L);
        dataFeed.setName("dummy");
        dataFeed.setDataSourceId(1L);
        dataFeed.setType("KAFKA");
        dataFeed.setParserId(1L);
        return dataFeed;
    }

    private ParserInfo buildParserInfo() {
        ParserInfo parserInfo = new ParserInfo();
        parserInfo.setId(1L);
        parserInfo.setName("dummy");
        parserInfo.setClassName("dummy");
        parserInfo.setJarStoragePath("dummy");
        parserInfo.setTimestamp(1L);
        parserInfo.setVersion(1L);
        parserInfo.setParserSchema(EvolvingSchemaTestObject.inputStream().getSchema());
        return parserInfo;
    }

    private Map<String, Object> buildParserComponentConfig() throws IOException {
        Map<String, Object> componentConfig = Maps.newHashMap();
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_UINAME, "PARSER");
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_TYPE, "PARSER");
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_TRANSFORMATION_CLASS, "dummy");

        Map<String, Object> configMap = Maps.newHashMap();
        configMap.put(TopologyLayoutConstants.JSON_KEY_PARALLELISM, 1);
        configMap.put(TopologyLayoutConstants.JSON_KEY_PARSED_TUPLES_STREAM, "parsedTuplesStream");
        configMap.put(TopologyLayoutConstants.JSON_KEY_PARSER_ID, 1);

        componentConfig.put("config", configMap);

        return componentConfig;
    }

    private Map<String, Object> buildParserComponentConfigForDataSourceId() throws IOException {
        Map<String, Object> componentConfig = Maps.newHashMap();
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_UINAME, "PARSER");
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_TYPE, "PARSER");
        componentConfig.put(TopologyLayoutConstants.JSON_KEY_TRANSFORMATION_CLASS, "dummy");

        Map<String, Object> configMap = Maps.newHashMap();
        configMap.put(TopologyLayoutConstants.JSON_KEY_PARALLELISM, 1);
        configMap.put(TopologyLayoutConstants.JSON_KEY_PARSED_TUPLES_STREAM, "parsedTuplesStream");
        configMap.put(TopologyLayoutConstants.JSON_KEY_DATA_SOURCE_ID, 1);

        componentConfig.put("config", configMap);

        return componentConfig;
    }
}
