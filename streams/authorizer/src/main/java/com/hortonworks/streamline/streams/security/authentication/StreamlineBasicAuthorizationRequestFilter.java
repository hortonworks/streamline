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
package com.hortonworks.streamline.streams.security.authentication;

import com.google.common.io.BaseEncoding;
import com.hortonworks.streamline.common.exception.service.exception.request.WebserviceAuthorizationException;
import com.hortonworks.streamline.streams.security.StreamlinePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Map;

/**
 * This is for testing the authorization until we get the kerberos based authentication working.
 */
@Priority(100)
public class StreamlineBasicAuthorizationRequestFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(StreamlineBasicAuthorizationRequestFilter.class);

    private String prefix = "Basic";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Map.Entry<String, String> auth = getAuth(requestContext.getHeaders().getFirst("Authorization"));
        if (auth == null) {
            throw new WebserviceAuthorizationException("Not authorized");
        }
        StreamlinePrincipal user = new StreamlinePrincipal(auth.getKey());
        LOG.debug("StreamlinePrincipal: {}", user);
        String scheme = requestContext.getUriInfo().getRequestUri().getScheme();
        requestContext.setSecurityContext(new StreamlineSecurityContext(user, scheme));
    }

    private Map.Entry<String, String> getAuth(String header) {
        if(header == null) {
            return null;
        } else {
            int space = header.indexOf(32);
            if(space <= 0) {
                return null;
            } else {
                String method = header.substring(0, space);
                if(!this.prefix.equalsIgnoreCase(method)) {
                    return null;
                } else {
                    String decoded;
                    try {
                        decoded = new String(BaseEncoding.base64().decode(header.substring(space + 1)), StandardCharsets.UTF_8);
                    } catch (IllegalArgumentException var8) {
                        return null;
                    }

                    int i = decoded.indexOf(58);
                    if(i <= 0) {
                        return null;
                    } else {
                        String username = decoded.substring(0, i);
                        String password = decoded.substring(i + 1);
                        return new AbstractMap.SimpleImmutableEntry<String, String>(username, password);
                    }
                }
            }
        }
    }
}