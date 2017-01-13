package com.hortonworks.streamline.streams.service.metadata;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.service.EnvironmentService;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;

import com.hortonworks.streamline.streams.catalog.exception.EntityNotFoundException;
import com.hortonworks.streamline.streams.catalog.service.metadata.KafkaMetadataService;
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
    private final EnvironmentService environmentService;

    public KafkaMetadataResource(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @GET
    @Path("/clusters/{clusterId}/services/kafka/brokers")
    @Timed
    public Response getBrokersByClusterId(@PathParam("clusterId") Long clusterId) throws Exception {
        try(final KafkaMetadataService kafkaMetadataService = KafkaMetadataService.newInstance(environmentService, clusterId)) {
            return WSUtils.respondEntity(kafkaMetadataService.getBrokerHostPortFromStreamsJson(clusterId), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }

    @GET
    @Path("/clusters/{clusterId}/services/kafka/topics")
    @Timed
    public Response getTopicsByClusterId(@PathParam("clusterId") Long clusterId) throws Exception {
        try(final KafkaMetadataService kafkaMetadataService = KafkaMetadataService.newInstance(environmentService, clusterId)) {
            return WSUtils.respondEntity(kafkaMetadataService.getTopicsFromZk(), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }
}
