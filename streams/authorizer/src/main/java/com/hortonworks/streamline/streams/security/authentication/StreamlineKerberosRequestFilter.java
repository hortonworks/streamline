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

import com.hortonworks.streamline.common.exception.service.exception.request.WebserviceAuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;

import static com.hortonworks.streamline.streams.security.authentication.StreamlineSecurityContext.JWT_AUTH;
import static com.hortonworks.streamline.streams.security.authentication.StreamlineSecurityContext.KERBEROS_AUTH;

@Provider
public class StreamlineKerberosRequestFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(StreamlineKerberosRequestFilter.class);

    @Context
    private HttpServletRequest httpRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Principal principal = httpRequest.getUserPrincipal();
        String scheme = requestContext.getUriInfo().getRequestUri().getScheme();

        LOG.debug("Method: {}, AuthType: {}, RemoteUser: {}, UserPrincipal: {}, Scheme: {}",
                httpRequest.getMethod(), httpRequest.getAuthType(),
                httpRequest.getRemoteUser(), principal, scheme);

        // We now also support Knox SSO using jwt as underlying authentication mechanism. Hence added jwt as a valid type
        // Probably jwt does not sound right in this class and we are also passing KERBEROS_AUTH for jwt below where we
        // instantiate a SecurityContext. That is because there are checks down the line for kerberos as well
        if (principal == null || (!httpRequest.getAuthType().equalsIgnoreCase(KERBEROS_AUTH) && !httpRequest.getAuthType().equalsIgnoreCase(JWT_AUTH))) {
            throw new WebserviceAuthorizationException("Not authorized");
        }

        SecurityContext securityContext = new StreamlineSecurityContext(principal, scheme, KERBEROS_AUTH);
        LOG.debug("SecurityContext {}", securityContext);
        requestContext.setSecurityContext(securityContext);
    }
}
