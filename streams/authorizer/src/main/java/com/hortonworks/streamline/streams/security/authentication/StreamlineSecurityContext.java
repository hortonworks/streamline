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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

/**
 * Holds authenticated principal and security context which gets passed to the JAX-RS request methods
 */
public class StreamlineSecurityContext implements SecurityContext {
    private static final Logger LOG = LoggerFactory.getLogger(StreamlineSecurityContext.class);

    public static final String KERBEROS_AUTH = "kerberos";
    public static final String AUTHENTICATION_SCHEME_NOT_KERBEROS = "NOT_KERBEROS";     // useful for tests

    private final Principal principal;
    private final String scheme;
    private final String authenticationScheme;

    public StreamlineSecurityContext(Principal principal, String scheme) {
        this(principal, scheme, SecurityContext.BASIC_AUTH);
    }

    public StreamlineSecurityContext(Principal principal, String scheme, String authenticationScheme) {
        this.principal = principal;
        this.scheme = scheme;
        this.authenticationScheme = authenticationScheme;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        LOG.debug("isUserInRole user: {}, role: {}", principal, role);
        return false;
    }

    @Override
    public boolean isSecure() {
        return "https".equals(this.scheme);
    }

    @Override
    public String getAuthenticationScheme() {
        return authenticationScheme;
    }

    @Override
    public String toString() {
        return "StreamlineSecurityContext{" +
                "principal=" + principal +
                ", scheme='" + scheme + '\'' +
                ", authenticationScheme='" + authenticationScheme + '\'' +
                ", isSecure=" + isSecure() +
                '}';
    }
}