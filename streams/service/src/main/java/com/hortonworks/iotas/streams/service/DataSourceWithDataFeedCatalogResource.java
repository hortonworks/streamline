package com.hortonworks.iotas.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.util.WSUtils;
import com.hortonworks.iotas.streams.catalog.DataSource;
import com.hortonworks.iotas.streams.catalog.DataSourceDto;
import com.hortonworks.iotas.streams.catalog.DataSourceFacade;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.*;

/**
 * REST API endpoint for adding datasource with datafeed.
 */
@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class DataSourceWithDataFeedCatalogResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceWithDataFeedCatalogResource.class);
    private final DataSourceFacade dataSourceFacade;

    public DataSourceWithDataFeedCatalogResource(DataSourceFacade dataSourceFacade) {
        this.dataSourceFacade = dataSourceFacade;
    }

    @POST
    @Path("/datasources")
    @Timed
    public Response addDataSourceWithDataFeed(DataSourceDto dataSourceDto) {
        try {
            if (StringUtils.isEmpty(dataSourceDto.getTypeConfig())) {
                return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "typeConfig");
            }
            DataSourceDto createdDataSourceDto = dataSourceFacade.createDataSourceWithDataFeed(dataSourceDto);
            return WSUtils.respond(CREATED, SUCCESS, createdDataSourceDto);
        } catch (Exception ex) {
            LOGGER.error("Error encountered while adding datasource with datafeed", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    @PUT
    @Path("/datasources/{id}")
    @Timed
    public Response addOrUpdateDataSourceWithDataFeed(@PathParam("id") Long dataSourceId, DataSourceDto dataSourceDto) {
        try {
            if (StringUtils.isEmpty(dataSourceDto.getTypeConfig())) {
                return WSUtils.respond(BAD_REQUEST, BAD_REQUEST_PARAM_MISSING, "typeConfig");
            }
            DataSourceDto createdDataSourceDto = dataSourceFacade.addOrUpdateDataSourceWithDataFeed(dataSourceId, dataSourceDto);
            return WSUtils.respond(CREATED, SUCCESS, createdDataSourceDto);
        } catch (Exception ex) {
            LOGGER.error("Error encountered while adding datasource with datafeed", ex);
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }


    @GET
    @Path("/datasources")
    @Timed
    public Response listDataSourcesWithDataFeed(@javax.ws.rs.QueryParam("filter") List<String> filter) {
        try {
            List<DataSourceDto> dataSourceDtoList = dataSourceFacade.getAllDataSourceDtos();

            return WSUtils.respond(OK, SUCCESS, dataSourceDtoList);
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

    /**
     * List datasource matching the type and the type specific fields and values.
     */
    @GET
    @Path("/datasources/type/{type}")
    @Timed
    public Response listDataSourcesWithDataFeedForTypeWithFilter(@PathParam("type") DataSource.Type type,
                                                                 @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<QueryParam>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            queryParams = WSUtils.buildQueryParameters(params);

            Collection<DataSourceDto> dataSources = dataSourceFacade.listDataSourcesForType(type, queryParams);
            if (!dataSources.isEmpty()) {
                return WSUtils.respond(OK, SUCCESS, dataSources);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
        return WSUtils.respond(NOT_FOUND, DATASOURCE_TYPE_FILTER_NOT_FOUND, type.toString(), queryParams.toString());
    }

    @GET
    @Path("/datasources/{id}")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDataSourceWithDataFeedById(@PathParam("id") Long dataSourceId) {
        try {
            DataSourceDto dataSource = dataSourceFacade.getDataSource(dataSourceId);
            if (dataSource != null) {
                return WSUtils.respond(OK, SUCCESS, dataSource);
            }
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }

        return WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataSourceId.toString());
    }

    @DELETE
    @Path("/datasources/{id}")
    @Timed
    public Response removeDataSourceWithDataFeed(@PathParam("id") Long dataSourceId) {
        try {
            DataSourceDto removedDataSource = dataSourceFacade.removeDataSource(dataSourceId);
            return removedDataSource != null
                    ? WSUtils.respond(OK, SUCCESS, removedDataSource)
                    : WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataSourceId.toString());
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

}
