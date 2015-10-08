package com.hortonworks.iotas.webservice;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.webservice.pojo.DataSourceInfo;
import com.hortonworks.iotas.webservice.util.WSUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
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
    public Response addDataSourceWithDataFeed(DataSourceInfo dataSourceInfo) {
        try {
            if (StringUtils.isEmpty(dataSourceInfo.getTypeConfig())) {
                return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "typeConfig");
            }
            DataSource dataSource = createDataSource(dataSourceInfo);
            DataSource createdDataSource = catalogService.addDataSource(dataSource);
            dataSourceInfo.setDataSourceId(createdDataSource.getDataSourceId());

            DataFeed dataFeed = createDataFeed(dataSourceInfo);
            DataFeed createdDataFeed = catalogService.addDataFeed(dataFeed);

            return WSUtils.respond(CREATED, SUCCESS, new DataSourceInfo(createdDataSource, createdDataFeed));
        } catch (Exception ex) {
            LOGGER.error("Error encountered while adding datasource with datafeed", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    private DataFeed createDataFeed(DataSourceInfo dataSourceInfo) {
        DataFeed dataFeed = new DataFeed();
        dataFeed.setDataFeedName(dataSourceInfo.getDataFeedName());
        dataFeed.setDataSourceId(dataSourceInfo.getDataSourceId());
        dataFeed.setEndpoint(dataSourceInfo.getEndpoint());
        dataFeed.setParserId(dataSourceInfo.getParserId());

        return dataFeed;
    }

    private DataSource createDataSource(DataSourceInfo dataSourceInfo) {
        DataSource dataSource = new DataSource();
        dataSource.setDataSourceId(dataSourceInfo.getDataSourceId());
        dataSource.setDataSourceName(dataSourceInfo.getDataSourceName());
        dataSource.setDescription(dataSourceInfo.getDescription());
        dataSource.setTags(dataSourceInfo.getTags());
        dataSource.setTimestamp(dataSourceInfo.getTimestamp());
        dataSource.setType(dataSourceInfo.getType());
        dataSource.setTypeConfig(dataSourceInfo.getTypeConfig());

        return dataSource;
    }

}
