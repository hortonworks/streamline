package com.hortonworks.iotas.webservice.catalog;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.catalog.dto.DataSourceDto;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * REST API endpoint for adding datasource with datafeed.
 *
 */
@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class DataSourceWithDataFeedCatalogResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceWithDataFeedCatalogResource.class);
    private final CatalogService catalogService;

    public DataSourceWithDataFeedCatalogResource(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // todo: better path for this resource
    @POST
    @Path("/datasourceswithfeed")
    @Timed
    public Response addDataSourceWithDataFeed(DataSourceDto dataSourceDto) {
        try {
            if (StringUtils.isEmpty(dataSourceDto.getTypeConfig())) {
                return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "typeConfig");
            }
            DataSourceDto createdDataSourceDto = createDataSourceWithDataFeed(dataSourceDto);
            return WSUtils.respond(CREATED, SUCCESS, createdDataSourceDto);
        } catch (Exception ex) {
            LOGGER.error("Error encountered while adding datasource with datafeed", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private DataSourceDto createDataSourceWithDataFeed(DataSourceDto dataSourceDto) throws IOException {
        DataSource dataSource = createDataSource(dataSourceDto);
        DataSource createdDataSource = catalogService.addDataSource(dataSource);
        dataSourceDto.setDataSourceId(createdDataSource.getDataSourceId());

        DataFeed dataFeed = createDataFeed(dataSourceDto);
        DataFeed createdDataFeed = catalogService.addDataFeed(dataFeed);

        return new DataSourceDto(createdDataSource, createdDataFeed);
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

}
