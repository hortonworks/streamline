/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.webservice.util;

import com.google.common.io.ByteStreams;
import com.hortonworks.iotas.catalog.CatalogResponse;
import com.hortonworks.iotas.service.CatalogService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Utility methods for the webservice.
 */
public class WSUtils {
    private WSUtils() {
    }

    public static Response respond(Response.Status status, CatalogResponse.ResponseMessage msg, Collection<? extends Object> entities, String... formatArgs) {
        return Response.status(status)
                .entity(CatalogResponse.newResponse(msg).entities(entities).format(formatArgs))
                .build();
    }

    public static Response respond(Response.Status status, CatalogResponse.ResponseMessage msg, Object entity, String... formatArgs) {
        return Response.status(status)
                .entity(CatalogResponse.newResponse(msg).entity(entity).format(formatArgs))
                .build();
    }

    public static Response respond(Response.Status status, CatalogResponse.ResponseMessage msg, String... formatArgs) {
        return Response.status(status)
                .entity(CatalogResponse.newResponse(msg).entity(null).format(formatArgs))
                .build();
    }

    public static List<CatalogService.QueryParam> buildQueryParameters(MultivaluedMap<String, String> params) {
        if (params == null || params.isEmpty()) {
            return Collections.<CatalogService.QueryParam>emptyList();
        }

        List<CatalogService.QueryParam> queryParams = new ArrayList<>();
        for (String param : params.keySet()) {
            queryParams.add(new CatalogService.QueryParam(param, params.getFirst(param)));
        }
        return queryParams;
    }

    public static StreamingOutput wrapWithStreamingOutput(final InputStream inputStream) {
        return new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                try {
                    ByteStreams.copy(inputStream, os);
                } finally {
                    os.close();
                }
            }
        };
    }


}
