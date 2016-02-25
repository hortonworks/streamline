package com.hortonworks.iotas.layout.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;

import java.util.HashMap;
import java.util.Map;

public class ParserSchemaEvolver implements EvolvingSchema, CatalogServiceAware {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private CatalogService catalogService;

    @Override
    public Map<String, Schema> apply(String config, Schema inputSchema) throws BadComponentConfigException {
        try {
            Map<String, Object> componentConfig = objectMapper.readValue(config, Map.class);
            Map<String, Object> parserConfig = (Map<String, Object>) componentConfig.get(
                    TopologyLayoutConstants.JSON_KEY_CONFIG);
            String parsedTuplesStream = (String) parserConfig.get("parsedTuplesStream");
            // TODO: decide parserId field to be mandatory again or not
            Long parserId;
            try {
                parserId = (Long) parserConfig.get("parserId");
            } catch (ClassCastException e) {
                parserId = Long.valueOf((Integer) parserConfig.get("parserId"));
            }

            ParserInfo parserInfo = catalogService.getParserInfo(parserId);
            if (parserInfo == null) {
                throw new RuntimeException("Parser ID " + parserId + " not found from catalog");
            }

            HashMap<String, Schema> applied = Maps.newHashMap();
            applied.put(parsedTuplesStream, parserInfo.getSchema());
            return applied;
        } catch (Exception e) {
            throw new BadComponentConfigException("Exception while simulating evolution of parser schema", e);
        }
    }

    @Override
    public void setCatalogService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }
}
