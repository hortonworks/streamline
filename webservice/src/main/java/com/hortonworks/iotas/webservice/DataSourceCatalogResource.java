package com.hortonworks.iotas.webservice;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.storage.StorageManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

//TODO Path should be all lower case.

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class DataSourceCatalogResource {
    private StorageManager dao;
    // TODO should probably make namespace static
    private static final String DATA_SOURCE_NAMESPACE = new DataSource().getNameSpace();

    public DataSourceCatalogResource(StorageManager manager) {
        this.dao = manager;
    }

    @GET
    @Path("/dataSources")
    @Timed
    // TODO add a way to query/filter and/or page results
    public Collection<DataSource> listDataSources() {
        return this.dao.<DataSource>list(DATA_SOURCE_NAMESPACE);
    }

    @GET
    @Path("/dataSources/{id}")
    @Timed
    public DataSource getDataSourceById(@PathParam("id") Long dataSourceId) {
        DataSource ds = new DataSource();
        ds.setDataSourceId(dataSourceId);
        return this.dao.<DataSource>get(DATA_SOURCE_NAMESPACE, ds.getPrimaryKey());
    }

    @POST
    @Path("/dataSources")
    @Timed
    public DataSource addDataSource(DataSource dataSource) {
        if (dataSource.getDataSourceId() == null) {
            dataSource.setDataSourceId(this.dao.nextId(DATA_SOURCE_NAMESPACE));
        }
        if (dataSource.getTimestamp() == null) {
            dataSource.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(dataSource);
        return dataSource;
    }

    @DELETE
    @Path("/dataSources/{id}")
    @Timed
    public DataSource removeParser(@PathParam("id") Long dataSourceId) {
        DataSource dataSource = new DataSource();
        dataSource.setDataSourceId(dataSourceId);
        return this.dao.remove(DATA_SOURCE_NAMESPACE, dataSource.getPrimaryKey());
    }

    @PUT
    @Path("/dataSources")
    @Timed
    public DataSource addOrUpdateDataSource(DataSource dataSource) {
        if (dataSource.getDataSourceId() == null) {
            dataSource.setDataSourceId(this.dao.nextId(DATA_SOURCE_NAMESPACE));
        }
        if (dataSource.getTimestamp() == null) {
            dataSource.setTimestamp(System.currentTimeMillis());
        }
        this.dao.addOrUpdate(dataSource);
        return dataSource;
    }
}
