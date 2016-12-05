package org.apache.streamline.streams.service.metadata;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;

import org.apache.streamline.streams.catalog.exception.EntityNotFoundException;
import org.apache.streamline.streams.catalog.service.metadata.KafkaMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class KafkaMetadataResource {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMetadataResource.class);
    private final StreamCatalogService catalogService;

    public KafkaMetadataResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/clusters/name/{clusterName}/services/kafka/brokers")
    @Timed
    public Response getBrokersByClusterName(@PathParam("clusterName") String clusterName)
        throws Exception {
        final Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            throw org.apache.streamline.common.exception.service.exception.request.EntityNotFoundException.byName("cluster name " + clusterName);
        }
        return getBrokersByClusterId(cluster.getId());
    }

    @GET
    @Path("/clusters/{clusterId}/services/kafka/brokers")
    @Timed
    public Response getBrokersByClusterId(@PathParam("clusterId") Long clusterId) throws Exception {
        try(final KafkaMetadataService kafkaMetadataService = KafkaMetadataService.newInstance(catalogService, clusterId)) {
            return WSUtils.respondEntity(kafkaMetadataService.getBrokerHostPortFromStreamsJson(clusterId), OK);
        } catch (EntityNotFoundException ex) {
            throw org.apache.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }

    @GET
    @Path("/clusters/name/{clusterName}/services/kafka/topics")
    @Timed
    public Response getTopicsByClusterName(@PathParam("clusterName") String clusterName)
        throws Exception {
        final Cluster cluster = catalogService.getClusterByName(clusterName);
        if (cluster == null) {
            throw org.apache.streamline.common.exception.service.exception.request.EntityNotFoundException.byName("cluster name " + clusterName);
        }
        return getTopicsByClusterId(cluster.getId());
    }

    @GET
    @Path("/clusters/{clusterId}/services/kafka/topics")
    @Timed
    public Response getTopicsByClusterId(@PathParam("clusterId") Long clusterId) throws Exception {
        try(final KafkaMetadataService kafkaMetadataService = KafkaMetadataService.newInstance(catalogService, clusterId)) {
            return WSUtils.respondEntity(kafkaMetadataService.getTopicsFromZk(), OK);
        } catch (EntityNotFoundException ex) {
            throw org.apache.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }
}
