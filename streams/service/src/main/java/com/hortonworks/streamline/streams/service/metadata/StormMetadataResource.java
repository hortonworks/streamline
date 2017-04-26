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
import com.hortonworks.streamline.streams.cluster.service.metadata.StormMetadataService;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;

import java.util.Collections;

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
public class StormMetadataResource {
    private final StreamlineAuthorizer authorizer;
    private final EnvironmentService environmentService;

    public StormMetadataResource(StreamlineAuthorizer authorizer, EnvironmentService environmentService) {
        this.authorizer = authorizer;
        this.environmentService = environmentService;
    }

    @GET
    @Path("/clusters/{clusterId}/services/storm/topologies")
    @Timed
    public Response getTopologiesByClusterId(@PathParam("clusterId") Long clusterId,
                                             @Context SecurityContext securityContext) {
        SecurityUtil.checkPermissions(authorizer, securityContext, Cluster.NAMESPACE, clusterId, READ);
        try {
            StormMetadataService stormMetadataService = new StormMetadataService
                    .Builder(environmentService, clusterId, securityContext).build();
            return WSUtils.respondEntity(stormMetadataService.getTopologies(), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }

    @GET
    @Path("/clusters/{clusterId}/services/storm/mainpage/url")
    @Timed
    public Response getMainPageByClusterId(@PathParam("clusterId") Long clusterId,
                                           @Context SecurityContext securityContext) {
        SecurityUtil.checkPermissions(authorizer, securityContext, Cluster.NAMESPACE, clusterId, READ);
        try {
            StormMetadataService stormMetadataService = new StormMetadataService
                    .Builder(environmentService, clusterId, securityContext).build();
            return WSUtils.respondEntity(Collections.singletonMap("url", stormMetadataService.getMainPageUrl()), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }
}
