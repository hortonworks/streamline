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
package com.hortonworks.streamline.streams.security.impl;

import com.hortonworks.streamline.streams.security.AuthenticationContext;
import com.hortonworks.streamline.streams.security.AuthorizationException;
import com.hortonworks.streamline.streams.security.Permission;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.catalog.AclEntry;
import com.hortonworks.streamline.streams.security.catalog.Role;
import com.hortonworks.streamline.streams.security.catalog.User;
import com.hortonworks.streamline.streams.security.service.SecurityCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DefaultStreamlineAuthorizer implements StreamlineAuthorizer {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultStreamlineAuthorizer.class);

    public static final String CONF_CATALOG_SERVICE = "catalogService";
    public static final String CONF_ADMIN_PRINCIPALS = "adminPrincipals";

    private SecurityCatalogService catalogService;
    private Set<String> adminPrincipals;

    @SuppressWarnings("unchecked")
    @Override
    public void init(Map<String, Object> config) {
        LOG.info("Initializing DefaultStreamlineAuthorizer with config {}", config);
        catalogService = (SecurityCatalogService) config.get(CONF_CATALOG_SERVICE);
        adminPrincipals = (Set<String>) config.get(CONF_ADMIN_PRINCIPALS);
    }

    @Override
    public boolean hasPermissions(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId, EnumSet<Permission> permissions) {
        boolean result = checkPermissions(ctx, targetEntityNamespace, targetEntityId, permissions);
        LOG.debug("DefaultStreamlineAuthorizer, AuthenticationContext: {}, targetEntityNamespace: {}, targetEntityId: {}, " +
                "permissions: {}, result: {}", ctx, targetEntityNamespace, targetEntityId, permissions, result);
        return result;
    }

    @Override
    public boolean hasRole(AuthenticationContext ctx, String role) {
        boolean result = checkRole(ctx, role);
        LOG.debug("DefaultStreamlineAuthorizer, AuthenticationContext: {}, Role: {}, Result: {}", ctx, role, result);
        return result;
    }

    @Override
    public void addAcl(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId, EnumSet<Permission> permissions) {
        validateAuthenticationContext(ctx);
        String userName = ctx.getPrincipal().getName();
        User user = catalogService.getUser(userName);
        if (user == null || user.getId() == null) {
            LOG.warn("No such user '{}'", userName);
            throw new AuthorizationException("No such user '" + userName + "'");
        }
        AclEntry aclEntry = new AclEntry();
        aclEntry.setObjectId(targetEntityId);
        aclEntry.setObjectNamespace(targetEntityNamespace);
        aclEntry.setSidId(user.getId());
        aclEntry.setSidType(AclEntry.SidType.USER);
        aclEntry.setPermissions(permissions);
        catalogService.addAcl(aclEntry);
    }

    @Override
    public void removeAcl(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId) {
        validateAuthenticationContext(ctx);
        String userName = ctx.getPrincipal().getName();
        User user = catalogService.getUser(userName);
        if (user == null || user.getId() == null) {
            LOG.warn("No such user '{}'", userName);
            throw new AuthorizationException("No such user '" + userName + "'");
        }
        catalogService.listUserAcls(user.getId(), targetEntityNamespace, targetEntityId).forEach(acl -> {
            LOG.debug("Removing Acl {}", acl);
            catalogService.removeAcl(acl.getId());
        });
    }

    private boolean checkPermissions(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId, EnumSet<Permission> permissions) {
        validateAuthenticationContext(ctx);
        String userName = ctx.getPrincipal().getName();
        if (adminPrincipals.contains(userName)) {
            return true;
        }
        User user = catalogService.getUser(userName);
        if (user == null || user.getId() == null) {
            LOG.warn("No such user '{}'", userName);
            return false;
        }
        return catalogService.checkUserPermissions(targetEntityNamespace, targetEntityId, user.getId(), permissions);
    }

    private void validateAuthenticationContext(AuthenticationContext ctx) {
        if (ctx.getPrincipal() == null) {
            throw new AuthorizationException("No principal in AuthenticationContext");
        }
    }

    private boolean checkRole(AuthenticationContext ctx, String role) {
        validateAuthenticationContext(ctx);
        String userName = ctx.getPrincipal().getName();
        if (adminPrincipals.contains(userName)) {
            return true;
        }
        User user = catalogService.getUser(userName);
        if (user == null) {
            LOG.warn("No such user '{}'", userName);
            return false;
        }
        return userHasRole(user, role);
    }

    private boolean userHasRole(User user, String roleName) {
        Set<String> userRoles = user.getRoles();
        // top level roles
        if (userRoles.contains(roleName)) {
            return true;
        }
        Role roleToCheck = new Role();
        roleToCheck.setName(roleName);
        // child roles
        for (String userRole : userRoles) {
            Optional<Role> role = catalogService.getRole(userRole);
            if (role.isPresent()) {
                if (catalogService.getChildRoles(role.get().getId()).contains(roleToCheck)) {
                    return true;
                }
            }
        }
        return false;
    }
}
