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
package com.hortonworks.streamline.streams.security;

import com.hortonworks.streamline.common.exception.service.exception.request.WebserviceAuthorizationException;
import com.hortonworks.streamline.storage.Storable;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Collection;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SecurityUtil {

    public static void checkRole(StreamlineAuthorizer authorizer, SecurityContext securityContext, String... roles) {
        Principal principal = securityContext.getUserPrincipal();
        AuthenticationContext authenticationCtx = SecurityUtil.getAuthenticationContext(principal);
        for (String role : roles) {
            if (!authorizer.hasRole(authenticationCtx, role)) {
                throw new WebserviceAuthorizationException("Principal: " + principal + " does not have role: " + role);
            }
        }
    }

    public static void checkPermissions(StreamlineAuthorizer authorizer, SecurityContext securityContext,
                                        String targetEntityNamespace, Long targetEntityId,
                                        Permission first, Permission... rest) {
        Principal principal = securityContext.getUserPrincipal();
        EnumSet<Permission> permissions = EnumSet.of(first, rest);
        if (!doCheckPermissions(authorizer, principal, targetEntityNamespace, targetEntityId, permissions)) {
            throw new WebserviceAuthorizationException("Principal: " + principal + " does not have permissions: "
                    + permissions + " on namespace: " + targetEntityNamespace + " id: " + targetEntityId);
        }
    }

    public static void addAcl(StreamlineAuthorizer authorizer, SecurityContext securityContext,
                              String targetEntityNamespace, Long targetEntityId,
                              EnumSet<Permission> permissions) {
        AuthenticationContext ctx = SecurityUtil.getAuthenticationContext(securityContext.getUserPrincipal());
        authorizer.addAcl(ctx, targetEntityNamespace, targetEntityId, permissions);

    }

    public static void removeAcl(StreamlineAuthorizer authorizer, SecurityContext securityContext,
                                 String targetEntityNamespace, Long targetEntityId) {
        AuthenticationContext ctx = SecurityUtil.getAuthenticationContext(securityContext.getUserPrincipal());
        authorizer.removeAcl(ctx, targetEntityNamespace, targetEntityId);
    }

    public static <T extends Storable> Collection<T> filter(StreamlineAuthorizer authorizer, SecurityContext securityContext,
                                                            String entityNamespace, Collection<T> entities,
                                                            Permission first, Permission... rest) {
        return filter(authorizer, securityContext, entityNamespace, entities, Storable::getId, first, rest);
    }

    public static <T> Collection<T> filter(StreamlineAuthorizer authorizer, SecurityContext securityContext,
                                           String entityNamespace, Collection<T> entities,
                                           Function<T, Long> idFunction,
                                           Permission first, Permission... rest) {
        Principal principal = securityContext.getUserPrincipal();
        EnumSet<Permission> permissions = EnumSet.of(first, rest);
        return entities.stream()
                .filter(e -> doCheckPermissions(authorizer, principal, entityNamespace, idFunction.apply(e), permissions))
                .collect(Collectors.toList());
    }

    public static String getUserName(String principalName) {
        return principalName == null ? null : principalName.split("[/@]")[0];
    }

    public static String getUserName(AuthenticationContext context) {
        return context.getPrincipal() == null ? null : getUserName(context.getPrincipal().getName());
    }

    private static boolean doCheckPermissions(StreamlineAuthorizer authorizer, Principal principal,
                                              String targetEntityNamespace, Long targetEntityId,
                                              EnumSet<Permission> permissions) {
        AuthenticationContext authenticationCtx = SecurityUtil.getAuthenticationContext(principal);
        return authorizer.hasPermissions(authenticationCtx, targetEntityNamespace, targetEntityId, permissions);
    }

    private static AuthenticationContext getAuthenticationContext(Principal principal) {
        AuthenticationContext context = new AuthenticationContext();
        context.setPrincipal(principal);
        return context;
    }
}
