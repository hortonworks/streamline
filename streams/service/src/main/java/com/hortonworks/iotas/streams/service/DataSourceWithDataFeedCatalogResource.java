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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.BAD_REQUEST_PARAM_MISSING;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.DATASOURCE_TYPE_FILTER_NOT_FOUND;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.ENTITY_NOT_FOUND;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.EXCEPTION;
import static com.hortonworks.iotas.common.catalog.CatalogResponse.ResponseMessage.SUCCESS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

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
            return WSUtils.respond(createdDataSourceDto, CREATED, SUCCESS);
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
            return WSUtils.respond(createdDataSourceDto, CREATED, SUCCESS);
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

            return WSUtils.respond(dataSourceDtoList, OK, SUCCESS);
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
        List<QueryParam> queryParams = new ArrayList<>();
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            queryParams = WSUtils.buildQueryParameters(params);

            Collection<DataSourceDto> dataSources = dataSourceFacade.listDataSourcesForType(type, queryParams);
            if (!dataSources.isEmpty()) {
                return WSUtils.respond(dataSources, OK, SUCCESS);
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
                return WSUtils.respond(dataSource, OK, SUCCESS);
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
                    ? WSUtils.respond(removedDataSource, OK, SUCCESS)
                    : WSUtils.respond(NOT_FOUND, ENTITY_NOT_FOUND, dataSourceId.toString());
        } catch (Exception ex) {
            return WSUtils.respond(INTERNAL_SERVER_ERROR, EXCEPTION, ex.getMessage());
        }
    }

}
