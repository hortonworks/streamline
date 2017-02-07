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
package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.catalog.TopologyEditorMetadata;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyEditorMetadataResource {
    private final StreamCatalogService catalogService;

    public TopologyEditorMetadataResource(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GET
    @Path("/system/topologyeditormetadata")
    @Timed
    public Response listTopologyEditorMetadata () {
        Collection<TopologyEditorMetadata> result = catalogService.listTopologyEditorMetadata();
        if (result != null) {
            return WSUtils.respondEntities(result, OK);
        }

        throw EntityNotFoundException.byFilter("");
    }

    @GET
    @Path("/system/topologyeditormetadata/{id}")
    @Timed
    public Response getTopologyEditorMetadataByTopologyId (@PathParam("id") Long topologyId) {
        TopologyEditorMetadata result = catalogService.getTopologyEditorMetadata(topologyId);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/system/versions/{versionId}/topologyeditormetadata/{id}/")
    @Timed
    public Response getTopologyEditorMetadataByTopologyIdAndVersionId(@PathParam("versionId") Long versionId,
                                                                      @PathParam("id") Long topologyId) {
        TopologyEditorMetadata result = catalogService.getTopologyEditorMetadata(topologyId, versionId);
        if (result != null) {
            return WSUtils.respondEntity(result, OK);
        }

        throw EntityNotFoundException.byVersion(topologyId.toString(), versionId.toString());
    }

    @POST
    @Path("/system/topologyeditormetadata")
    @Timed
    public Response addTopologyEditorMetadata (TopologyEditorMetadata topologyEditorMetadata) {
        TopologyEditorMetadata addedTopologyEditorMetadata = catalogService.addTopologyEditorMetadata(
                topologyEditorMetadata.getTopologyId(), topologyEditorMetadata);
        return WSUtils.respondEntity(addedTopologyEditorMetadata, CREATED);
    }

    @DELETE
    @Path("/system/topologyeditormetadata/{id}")
    @Timed
    public Response removeTopologyEditorMetadata (@PathParam("id") Long topologyId) {
        TopologyEditorMetadata removedTopologyEditorMetadata = catalogService.removeTopologyEditorMetadata(topologyId);
        if (removedTopologyEditorMetadata != null) {
            return WSUtils.respondEntity(removedTopologyEditorMetadata, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @PUT
    @Path("/system/topologyeditormetadata/{id}")
    @Timed
    public Response addOrUpdateTopologyEditorMetadata (@PathParam("id") Long topologyId, TopologyEditorMetadata topologyEditorMetadata) {
        TopologyEditorMetadata newTopologyEditorMetadata = catalogService.addOrUpdateTopologyEditorMetadata(topologyId, topologyEditorMetadata);
        return WSUtils.respondEntity(topologyEditorMetadata, OK);
    }
}