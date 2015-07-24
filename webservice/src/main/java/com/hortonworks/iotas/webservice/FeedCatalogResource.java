package com.hortonworks.iotas.webservice;

import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.storage.StorageManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class FeedCatalogResource {
    private StorageManager dao;
    private static final String DATA_FEED_NAMESPACE = new DataFeed().getNameSpace();

    public FeedCatalogResource(StorageManager manager){
        this.dao = manager;
    }

    @GET
    @Path("/feeds")
    // TODO add a way to query/filter and/or page results
    public Collection<DataFeed> listDataFeeds(){
        // TODO should probably make namespace static
        return this.dao.<DataFeed>list(DATA_FEED_NAMESPACE);
    }

    @GET
    @Path("/feeds/{id}")
    public DataFeed getDataFeedById(@PathParam("id") Long dataFeedId){
        DataFeed df = new DataFeed();
        df.setDatafeedId(dataFeedId);
        return this.dao.<DataFeed>get(DATA_FEED_NAMESPACE, df.getPrimaryKey());
    }

    @POST
    @Path("/feeds")
    public DataFeed createDataFeed(DataFeed feed){
        feed.setDatafeedId(this.dao.nextId(DATA_FEED_NAMESPACE));
        if(feed.getTimestamp() == null){
            feed.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(feed);
        return feed;
    }

    @PUT
    @Path("/feeds")
    public DataFeed addOrUpdateDataFeed(DataFeed feed){
        if(feed.getDatafeedId() == null) {
            feed.setDatafeedId(this.dao.nextId(DATA_FEED_NAMESPACE));
        }
        if(feed.getTimestamp() == null){
            feed.setTimestamp(System.currentTimeMillis());
        }
        this.dao.addOrUpdate(feed);
        return feed;
    }


}
