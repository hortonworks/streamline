package com.hortonworks.iotas.streams.catalog;

import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.registries.parser.ParserInfo;
import com.hortonworks.iotas.registries.tag.Tag;
import com.hortonworks.iotas.registries.tag.client.TagClient;
import com.hortonworks.iotas.streams.catalog.service.CatalogService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSourceFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFacade.class);

    private final CatalogService catalogService;
    private final TagClient tagClient;


    public DataSourceFacade(CatalogService catalogService, TagClient tagClient) {
        this.catalogService = catalogService;
        this.tagClient = tagClient;

    }

    public DataSourceDto addOrUpdateDataSourceWithDataFeed(Long dataSourceId, DataSourceDto dataSourceDto) throws Exception {
        createTagsForDataSourceDto(dataSourceDto);
        DataSource dataSource = createDataSource(dataSourceDto);
        DataSource createdDataSource = catalogService.addOrUpdateDataSource(dataSourceId, dataSource);
        dataSourceDto.setDataSourceId(createdDataSource.getId());

        DataFeed dataFeed = createDataFeed(dataSourceDto);
        DataFeed existingDataFeed = getDataFeedByDataSourceId(dataSourceId);
        DataFeed createdDataFeed = existingDataFeed != null
                ? catalogService.addOrUpdateDataFeed(existingDataFeed.getId(), dataFeed)
                : catalogService.addDataFeed(dataFeed);

        return createDataSourceDto(createdDataSource, createdDataFeed);
    }

    public DataSourceDto createDataSourceWithDataFeed(DataSourceDto dataSourceDto) throws Exception {
        createTagsForDataSourceDto(dataSourceDto);
        DataSource dataSource = createDataSource(dataSourceDto);
        DataSource createdDataSource = catalogService.addDataSource(dataSource);
        dataSourceDto.setDataSourceId(createdDataSource.getId());

        DataFeed dataFeed = createDataFeed(dataSourceDto);
        DataFeed createdDataFeed = null;
        if (dataFeed.getName() != null && dataFeed.getType() != null) {
            createdDataFeed = catalogService.addDataFeed(dataFeed);
        } else {
            LOGGER.warn("Not creating datafeed. Name and type must be present");
        }
        return createDataSourceDto(createdDataSource, createdDataFeed);
    }

    private DataSourceDto createDataSourceDto(DataSource dataSource, DataFeed dataFeed) {
        DataSourceDto createdDataSourceDto = new DataSourceDto(dataSource, dataFeed);
        if (dataFeed != null) {
            ParserInfo parserInfo = catalogService.getParserInfo(dataFeed.getParserId());
            if (parserInfo != null) {
                createdDataSourceDto.setParserName(parserInfo.getName());
            }
        }
        return createdDataSourceDto;
    }

    private DataFeed createDataFeed(DataSourceDto dataSourceDto) {
        DataFeed dataFeed = new DataFeed();
        dataFeed.setName(dataSourceDto.getDataFeedName());
        dataFeed.setDataSourceId(dataSourceDto.getDataSourceId());
        dataFeed.setType(dataSourceDto.getDataFeedType());
        dataFeed.setParserId(dataSourceDto.getParserId());

        return dataFeed;
    }

    private DataSource createDataSource(DataSourceDto dataSourceDto) {
        DataSource dataSource = new DataSource();
        dataSource.setId(dataSourceDto.getDataSourceId());
        dataSource.setName(dataSourceDto.getDataSourceName());
        dataSource.setDescription(dataSourceDto.getDescription());
        List<Tag> tags = getTagsForDataSourceDto(dataSourceDto);
        dataSource.setTags(tags);
        dataSource.setTimestamp(dataSourceDto.getTimestamp());
        dataSource.setType(dataSourceDto.getType());
        dataSource.setTypeConfig(dataSourceDto.getTypeConfig());
        return dataSource;
    }


    public List<DataSourceDto> getDataSourceDtos(Collection<DataSource> dataSources) throws Exception {
        List<DataSourceDto> dataSourceDtoList = new ArrayList<>();
        // todo we may want to add an API to fetch results in one invocation from dao/storage layer
        for (DataSource dataSource : dataSources) {
            dataSourceDtoList.add(createDataSourceDto(dataSource, getDataFeedByDataSourceId(dataSource.getId())));
        }

        return dataSourceDtoList;
    }

    public List<DataSourceDto> getAllDataSourceDtos() throws IOException, InstantiationException, IllegalAccessException {
        Collection<DataSource> dataSources = catalogService.listDataSources();
        Collection<DataFeed> dataFeeds = catalogService.listDataFeeds();
        Map<Long, DataFeed> feedMap = new HashMap<>();
        for (DataFeed dataFeed : dataFeeds) {
            feedMap.put(dataFeed.getDataSourceId(), dataFeed);
        }

        List<DataSourceDto> dataSourceDtoList = new ArrayList<>();
        for (DataSource dataSource : dataSources) {
            DataSourceDto dataSourceDto = createDataSourceDto(dataSource, feedMap.get(dataSource.getId()));
            dataSourceDtoList.add(dataSourceDto);
        }

        return dataSourceDtoList;
    }

    public DataFeed getDataFeedByDataSourceId(Long dataSourceId) throws Exception {
        List<QueryParam> params = Collections.singletonList(new QueryParam(DataFeed.DATASOURCE_ID, dataSourceId.toString()));
        Collection<DataFeed> dataFeeds = catalogService.listDataFeeds(params);

        if (dataFeeds.size() == 0) {
            LOGGER.warn("Datafeed could not be found for datasource with dataSourceId: " + dataSourceId);
            return null;
        } else if (dataFeeds.size() > 1) {
            LOGGER.warn("Multiple datafeeds for datasource with id: " + dataSourceId);
        }

        return dataFeeds.iterator().next();
    }

    public DataSourceDto removeDataSource(Long dataSourceId) throws Exception {
        DataSource removedDataSource = catalogService.removeDataSource(dataSourceId);
        if (removedDataSource == null) {
            return null;
        }
        DataFeed removedDataFeed = catalogService.getDataFeed(dataSourceId);
        if (removedDataFeed != null) {
            catalogService.removeDataFeed(removedDataFeed.getId());
        } else {
            LOGGER.warn("No datafeed found with dataSourceId: " + dataSourceId);
        }
        return new DataSourceDto(removedDataSource, removedDataFeed);
    }

    public Collection<DataSourceDto> listDataSourcesForType(DataSource.Type type, List<QueryParam> queryParams) throws Exception {
        Collection<DataSource> dataSources = catalogService.listDataSourcesForType(type, queryParams);
        if (dataSources.isEmpty()) {
            return null;
        }
        return getDataSourceDtos(dataSources);
    }

    public DataSourceDto getDataSource(Long dataSourceId) throws Exception {
        DataSource dataSource = catalogService.getDataSource(dataSourceId);
        if (dataSource == null) {
            return null;
        }
        // todo: can we have datasources without datafeeds?
        // currently returns with whatever info it retrieves.
        DataFeed dataFeed = getDataFeedByDataSourceId(dataSourceId);
        return createDataSourceDto(dataSource, dataFeed);
    }

    private void createTagsForDataSourceDto (DataSourceDto dataSourceDto) {
        if (dataSourceDto.getTags() != null) {
            String[] tags = dataSourceDto.getTags().split(",");
            for (String tagString: tags) {
                Collection<Tag> existingTags = tagClient.listTags();
                boolean found = false;
                for (Tag tag: existingTags) {
                    if (tag.getName().equals(tagString)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Tag newTag = new Tag();
                    newTag.setName(tagString);
                    newTag.setDescription(tagString);
                    newTag.setTags(new ArrayList<Tag>());
                    tagClient.addTag(newTag);
                }
            }
        }
    }

    private List<Tag> getTagsForDataSourceDto (DataSourceDto dataSourceDto) {
        List<Tag> result = new ArrayList<>();
        if (!StringUtils.isEmpty(dataSourceDto.getTags())) {
            Collection<Tag> existingTags = tagClient.listTags();
            for (String tagString: dataSourceDto.getTags().split(",")) {
                for (Tag tag: existingTags) {
                    if (tag.getName().equals(tagString)) {
                        result.add(tag);
                        break;
                    }
                }
            }
        }
        return result;
    }
}
