package com.hortonworks.iotas.webservice.catalog;

import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.catalog.dto.DataSourceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class DataSourceFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFacade.class);

    private final CatalogService catalogService;

    public DataSourceFacade(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public DataSourceDto addOrUpdateDataSourceWithDataFeed(Long dataSourceId, DataSourceDto dataSourceDto) throws Exception {
        DataSource dataSource = createDataSource(dataSourceDto);
        DataSource createdDataSource = catalogService.addOrUpdateDataSource(dataSourceId, dataSource);
        dataSourceDto.setDataSourceId(createdDataSource.getDataSourceId());

        DataFeed dataFeed = createDataFeed(dataSourceDto);
        DataFeed existingDataFeed = getDataFeed(dataSourceId);
        DataFeed createdDataFeed = existingDataFeed != null
                ? catalogService.addOrUpdateDataFeed(existingDataFeed.getDataFeedId(), dataFeed)
                : catalogService.addDataFeed(dataFeed);

        return new DataSourceDto(createdDataSource, createdDataFeed);
    }

    public DataSourceDto createDataSourceWithDataFeed(DataSourceDto dataSourceDto) throws Exception {
        DataSource dataSource = createDataSource(dataSourceDto);
        DataSource createdDataSource = catalogService.addDataSource(dataSource);
        dataSourceDto.setDataSourceId(createdDataSource.getDataSourceId());

        DataFeed dataFeed = createDataFeed(dataSourceDto);
        DataFeed createdDataFeed = catalogService.addDataFeed(dataFeed);

        DataSourceDto createdDataSourceDto = new DataSourceDto(createdDataSource, createdDataFeed);
        ParserInfo parserInfo = catalogService.getParserInfo(dataSourceDto.getParserId());
        if (parserInfo != null) {
            createdDataSourceDto.setParserName(parserInfo.getParserName());
        }

        return createdDataSourceDto;
    }

    private DataFeed createDataFeed(DataSourceDto dataSourceDto) {
        DataFeed dataFeed = new DataFeed();
        dataFeed.setDataFeedName(dataSourceDto.getDataFeedName());
        dataFeed.setDataSourceId(dataSourceDto.getDataSourceId());
        dataFeed.setEndpoint(dataSourceDto.getEndpoint());
        dataFeed.setParserId(dataSourceDto.getParserId());

        return dataFeed;
    }

    private DataSource createDataSource(DataSourceDto dataSourceDto) {
        DataSource dataSource = new DataSource();
        dataSource.setDataSourceId(dataSourceDto.getDataSourceId());
        dataSource.setDataSourceName(dataSourceDto.getDataSourceName());
        dataSource.setDescription(dataSourceDto.getDescription());
        dataSource.setTags(dataSourceDto.getTags());
        dataSource.setTimestamp(dataSourceDto.getTimestamp());
        dataSource.setType(dataSourceDto.getType());
        dataSource.setTypeConfig(dataSourceDto.getTypeConfig());

        return dataSource;
    }


    public List<DataSourceDto> getDataSourceDtos(Collection<DataSource> dataSources) throws Exception {
        List<DataSourceDto> dataSourceDtoList = new ArrayList<>();
        // todo we may want to add an API to fetch results in one invocation from dao/storage layer
        for (DataSource dataSource : dataSources) {
            dataSourceDtoList.add(new DataSourceDto(dataSource, getDataFeed(dataSource.getDataSourceId())));
        }

        return dataSourceDtoList;
    }

    public List<DataSourceDto> getAllDataSourceDtos() throws IOException {
        Collection<DataSource> dataSources = catalogService.listDataSources();
        Collection<DataFeed> dataFeeds = catalogService.listDataFeeds();
        Map<Long, DataFeed> feedMap = new HashMap<Long, DataFeed>();
        for (DataFeed dataFeed : dataFeeds) {
            feedMap.put(dataFeed.getDataSourceId(), dataFeed);
        }

        List<DataSourceDto> dataSourceDtoList = new ArrayList<>();
        for (DataSource dataSource : dataSources) {
            dataSourceDtoList.add(new DataSourceDto(dataSource, feedMap.get(dataSource.getDataSourceId())));
        }
        return dataSourceDtoList;
    }

    public DataFeed getDataFeed(Long dataSourceId) throws Exception {
        List<CatalogService.QueryParam> params = Collections.singletonList(new CatalogService.QueryParam(DataFeed.DATASOURCE_ID, dataSourceId.toString()));
        Collection<DataFeed> dataFeeds = catalogService.listDataFeeds(params);

        if (dataFeeds.size() == 0) {
            LOGGER.warn("Datafeed could not be found for datasource with dataSourceId: " + dataSourceId);
            return null;
        } else if (dataFeeds.size() > 1) {
            LOGGER.warn("Multiple datafeeds for datasource with id: " + dataSourceId);
        }

        return dataFeeds.iterator().next();
    }

    public DataSourceDto removeDataSource(Long dataSourceId) throws IOException {
        DataSource removedDataSource = catalogService.removeDataSource(dataSourceId);
        if (removedDataSource == null) {
            return null;
        }
        DataFeed removedDataFeed = catalogService.getDataFeed(dataSourceId);
        if (removedDataFeed != null) {
            catalogService.removeDataFeed(removedDataFeed.getDataFeedId());
        } else {
            LOGGER.warn("No datafeed found with dataSourceId: " + dataSourceId);
        }
        return new DataSourceDto(removedDataSource, removedDataFeed);
    }

    public Collection<DataSourceDto> listDataSourcesForType(DataSource.Type type, List<CatalogService.QueryParam> queryParams) throws Exception {
        Collection<DataSource> dataSources = catalogService.listDataSourcesForType(type, queryParams);
        if (dataSources.isEmpty()) {
            return null;
        }
        return getDataSourceDtos(dataSources);
    }

    public DataSourceDto getDataSource(Long dataSourceId) throws IOException {
        DataSource dataSource = catalogService.getDataSource(dataSourceId);
        if (dataSource == null) {
            return null;
        }
        // todo: can we have datasources without datafeeds?
        // currently returns with whatever info it retrieves.
        DataFeed dataFeed = catalogService.getDataFeed(dataSourceId);
        return new DataSourceDto(dataSource, dataFeed);
    }
}
