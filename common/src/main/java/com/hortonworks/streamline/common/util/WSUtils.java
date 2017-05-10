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

package com.hortonworks.streamline.common.util;

import com.google.common.io.ByteStreams;
import com.hortonworks.streamline.common.CollectionResponse;
import com.hortonworks.streamline.common.QueryParam;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedOutputStream;
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
    public static final String CURRENT_VERSION = "CURRENT";
    public static final String TOPOLOGY_ID = "topologyId";
    public static final String VERSION_ID = "versionId";
    public static final String NAME = "name";
    public static final String FROM_ID = "fromId";
    public static final String TO_ID = "toId";

    private WSUtils() {
    }

    public static Response respondEntities(Collection<?> entities, Response.Status status) {
        return Response.status(status)
            .entity(CollectionResponse.newResponse().entities(entities).build())
            .build();
    }

    public static Response respondEntity(Object entity, Response.Status status) {
        return Response.status(status)
                .entity(entity)
                .build();
    }

    public static List<QueryParam> buildEdgesFromQueryParam(Long topologyId,
                                                            Long versionId,
                                                            Long componentId) {
        return QueryParam.params(
                TOPOLOGY_ID, topologyId.toString(),
                VERSION_ID, versionId.toString(),
                FROM_ID, componentId.toString()
        );
    }

    public static List<QueryParam> buildEdgesToQueryParam(Long topologyId,
                                                            Long versionId,
                                                            Long componentId) {
        return QueryParam.params(
                TOPOLOGY_ID, topologyId.toString(),
                VERSION_ID, versionId.toString(),
                TO_ID, componentId.toString()
        );
    }

    public static List<QueryParam> buildQueryParameters(MultivaluedMap<String, String> params) {
        if (params == null || params.isEmpty()) {
            return Collections.emptyList();
        }

        List<QueryParam> queryParams = new ArrayList<>();
        for (String param : params.keySet()) {
            queryParams.add(new QueryParam(param, params.getFirst(param)));
        }
        return queryParams;
    }


    public static List<QueryParam> buildTopologyIdAwareQueryParams(Long topologyId, UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(TOPOLOGY_ID, topologyId.toString()));
        addQueryParams(uriInfo, queryParams);
        return queryParams;
    }

    public static List<QueryParam> buildTopologyIdAndVersionIdAwareQueryParams(Long topologyId,
                                                                             Long versionId,
                                                                             UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(TOPOLOGY_ID, topologyId.toString()));
        queryParams.addAll(versionIdQueryParam(versionId));
        addQueryParams(uriInfo, queryParams);
        return queryParams;
    }

    public static List<QueryParam> currentTopologyVersionQueryParam(Long topologyId, UriInfo uriInfo) {
        List<QueryParam> params = buildTopologyIdAwareQueryParams(topologyId, uriInfo);
        params.addAll(currentVersionQueryParam());
        return params;
    }

    public static List<QueryParam> currentVersionQueryParam() {
        return Collections.singletonList(new QueryParam(NAME, CURRENT_VERSION));
    }

    public static List<QueryParam> topologyVersionsQueryParam(Long topologyId) {
        List<QueryParam> params = buildTopologyIdAwareQueryParams(topologyId, null);
        return params;
    }

    public static List<QueryParam> versionIdQueryParam(Long version) {
        return Collections.singletonList(new QueryParam(VERSION_ID, version.toString()));
    }

    /**
     * @param uriInfo {@link UriInfo} from where to extract query parameters
     * @param queryParams {@link List<QueryParam>} to  where add the query parameters extracted from {@link UriInfo}
     * @return the updated  {@link List<QueryParam>} passed in the queryParams parameter
     * @throws NullPointerException if queryParams is null
     */
    public static List<QueryParam> addQueryParams(UriInfo uriInfo, List<QueryParam> queryParams) {
        if (uriInfo != null) {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            if (!params.isEmpty()) {
                queryParams.addAll(WSUtils.buildQueryParameters(params));
            }
        }
        return queryParams;
    }

    /**
     * @return A {@link List<QueryParam>} extracted from {@link UriInfo} or an empty list if no query parameters defined
     */
    public static List<QueryParam> buildQueryParams(UriInfo uriInfo) {
        return addQueryParams(uriInfo, new ArrayList<QueryParam>());
    }

    public static StreamingOutput wrapWithStreamingOutput(final InputStream inputStream) {
        return new StreamingOutput() {
            public void write(OutputStream os) throws IOException, WebApplicationException {
                OutputStream wrappedOutputStream = os;
                if (!(os instanceof BufferedOutputStream)) {
                    wrappedOutputStream = new BufferedOutputStream(os);
                }

                ByteStreams.copy(inputStream, wrappedOutputStream);

                wrappedOutputStream.flush();
            }
        };
    }

    public static String getUserFromSecurityContext(SecurityContext securityContext) {
        return securityContext.isSecure() ? securityContext.getUserPrincipal().getName() : null;
    }
}
