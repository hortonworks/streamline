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
import com.hortonworks.streamline.streams.cluster.service.metadata.HBaseMetadataService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.Tables;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;
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
public class HBaseMetadataResource {
    private static final Logger LOG = LoggerFactory.getLogger(HBaseMetadataResource.class);
    private final StreamlineAuthorizer authorizer;
    private final EnvironmentService environmentService;
    private Subject subject;

    public HBaseMetadataResource(StreamlineAuthorizer authorizer, EnvironmentService environmentService, Subject subject) {
        this.authorizer = authorizer;
        this.environmentService = environmentService;
        this.subject = subject;
    }

    @GET
    @Path("/clusters/{clusterId}/services/hbase/namespaces")
    @Timed
    public Response getNamespacesByClusterId(@PathParam("clusterId") Long clusterId,
                                             @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkPermissions(authorizer, securityContext, Cluster.NAMESPACE, clusterId, READ);
        try (HBaseMetadataService hbaseMetadataService = HBaseMetadataService
                .newInstance(environmentService, clusterId, securityContext, subject)) {
            return WSUtils.respondEntity(hbaseMetadataService.getHBaseNamespaces(), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }

    // ===

    @GET
    @Path("/clusters/{clusterId}/services/hbase/tables")
    @Timed
    public Response getTablesByClusterId(@PathParam("clusterId") Long clusterId,
                                         @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkPermissions(authorizer, securityContext, Cluster.NAMESPACE, clusterId, READ);
        try (HBaseMetadataService hbaseMetadataService = HBaseMetadataService
                .newInstance(environmentService, clusterId, securityContext, subject)) {
            return WSUtils.respondEntity(Subject.doAs(subject, (PrivilegedExceptionAction<Tables>) hbaseMetadataService::getHBaseTables), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }

    // ===

    @GET
    @Path("/clusters/{clusterId}/services/hbase/namespaces/{namespace}/tables")
    @Timed
    public Response getNamespaceTablesByClusterId(@PathParam("clusterId") Long clusterId, @PathParam("namespace") String namespace,
                                                  @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkPermissions(authorizer, securityContext, Cluster.NAMESPACE, clusterId, READ);
        try (HBaseMetadataService hbaseMetadataService = HBaseMetadataService
                .newInstance(environmentService, clusterId, securityContext, subject)) {
            return WSUtils.respondEntity(Subject.doAs(subject, (PrivilegedExceptionAction<Tables>)() -> hbaseMetadataService.getHBaseTables(namespace)), OK);
        } catch (EntityNotFoundException ex) {
            throw com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException.byId(ex.getMessage());
        }
    }
}
