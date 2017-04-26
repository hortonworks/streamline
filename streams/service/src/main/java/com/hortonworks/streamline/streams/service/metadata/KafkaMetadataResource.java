/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.service.metadata;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.exception.EntityNotFoundException;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.KafkaMetadataService;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import static com.hortonworks.streamline.streams.security.Permission.READ;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class KafkaMetadataResource {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMetadataResource.class);
    private final StreamlineAuthorizer authorizer;
    private final EnvironmentService environmentService;

    public KafkaMetadataResource(StreamlineAuthorizer authorizer, EnvironmentService environmentService) {
        this.authorizer = authorizer;
        this.environmentService = environmentService;
    }

    @GET
    @Path("/clusters/{clusterId}/services/kafka/brokers")
    @Timed
    public Response getBrokersByClusterId(@PathParam("clusterId") Long clusterId,
                                          @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkPermissions(authorizer, securityContext, Cluster.NAMESPACE, clusterId, READ);
        try(final KafkaMetadataService kafkaMetadataService = KafkaMetadataService
                .newInstance(environmentService, clusterId, securityContext)) {
            return WSUtils.respondEntity(kafkaMetadataService.getBrokerHostPortFromStreamsJson(clusterId), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }

    @GET
    @Path("/clusters/{clusterId}/services/kafka/topics")
    @Timed
    public Response getTopicsByClusterId(@PathParam("clusterId") Long clusterId,
                                         @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkPermissions(authorizer, securityContext, Cluster.NAMESPACE, clusterId, READ);
        try(final KafkaMetadataService kafkaMetadataService = KafkaMetadataService
                .newInstance(environmentService, clusterId, securityContext)) {
            return WSUtils.respondEntity(kafkaMetadataService.getTopicsFromZk(), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }
}
