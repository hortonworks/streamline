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
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
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
import java.util.stream.Collectors;

public class DefaultStreamlineAuthorizer implements StreamlineAuthorizer {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultStreamlineAuthorizer.class);

    public static final String CONF_CATALOG_SERVICE = "catalogService";
    public static final String CONF_ADMIN_PRINCIPALS = "adminPrincipals";

    private SecurityCatalogService catalogService;
    private Set<String> adminUsers;

    @SuppressWarnings("unchecked")
    @Override
    public void init(Map<String, Object> config) {
        LOG.info("Initializing DefaultStreamlineAuthorizer with config {}", config);
        catalogService = (SecurityCatalogService) config.get(CONF_CATALOG_SERVICE);
        adminUsers = ((Set<String>) config.get(CONF_ADMIN_PRINCIPALS)).stream()
                .map(SecurityUtil::getUserName)
                .collect(Collectors.toSet());
        LOG.info("Admin users: {}", adminUsers);
        mayBeAddAdminUsers();
        mayBeAssignAdminRole();
    }

    private void mayBeAddAdminUsers() {
        LOG.info("Checking user entries for admin users");
        adminUsers.stream()
                .filter(name -> {
                    User user = catalogService.getUser(name);
                    if (user != null) {
                        LOG.info("Entry for user '{}' already exists", name);
                        return false;
                    } else {
                        return true;
                    }
                })
                .forEach(name -> {
                    User user = new User();
                    user.setName(name);
                    user.setEmail(name + "@auto-generated.com");
                    user.setMetadata("{\"colorCode\":\"#8261be\",\"colorLabel\":\"purple\",\"icon\":\"gears\"}");
                    User addedUser = catalogService.addUser(user);
                    LOG.info("Added admin user entry: {}", addedUser);
                });
    }

    private void mayBeAssignAdminRole() {
        LOG.info("Checking if admin users have admin role");
        Role adminRole = catalogService.getRole(Roles.ROLE_ADMIN)
                .orElseGet(() -> {
                    Role admin = new Role();
                    admin.setName("ROLE_ADMIN");
                    admin.setDisplayName("Admin");
                    admin.setDescription("");
                    admin.setSystem(true);
                    return catalogService.addRole(admin);
                });
        adminUsers.stream()
                .map(userName -> catalogService.getUser(userName))
                .filter(user -> {
                    if (userHasRole(user, Roles.ROLE_ADMIN)) {
                        LOG.info("user '{}' already has '{}'", user, Roles.ROLE_ADMIN);
                        return false;
                    } else {
                        return true;
                    }
                })
                .forEach(user -> catalogService.addUserRole(user.getId(), adminRole.getId()));
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
    public void addAcl(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId, boolean owner, boolean grant, EnumSet<Permission> permissions) {
        validateAuthenticationContext(ctx);
        String userName = SecurityUtil.getUserName(ctx);
        User user = catalogService.getUser(userName);
        if (user == null || user.getId() == null) {
            String msg = String.format("No such user '%s'", userName);
            LOG.warn(msg);
            throw new AuthorizationException(msg);
        }
        AclEntry aclEntry = new AclEntry();
        aclEntry.setObjectId(targetEntityId);
        aclEntry.setObjectNamespace(targetEntityNamespace);
        aclEntry.setSidId(user.getId());
        aclEntry.setSidType(AclEntry.SidType.USER);
        aclEntry.setOwner(owner);
        aclEntry.setGrant(grant);
        aclEntry.setPermissions(permissions);
        catalogService.addAcl(aclEntry);
    }

    @Override
    public void removeAcl(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId) {
        validateAuthenticationContext(ctx);
        String userName = SecurityUtil.getUserName(ctx);
        User user = catalogService.getUser(userName);
        if (user == null || user.getId() == null) {
            String msg = String.format("No such user '%s'", userName);
            LOG.warn(msg);
            throw new AuthorizationException(msg);
        }
        catalogService.listUserAcls(user.getId(), targetEntityNamespace, targetEntityId).forEach(acl -> {
            LOG.debug("Removing Acl {}", acl);
            catalogService.removeAcl(acl.getId());
        });
    }

    private boolean checkPermissions(AuthenticationContext ctx, String targetEntityNamespace, Long targetEntityId, EnumSet<Permission> permissions) {
        validateAuthenticationContext(ctx);
        String userName = SecurityUtil.getUserName(ctx);
        User user = catalogService.getUser(userName);
        if (user == null || user.getId() == null) {
            String msg = String.format("No such user '%s'", userName);
            LOG.warn(msg);
            throw new AuthorizationException(msg);
        }
        return userHasRole(user, Roles.ROLE_ADMIN) ||
                catalogService.checkUserPermissions(targetEntityNamespace, targetEntityId, user.getId(), permissions);
    }

    private void validateAuthenticationContext(AuthenticationContext ctx) {
        if (ctx == null || ctx.getPrincipal() == null) {
            throw new AuthorizationException("No principal in AuthenticationContext");
        }
    }

    private boolean checkRole(AuthenticationContext ctx, String role) {
        validateAuthenticationContext(ctx);
        String userName = SecurityUtil.getUserName(ctx);
        User user = catalogService.getUser(userName);
        if (user == null) {
            String msg = String.format("No such user '%s'", userName);
            LOG.warn(msg);
            throw new AuthorizationException(msg);
        }
        return userHasRole(user, Roles.ROLE_ADMIN) || userHasRole(user, role);
    }

    private boolean userHasRole(User user, String roleName) {
        Set<String> userRoles = user.getRoles();
        boolean res = false;
        // top level roles
        if (userRoles.contains(roleName)) {
            res = true;
        } else {
            Role roleToCheck = new Role();
            roleToCheck.setName(roleName);
            // child roles
            for (String userRole : userRoles) {
                Optional<Role> role = catalogService.getRole(userRole);
                if (role.isPresent()) {
                    if (catalogService.getChildRoles(role.get().getId()).contains(roleToCheck)) {
                        res = true;
                        break;
                    }
                }
            }
        }
        LOG.debug("User: {}, Role: {}, Result: {}", user.getName(), roleName, res);
        return res;
    }
}
