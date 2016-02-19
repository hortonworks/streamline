package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of EvolvingSchema which supports 'Parser' component.
 * Since this class retrieves Parser information from catalog, it implements CatalogServiceAware.
 */
public class ParserSchemaEvolver implements EvolvingSchema, CatalogServiceAware {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private CatalogService catalogService;

    @Override
    public Set<Stream> apply(String config, Stream inputStream) throws BadComponentConfigException {
        try {
            Map<String, Object> componentConfig = objectMapper.readValue(config, Map.class);
            Map<String, Object> parserConfig = (Map<String, Object>) componentConfig.get(
                    TopologyLayoutConstants.JSON_KEY_CONFIG);
            String parsedTuplesStream = (String) parserConfig.get("parsedTuplesStream");

            Long parserId = getParserId(parserConfig);
            ParserInfo parserInfo = catalogService.getParserInfo(parserId);
            if (parserInfo == null) {
                throw new RuntimeException("Parser ID " + parserId + " not found from catalog");
            }

            return Sets.newHashSet(new Stream(parsedTuplesStream, parserInfo.getParserSchema()));
        } catch (Exception e) {
            throw new BadComponentConfigException("Exception while simulating evolution of parser schema", e);
        }
    }

    @Override
    public void setCatalogService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    private Long getParserId(Map<String, Object> parserConfig) throws Exception {
        Long parserId;

        Number parserIdNum = ((Number) parserConfig.get("parserId"));
        if (parserIdNum == null) {
            Number dataSourceIdNum = (Number) parserConfig.get("dataSourceId");
            if (dataSourceIdNum == null) {
                throw new IllegalArgumentException("Either parserId or dataSourceId should be provided.");
            }

            Long dataSourceId = dataSourceIdNum.longValue();

            parserId = getAssociatedParserIdForDataSource(dataSourceId);
        } else {
            parserId = ((Number) parserConfig.get("parserId")).longValue();
        }
        return parserId;
    }

    private Long getAssociatedParserIdForDataSource(Long dataSourceId) throws Exception {
        List<CatalogService.QueryParam> queryParams = Lists.newArrayList(
                new CatalogService.QueryParam("dataSourceId", String.valueOf(dataSourceId)));
        Collection<DataFeed> dataFeeds = catalogService.listDataFeeds(queryParams);
        if (dataFeeds == null || dataFeeds.isEmpty()) {
            throw new RuntimeException("DataFeed for this data source id " + dataSourceId + " not found.");
        }

        DataFeed dataFeed = dataFeeds.iterator().next();

        return dataFeed.getParserId();
    }
}
