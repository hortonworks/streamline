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
import com.hortonworks.streamline.common.function.SupplierException;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.streams.security.authentication.StreamlineSecurityContext;

import org.apache.hadoop.hbase.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;

public final class SecurityUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityUtil.class);

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

    /**
     * Executes the supplied action. If {@link SecurityUtil#isKerberosAuthenticated(javax.ws.rs.core.SecurityContext)}
     * returns true, it wraps the action execution with Subject.doAs(subject, action), where subject is created
     * for every call by {@link SecurityUtil#getSubject}
     */
    public static <T, E extends Exception> T execute(SupplierException<T, E> action, SecurityContext securityContext)
            throws E, PrivilegedActionException {
        return execute(action, securityContext, getSubject());
    }

    /**
     * Executes the supplied action. If {@link SecurityUtil#isKerberosAuthenticated(javax.ws.rs.core.SecurityContext)} returns
     * true, it wraps the action execution with Subject.doAs(subject, action) with the provided subject
     */
    public static <T, E extends Exception> T execute(SupplierException<T, E> action, SecurityContext securityContext, Subject subject)
            throws E, PrivilegedActionException {
        if (subject != null && isKerberosAuthenticated(securityContext)) {
            LOG.debug("Executing action [{}] for subject [{}] with security context [{}] using Kerberos authentication",
                    action, securityContext, subject);
            return Subject.doAs(subject, (PrivilegedExceptionAction<T>) action::get);
        } else {
            LOG.debug("Executing action [{}] for subject [{}] with security context [{}] without Kerberos authentication",
                    action, securityContext, subject);
            return action.get();
        }
    }

    public static <T, E extends Exception> T execute(SupplierException<T, E> action, SecurityContext securityContext, User user)
            throws E, PrivilegedActionException, IOException, InterruptedException {
        if (user != null && isKerberosAuthenticated(securityContext)) {
            LOG.debug("Executing action [{}] for user [{}] with security context [{}] using Kerberos authentication",
                    action, securityContext, user);
            return user.runAs((PrivilegedExceptionAction<T>) action::get);
        } else {
            LOG.debug("Executing action [{}] for user [{}] with security context [{}] without Kerberos authentication",
                    action, securityContext, user);
            return action.get();
        }
    }

    public static Subject getSubject() {
        return Subject.getSubject(AccessController.getContext());
    }

    /**
     * @return true If the request has a non-null {@link SecurityContext} with Kerberos authentication scheme. False otherwise
     */
    public static boolean isKerberosAuthenticated(SecurityContext securityContext) {
        return securityContext != null
                && securityContext.getAuthenticationScheme() != null
                && securityContext.getAuthenticationScheme().equals(StreamlineSecurityContext.KERBEROS_AUTH);
    }
}
