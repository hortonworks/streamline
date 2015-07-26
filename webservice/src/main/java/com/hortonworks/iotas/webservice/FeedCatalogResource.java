package com.hortonworks.iotas.webservice;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.storage.StorageManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/api/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class FeedCatalogResource {
    private StorageManager dao;
    // TODO should probably make namespace static
    private static final String DATA_FEED_NAMESPACE = new DataFeed().getNameSpace();

    public FeedCatalogResource(StorageManager manager) {
        this.dao = manager;
    }

    @GET
    @Path("/feeds")
    @Timed
    // TODO add a way to query/filter and/or page results
    public Collection<DataFeed> listDataFeeds() {
        return this.dao.<DataFeed>list(DATA_FEED_NAMESPACE);
    }

    @GET
    @Path("/feeds/{id}")
    @Timed
    public DataFeed getDataFeedById(@PathParam("id") Long dataFeedId) {
        DataFeed df = new DataFeed();
        df.setDatafeedId(dataFeedId);
        return this.dao.<DataFeed>get(DATA_FEED_NAMESPACE, df.getPrimaryKey());
    }

    @POST
    @Path("/feeds")
    @Timed
    public DataFeed addDataFeed(DataFeed feed) {
        if (feed.getDatafeedId() == null) {
            feed.setDatafeedId(this.dao.nextId(DATA_FEED_NAMESPACE));
        }
        if (feed.getTimestamp() == null) {
            feed.setTimestamp(System.currentTimeMillis());
        }
        this.dao.add(feed);
        return feed;
    }

    @DELETE
    @Path("/feeds/{id}")
    @Timed
    public DataFeed removeDatafeed(@PathParam("id") Long dataFeedId) {
        DataFeed feed = new DataFeed();
        feed.setDatafeedId(dataFeedId);
        return this.dao.remove(DATA_FEED_NAMESPACE, feed.getPrimaryKey());
    }

    @PUT
    @Path("/feeds")
    @Timed
    public DataFeed addOrUpdateDataFeed(DataFeed feed) {
        if (feed.getDatafeedId() == null) {
            feed.setDatafeedId(this.dao.nextId(DATA_FEED_NAMESPACE));
        }
        if (feed.getTimestamp() == null) {
            feed.setTimestamp(System.currentTimeMillis());
        }
        this.dao.addOrUpdate(feed);
        return feed;
    }
}
