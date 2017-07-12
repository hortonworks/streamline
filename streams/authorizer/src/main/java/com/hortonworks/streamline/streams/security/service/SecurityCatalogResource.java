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
package com.hortonworks.streamline.streams.security.service;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Sets;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.exception.service.exception.request.WebserviceAuthorizationException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.security.AuthenticationContext;
import com.hortonworks.streamline.streams.security.Permission;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.catalog.AclEntry;
import com.hortonworks.streamline.streams.security.catalog.Role;
import com.hortonworks.streamline.streams.security.catalog.RoleHierarchy;
import com.hortonworks.streamline.streams.security.catalog.User;
import com.hortonworks.streamline.streams.security.catalog.UserRole;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.security.authentication.client.AuthenticatedURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.hortonworks.streamline.streams.security.Roles.ROLE_SECURITY_ADMIN;
import static com.hortonworks.streamline.streams.security.catalog.AclEntry.SidType.ROLE;
import static com.hortonworks.streamline.streams.security.catalog.AclEntry.SidType.USER;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class SecurityCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityCatalogResource.class);

    private final StreamlineAuthorizer authorizer;
    private final SecurityCatalogService catalogService;

    public SecurityCatalogResource(StreamlineAuthorizer authorizer, SecurityCatalogService catalogService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
    }

    // role
    @GET
    @Path("/roles")
    @Timed
    public Response listRoles(@Context UriInfo uriInfo,
                              @Context SecurityContext securityContext) throws Exception {
        if (!SecurityUtil.hasRole(authorizer, securityContext, ROLE_SECURITY_ADMIN)) {
            LOG.debug("Allowing logged-in user '{}'", SecurityUtil.getUserName(securityContext.getUserPrincipal().getName()));
        }
        Collection<Role> roles;
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        List<QueryParam> queryParams = WSUtils.buildQueryParameters(params);
        if (params == null || params.isEmpty()) {
            roles = catalogService.listRoles();
        } else {
            roles = catalogService.listRoles(queryParams);
        }
        if (roles != null) {
            return WSUtils.respondEntities(roles, OK);
        }
        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @GET
    @Path("/roles/{id}")
    @Timed
    public Response getRole(@PathParam("id") Long roleId, @Context SecurityContext securityContext) {
        if (!SecurityUtil.hasRole(authorizer, securityContext, ROLE_SECURITY_ADMIN)) {
            LOG.debug("Allowing logged-in user '{}'", SecurityUtil.getUserName(securityContext.getUserPrincipal().getName()));
        }
        Role role = catalogService.getRole(roleId);
        if (role != null) {
            return WSUtils.respondEntity(role, OK);
        }
        throw EntityNotFoundException.byId(roleId.toString());
    }

    // convienience APIs for maintaining role-user mappings
    @GET
    @Path("/roles/{roleNameOrId}/users")
    @Timed
    public Response getRoleUsers(@PathParam("roleNameOrId") String roleNameOrId, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        Long roleId = StringUtils.isNumeric(roleNameOrId) ? Long.parseLong(roleNameOrId) : getIdFromRoleName(roleNameOrId);
        return getRoleUsers(roleId);
    }

    private Response getRoleUsers(Long roleId) {
        Role role = catalogService.getRole(roleId);
        Set<Role> rolesToQuery = new HashSet<>();
        if (role != null) {
            rolesToQuery.add(role);
            rolesToQuery.addAll(catalogService.getChildRoles(roleId));
            Set<User> res = rolesToQuery.stream().flatMap(r -> catalogService.listUsers(r).stream()).collect(Collectors.toSet());
            return WSUtils.respondEntities(res, OK);
        }
        throw EntityNotFoundException.byId(roleId.toString());
    }

    @POST
    @Path("/roles/{roleNameOrId}/users")
    @Timed
    public Response addRoleUsers(@PathParam("roleNameOrId") String roleNameOrId, Set<String> userNamesOrIds,
                                 @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        Long roleId = StringUtils.isNumeric(roleNameOrId) ? Long.parseLong(roleNameOrId) : getIdFromRoleName(roleNameOrId);
        Set<Long> userIds = new HashSet<>();
        for (String userNameOrId: userNamesOrIds) {
            if (StringUtils.isNumeric(userNameOrId)) {
                userIds.add(Long.parseLong(userNameOrId));
            } else {
                userIds.add(catalogService.getUser(userNameOrId).getId());
            }
        }
        return addRoleUsers(roleId, userIds);
    }

    private Response addRoleUsers(Long roleId, Set<Long> userIds) {
        List<UserRole> userRoles = new ArrayList<>();
        userIds.forEach(userId -> {
            userRoles.add(catalogService.addUserRole(userId, roleId));
        });
        return WSUtils.respondEntities(userRoles, OK);
    }


    @PUT
    @Path("/roles/{roleNameOrId}/users")
    @Timed
    public Response addOrUpdateRoleUsers(@PathParam("roleNameOrId") String roleNameOrId, Set<String> userNamesOrIds,
                                 @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        Long roleId = StringUtils.isNumeric(roleNameOrId) ? Long.parseLong(roleNameOrId) : getIdFromRoleName(roleNameOrId);
        Set<Long> userIds = new HashSet<>();
        for (String userNameOrId: userNamesOrIds) {
            if (StringUtils.isNumeric(userNameOrId)) {
                userIds.add(Long.parseLong(userNameOrId));
            } else {
                userIds.add(catalogService.getUser(userNameOrId).getId());
            }
        }
        return addOrUpdateRoleUsers(roleId, userIds);
    }

    private Response addOrUpdateRoleUsers(Long roleId, Set<Long> userIds) {
        List<UserRole> userRoles = new ArrayList<>();
        Role roleToQuery = catalogService.getRole(roleId);
        Set<Long> currentUserIds = catalogService.listUsers(roleToQuery).stream().map(User::getId).collect(Collectors.toSet());
        Set<Long> userIdsToAdd = Sets.difference(userIds, currentUserIds);
        Set<Long> userIdsToRemove = Sets.difference(currentUserIds, userIds);
        Sets.intersection(currentUserIds, userIds).forEach(userId -> {
            userRoles.add(new UserRole(userId, roleId));
        });
        userIdsToRemove.forEach(userId -> catalogService.removeUserRole(userId, roleId));
        userIdsToAdd.forEach(userId -> {
            userRoles.add(catalogService.addUserRole(userId, roleId));
        });
        return WSUtils.respondEntities(userRoles, OK);
    }

    @POST
    @Path("/roles")
    @Timed
    public Response addRole(Role role, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        Role createdRole = catalogService.addRole(role);
        return WSUtils.respondEntity(createdRole, CREATED);
    }

    @PUT
    @Path("/roles/{id}")
    @Timed
    public Response addOrUpdateRole(@PathParam("id") Long roleId, Role role, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        Role newRole = catalogService.addOrUpdateRole(roleId, role);
        return WSUtils.respondEntity(newRole, OK);
    }

    @DELETE
    @Path("/roles/{id}")
    @Timed
    public Response deleteRole(@PathParam("id") Long roleId, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        Role role = catalogService.removeRole(roleId);
        if (role != null) {
            return WSUtils.respondEntity(role, OK);
        }
        throw EntityNotFoundException.byId(roleId.toString());
    }

    // role hierarchy

    @GET
    @Path("/roles/{roleIdOrName}/children")
    @Timed
    public Response listChildRoles(@PathParam("roleIdOrName") String roleIdOrName, @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        Long roleId = StringUtils.isNumeric(roleIdOrName) ? Long.parseLong(roleIdOrName) : getIdFromRoleName(roleIdOrName);
        return listChildRoles(roleId, securityContext);
    }

    public Response listChildRoles(Long roleId, @Context SecurityContext securityContext) throws Exception {
        Collection<Role> roles = catalogService.getChildRoles(roleId);
        if (roles != null) {
            return WSUtils.respondEntities(roles, OK);
        }
        throw EntityNotFoundException.byId(roleId.toString());
    }


    private Long getIdFromRoleName(String roleName) {
        return catalogService.getRole(roleName)
                .map(Role::getId)
                .orElseThrow(() -> EntityNotFoundException.byName(roleName));
    }

    @POST
    @Path("/roles/{parentRoleName}/children/{childRoleName}")
    @Timed
    public Response addChildRole(@PathParam("parentRoleName") String parentRoleName, @PathParam("childRoleName") String childRoleName,
                                 @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        if (childRoleName.equals(parentRoleName)) {
            throw new IllegalArgumentException("Child role is same as parent role");
        }
        Long parentId = getIdFromRoleName(parentRoleName);
        Long childId = getIdFromRoleName(childRoleName);
        Role childRole = catalogService.getRole(childId);
        if (childRole != null) {
            RoleHierarchy roleHierarchy = catalogService.addChildRole(parentId, childId);
            if (roleHierarchy != null) {
                return WSUtils.respondEntity(roleHierarchy, OK);
            }
        }
        throw EntityNotFoundException.byId(childId.toString());
    }

    @POST
    @Path("/roles/{parentRoleName}/children")
    @Timed
    public Response addChildRoles(@PathParam("parentRoleName") String parentRoleName, Set<String> childRoleNames,
                                  @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        Long parentId = getIdFromRoleName(parentRoleName);
        Set<Long> childIds = new HashSet<>();
        childRoleNames.forEach(childRoleName -> {
            if (childRoleName.equals(parentRoleName)) {
                throw new IllegalArgumentException("Child role(s) contain parent role");
            }
            childIds.add(getIdFromRoleName(childRoleName));
        });
        Set<RoleHierarchy> res = new HashSet<>();
        childIds.forEach(childId -> res.add(catalogService.addChildRole(parentId, childId)));
        return WSUtils.respondEntities(res, OK);
    }

    @PUT
    @Path("/roles/{parentRoleName}/children")
    @Timed
    public Response addOrUpdateChildRoles(@PathParam("parentRoleName") String parentRoleName, Set<String> childRoleNames,
                                          @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        Long parentId = getIdFromRoleName(parentRoleName);
        Set<Long> currentChildIds = new HashSet<>();
        catalogService.getChildRoles(parentId).forEach(role -> currentChildIds.add(role.getId()));
        Set<Long> updatedChildIds = new HashSet<>();
        childRoleNames.forEach(childRoleName -> {
            if (childRoleName.equals(parentRoleName)) {
                throw new IllegalArgumentException("Child role(s) contain parent role");
            }
            updatedChildIds.add(getIdFromRoleName(childRoleName));
        });
        Set<Long> childIdsToAdd = Sets.difference(updatedChildIds, currentChildIds);
        Set<Long> childIdsToRemove = Sets.difference(currentChildIds, updatedChildIds);
        childIdsToRemove.forEach(childId -> catalogService.removeChildRole(parentId, childId));
        Set<RoleHierarchy> res = new HashSet<>();
        Sets.intersection(currentChildIds, updatedChildIds).forEach(childId -> res.add(new RoleHierarchy(parentId, childId)));
        childIdsToAdd.forEach(childId -> res.add(catalogService.addChildRole(parentId, childId)));
        return WSUtils.respondEntities(res, OK);
    }

    @DELETE
    @Path("/roles/{parentId}/children/{childId}")
    @Timed
    public Response deleteChildRole(@PathParam("parentId") Long parentId, @PathParam("childId") Long childId,
                                    @Context SecurityContext securityContext) throws Exception {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        RoleHierarchy roleHierarchy = catalogService.removeChildRole(parentId, childId);
        if (roleHierarchy != null) {
            return WSUtils.respondEntity(roleHierarchy, OK);
        }
        throw EntityNotFoundException.byId(childId.toString());
    }

    // user

    @GET
    @Path("/users/current")
    @Timed
    public Response getCurrentUser(@Context UriInfo uriInfo,
                                   @Context SecurityContext securityContext) throws Exception {

        return WSUtils.respondEntity(getCurrentUser(securityContext), OK);
    }


    @POST
    @Path("/users/current/logout")
    @Timed
    public Response logoutCurrentUser(@Context UriInfo uriInfo,
                                      @Context SecurityContext securityContext) throws Exception {
        User currentUser = getCurrentUser(securityContext);
        // Set-Cookie	hadoop.auth=deleted;Version=1;Path=/;Max-Age=0;HttpOnly;Expires=Thu, 01 Jan 1970 00:00:00 GMT
        Cookie cookie = new Cookie(AuthenticatedURL.AUTH_COOKIE, "deleted", "/", null);
        NewCookie newCookie = new NewCookie(cookie, null, 0, new Date(0), securityContext.isSecure(), true);
        return Response.status(OK)
                .entity(currentUser)
                .cookie(newCookie)
                .build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        Principal principal = securityContext.getUserPrincipal();
        if (principal == null) {
            throw EntityNotFoundException.byMessage("No principal in security context");
        }
        String userName = SecurityUtil.getUserName(principal.getName());
        if (userName == null || userName.isEmpty()) {
            throw EntityNotFoundException.byMessage("Empty user name for principal " + principal);
        }
        User user = catalogService.getUser(userName);
        if (user == null) {
            throw EntityNotFoundException.byMessage("User '" + userName + "' is not in the user database.");
        }
        AuthenticationContext context = new AuthenticationContext();
        context.setPrincipal(principal);
        if (authorizer.hasRole(context, Roles.ROLE_ADMIN)) {
            user.setAdmin(true);
        } else {
            user.setAdmin(false);
        }
        return user;
    }

    @GET
    @Path("/users")
    @Timed
    public Response listUsers(@Context UriInfo uriInfo,
                              @Context SecurityContext securityContext) throws Exception {
        if (!SecurityUtil.hasRole(authorizer, securityContext, ROLE_SECURITY_ADMIN)) {
            LOG.debug("Allowing logged-in user '{}'", SecurityUtil.getUserName(securityContext.getUserPrincipal().getName()));
        }
        Collection<User> users;
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        List<QueryParam> queryParams = WSUtils.buildQueryParameters(params);
        if (params == null || params.isEmpty()) {
            users = catalogService.listUsers();
        } else {
            users = catalogService.listUsers(queryParams);
        }
        if (users != null) {
            return WSUtils.respondEntities(users, OK);
        }
        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @GET
    @Path("/users/{id}")
    @Timed
    public Response getUser(@PathParam("id") Long userId, @Context SecurityContext securityContext) {
        if (!SecurityUtil.hasRole(authorizer, securityContext, ROLE_SECURITY_ADMIN)) {
            LOG.debug("Allowing logged-in user '{}'", SecurityUtil.getUserName(securityContext.getUserPrincipal().getName()));
        }
        User user = catalogService.getUser(userId);
        if (user != null) {
            return WSUtils.respondEntity(user, OK);
        }
        throw EntityNotFoundException.byId(userId.toString());
    }

    @POST
    @Path("/users")
    @Timed
    public Response addUser(User user, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        User createdUser = catalogService.addUser(user);
        return WSUtils.respondEntity(createdUser, CREATED);
    }

    @PUT
    @Path("/users/{id}")
    @Timed
    public Response addOrUpdateUser(@PathParam("id") Long userId, User user, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        User newUser = catalogService.addOrUpdateUser(userId, user);
        return WSUtils.respondEntity(newUser, OK);
    }

    @DELETE
    @Path("/users/{id}")
    @Timed
    public Response deleteUser(@PathParam("id") Long userId, @Context SecurityContext securityContext) {
        SecurityUtil.checkRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        User user = catalogService.removeUser(userId);
        if (user != null) {
            return WSUtils.respondEntity(user, OK);
        }
        throw EntityNotFoundException.byId(userId.toString());
    }

    // acl

    @GET
    @Path("/acls")
    @Timed
    public Response listAcls(@Context UriInfo uriInfo,
                             @Context SecurityContext securityContext) throws Exception {
        Collection<AclEntry> acls;
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        List<QueryParam> queryParams = WSUtils.buildQueryParameters(params);
        if (params == null || params.isEmpty()) {
            acls = catalogService.listAcls();
        } else {
            acls = catalogService.listAcls(queryParams);
        }
        if (acls != null) {
            return WSUtils.respondEntities(filter(acls, securityContext), OK);
        }
        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    private Collection<AclEntry> filter(Collection<AclEntry> aclEntries, SecurityContext securityContext) {
        User currentUser = getCurrentUser(securityContext);
        Set<Role> currentUserRoles = catalogService.getAllUserRoles(currentUser);
        boolean isSecurityAdmin = SecurityUtil.hasRole(authorizer, securityContext, ROLE_SECURITY_ADMIN);
        return aclEntries.stream()
                .filter(aclEntry -> isSecurityAdmin || matches(aclEntry, currentUser, currentUserRoles))
                .collect(Collectors.toSet());
    }

    @GET
    @Path("/acls/{id}")
    @Timed
    public Response getAcl(@PathParam("id") Long aclId, @Context SecurityContext securityContext) {
        AclEntry aclEntry = catalogService.getAcl(aclId);
        checkAclOp(aclEntry, securityContext, this::shouldAllowAclGet);
        if (aclEntry != null) {
            return WSUtils.respondEntity(aclEntry, OK);
        }
        throw EntityNotFoundException.byId(aclId.toString());
    }

    @POST
    @Path("/acls")
    @Timed
    public Response addAcl(AclEntry aclEntry, @Context SecurityContext securityContext) {
        mayBeFillSidId(aclEntry);
        checkAclOp(aclEntry, securityContext, this::shouldAllowAclAddOrUpdate);
        AclEntry createdAcl = catalogService.addAcl(aclEntry);
        return WSUtils.respondEntity(createdAcl, CREATED);
    }

    @PUT
    @Path("/acls/{id}")
    @Timed
    public Response addOrUpdateAcl(@PathParam("id") Long aclId, AclEntry aclEntry, @Context SecurityContext securityContext) {
        mayBeFillSidId(aclEntry);
        checkAclOp(aclEntry, securityContext, this::shouldAllowAclAddOrUpdate);
        AclEntry newAclEntry = catalogService.addOrUpdateAcl(aclId, aclEntry);
        return WSUtils.respondEntity(newAclEntry, OK);
    }

    @DELETE
    @Path("/acls/{id}")
    @Timed
    public Response deleteAcl(@PathParam("id") Long aclId, @Context SecurityContext securityContext) {
        AclEntry aclEntry = catalogService.getAcl(aclId);
        if (aclEntry != null) {
            checkAclOp(aclEntry, securityContext, this::shouldAllowAclDelete);
            AclEntry removedAcl = catalogService.removeAcl(aclId);
            if (removedAcl != null) {
                return WSUtils.respondEntity(aclEntry, OK);
            }
        }
        throw EntityNotFoundException.byId(aclId.toString());
    }

    // translate sid name to sid id if only name is provided
    private void mayBeFillSidId(AclEntry aclEntry) {
        if (aclEntry.getSidId() == null) {
            if (!StringUtils.isEmpty(aclEntry.getSidName())) {
                String name = aclEntry.getSidName();
                if (aclEntry.getSidType() == AclEntry.SidType.USER) {
                    User user = catalogService.getUser(name);
                    if (user == null) {
                        throw EntityNotFoundException.byName("User name : " + name);
                    }
                    aclEntry.setSidId(user.getId());
                } else {
                    Role role = catalogService.getRole(name).orElseThrow(() -> EntityNotFoundException.byName("Role name : " + name));
                    aclEntry.setSidId(role.getId());
                }
            } else {
                throw new IllegalArgumentException("Sid id or Sid name must be provided");
            }
        }
    }

    private boolean shouldAllowAclDelete(AclEntry aclEntry, SecurityContext securityContext) {
        return shouldAllowAclGet(aclEntry, securityContext);
    }

    private boolean shouldAllowAclGet(AclEntry aclEntry, SecurityContext securityContext) {
        if (SecurityUtil.hasRole(authorizer, securityContext, ROLE_SECURITY_ADMIN)) {
            return true;
        }
        User currentUser = getCurrentUser(securityContext);
        Set<Role> currentUserRoles = catalogService.getAllUserRoles(currentUser);
        return matches(aclEntry, currentUser, currentUserRoles);
    }

    private boolean shouldAllowAclAddOrUpdate(AclEntry aclEntry, SecurityContext securityContext) {
        if (SecurityUtil.hasRole(authorizer, securityContext, ROLE_SECURITY_ADMIN)) {
            return true;
        }
        User currentUser = getCurrentUser(securityContext);
        // check if the current user is the owner or can grant permission on the specific object
        EnumSet<Permission> remaining = aclEntry.getPermissions();
        Collection<AclEntry> userAcls = catalogService.listUserAcls(currentUser.getId(), aclEntry.getObjectNamespace(), aclEntry.getObjectId());
        for (AclEntry userAcl : userAcls) {
            if (userAcl.isOwner()) {
                return true;
            } else if (userAcl.isGrant()) {
                remaining.removeAll(userAcl.getPermissions());
                if (remaining.isEmpty()) {
                    return true;
                }
            }
        }
        // check if any roles that the current user belongs to is the owner or can grant
        Set<Role> currentUserRoles = catalogService.getAllUserRoles(currentUser);
        for (Role role : currentUserRoles) {
            Collection<AclEntry> roleAcls = catalogService.listRoleAcls(role.getId(), aclEntry.getObjectNamespace(), aclEntry.getObjectId());
            for (AclEntry roleAcl : roleAcls) {
                if (roleAcl.isOwner()) {
                    return true;
                } else if (roleAcl.isGrant()) {
                    remaining.removeAll(roleAcl.getPermissions());
                    if (remaining.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void checkAclOp(AclEntry aclEntry, SecurityContext securityContext, BiFunction<AclEntry, SecurityContext, Boolean> fn) {
        if (!fn.apply(aclEntry, securityContext)) {
            throw new WebserviceAuthorizationException("Principal: " + securityContext.getUserPrincipal() + " is neither a security admin " +
                    "nor have necessary ACLs for the requested operation");
        }
    }

    private boolean matches(AclEntry aclEntry, User currentUser, Set<Role> currentUserRoles) {
        Collection<AclEntry> userAcls = catalogService.listUserAcls(currentUser.getId(), aclEntry.getObjectNamespace(), aclEntry.getObjectId());
        for (AclEntry userAcl : userAcls) {
            if (userAcl.isOwner()) {
                return true;
            } else if (aclEntry.getSidType() == USER && userAcl.getSidId().equals(aclEntry.getSidId())) {
                return true;
            }
        }
        for (Role role : currentUserRoles) {
            Collection<AclEntry> roleAcls = catalogService.listRoleAcls(role.getId(), aclEntry.getObjectNamespace(), aclEntry.getObjectId());
            for (AclEntry roleAcl : roleAcls) {
                if (roleAcl.isOwner()) {
                    return true;
                } else if (aclEntry.getSidType() == ROLE && roleAcl.getSidId().equals(aclEntry.getSidId())) {
                    return true;
                }
            }
        }
        return false;
    }
}
