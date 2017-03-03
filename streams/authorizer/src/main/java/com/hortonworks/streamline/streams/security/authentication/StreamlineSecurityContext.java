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

import com.hortonworks.streamline.streams.security.StreamlinePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * Holds authenticated principal and security context which gets passed to the JAX-RS request methods
 */
public class StreamlineSecurityContext implements SecurityContext {
    private static final Logger LOG = LoggerFactory.getLogger(StreamlineSecurityContext.class);
    private final StreamlinePrincipal user;
    private final String scheme;

    public StreamlineSecurityContext(StreamlinePrincipal user, String scheme) {
        this.user = user;
        this.scheme = scheme;
    }

    @Override
    public Principal getUserPrincipal() {
        return user;
    }

    @Override
    public boolean isUserInRole(String role) {
        LOG.debug("isUserInRole user: {}, role: {}", user, role);
        return false;
    }

    @Override
    public boolean isSecure() {
        return "https".equals(this.scheme);
    }

    @Override
    public String getAuthenticationScheme() {
        return SecurityContext.BASIC_AUTH;
    }

}