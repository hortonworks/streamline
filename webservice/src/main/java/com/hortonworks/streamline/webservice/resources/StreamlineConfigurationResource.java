/**
 * Copyright 2017 Hortonworks.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.hortonworks.streamline.webservice.resources;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.webservice.configurations.StreamlineConfiguration;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static javax.ws.rs.core.Response.Status.OK;


@Path("/v1/config")
@Produces(MediaType.APPLICATION_JSON)

public class StreamlineConfigurationResource {

    private StreamlineConfiguration streamlineConfiguration;

    public StreamlineConfigurationResource(StreamlineConfiguration streamlineConfiguration) {
        this.streamlineConfiguration = streamlineConfiguration;
    }

    /**
     * List ALL streamline configuration.
     */
    @GET
    @Path("/streamline")
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStreamlineConfiguraiton(@Context UriInfo uriInfo) {

        // StreamlineConfiguration object here is serialized with StreamlineConfigurationSerializer
        return WSUtils.respondEntity(this.streamlineConfiguration, OK);
    }

}
