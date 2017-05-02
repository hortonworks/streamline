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

import com.google.common.collect.Sets;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.StorageManager;
import com.hortonworks.streamline.storage.util.StorageUtils;
import com.hortonworks.streamline.streams.security.Permission;
import com.hortonworks.streamline.streams.security.catalog.AclEntry;
import com.hortonworks.streamline.streams.security.catalog.Role;
import com.hortonworks.streamline.streams.security.catalog.RoleHierarchy;
import com.hortonworks.streamline.streams.security.catalog.User;
import com.hortonworks.streamline.streams.security.catalog.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hortonworks.streamline.streams.security.catalog.AclEntry.SidType.ROLE;
import static com.hortonworks.streamline.streams.security.catalog.AclEntry.SidType.USER;

public class SecurityCatalogService {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityCatalogService.class);

    private final StorageManager dao;

    public SecurityCatalogService(StorageManager storageManager) {
        this.dao = storageManager;
    }

    public Collection<Role> listRoles() {
        return this.dao.list(Role.NAMESPACE);
    }

    public Collection<Role> listRoles(List<QueryParam> params) {
        return dao.find(Role.NAMESPACE, params);
    }

    public Optional<Role> getRole(String roleName) {
        List<QueryParam> qps = QueryParam.params(Role.NAME, String.valueOf(roleName));
        Collection<Role> roles = listRoles(qps);
        return roles.isEmpty() ? Optional.empty() : Optional.of(roles.iterator().next());
    }

    public Role getRole(Long roleId) {
        Role role = new Role();
        role.setId(roleId);
        return this.dao.get(new StorableKey(Role.NAMESPACE, role.getPrimaryKey()));
    }

    public Role addRole(Role role) {
        if (role.getId() == null) {
            role.setId(this.dao.nextId(Role.NAMESPACE));
        }
        if (role.getTimestamp() == null) {
            role.setTimestamp(System.currentTimeMillis());
        }
        validateRole(role);
        this.dao.add(role);
        return role;
    }

    public Role addOrUpdateRole(Long id, Role role) {
        role.setId(id);
        role.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(role);
        return role;
    }

    public Role removeRole(Long roleId) {
        // check if role is part of any parent roles, if so parent role should be deleted first.
        Set<Role> parentRoles = getParentRoles(roleId);
        if (!parentRoles.isEmpty()) {
            throw new IllegalStateException("Role is a child role of the following parent role(s): " + parentRoles +
                    ". Parent roles must be deleted first.");
        }

        // check if role has any users
        List<QueryParam> qps = QueryParam.params(UserRole.ROLE_ID, String.valueOf(roleId));
        Collection<UserRole> userRoles = listUserRoles(qps);
        if (!userRoles.isEmpty()) {
            throw new IllegalStateException("Role has users");
        }

        // remove child role associations
        qps = QueryParam.params(RoleHierarchy.PARENT_ID, String.valueOf(roleId));
        Collection<RoleHierarchy> roleHierarchies = dao.find(RoleHierarchy.NAMESPACE, qps);
        LOG.info("Removing child role association for role id {}", roleId);
        roleHierarchies.forEach(rh -> removeChildRole(roleId, rh.getChildId()));

        // remove permissions assigned to role
        qps = QueryParam.params(AclEntry.SID_ID, String.valueOf(roleId), AclEntry.SID_TYPE, AclEntry.SidType.ROLE.toString());
        LOG.info("Removing ACL entries for role id {}", roleId);
        listAcls(qps).forEach(aclEntry -> removeAcl(aclEntry.getId()));
        Role role = new Role();
        role.setId(roleId);
        return dao.remove(new StorableKey(Role.NAMESPACE, role.getPrimaryKey()));
    }

    public Collection<User> listUsers() {
        return fillRoles(this.dao.list(User.NAMESPACE));
    }

    public Collection<User> listUsers(List<QueryParam> params) {
        return fillRoles(dao.find(User.NAMESPACE, params));
    }

    // list of users that have the given role
    public Collection<User> listUsers(Role role) {
        List<QueryParam> qps = QueryParam.params(UserRole.ROLE_ID, role.getId().toString());
        return listUserRoles(qps).stream().map(ur -> getUser(ur.getUserId())).collect(Collectors.toSet());
    }

    public User getUser(String name) {
        List<QueryParam> qps = QueryParam.params(User.NAME, name);
        Collection<User> users = listUsers(qps);
        if (users.size() == 1) {
            return fillRoles(users.iterator().next());
        }
        return null;
    }

    public User getUser(Long userId) {
        User user = new User();
        user.setId(userId);
        return fillRoles(this.dao.<User>get(new StorableKey(User.NAMESPACE, user.getPrimaryKey())));
    }

    public User addUser(User user) {
        if (user.getId() == null) {
            user.setId(this.dao.nextId(User.NAMESPACE));
        }
        if (user.getTimestamp() == null) {
            user.setTimestamp(System.currentTimeMillis());
        }
        validateUser(user);
        this.dao.add(user);
        // create user - role association
        if (user.getRoles() != null) {
            user.getRoles().forEach(roleName -> {
                Optional<Role> role = getRole(roleName);
                if (!role.isPresent()) {
                    removeUser(user.getId());
                    throw new IllegalArgumentException("No such role: " + roleName);
                }
                addUserRole(user.getId(), role.get().getId());
            });
        }
        return user;
    }

    public User addOrUpdateUser(Long id, User user) {
        user.setId(id);
        user.setTimestamp(System.currentTimeMillis());
        validateUser(user);
        this.dao.addOrUpdate(user);
        // update user - role association
        if (user.getRoles() != null) {
            List<QueryParam> qps = QueryParam.params(UserRole.USER_ID, String.valueOf(user.getId()));
            Set<Long> existing = listUserRoles(qps).stream().map(UserRole::getRoleId).collect(Collectors.toSet());
            Set<Long> newRoles = user.getRoles().stream().map(this::getRole).filter(Optional::isPresent)
                    .map(role -> role.get().getId()).collect(Collectors.toSet());
            Sets.difference(existing, newRoles).forEach(roleId -> removeUserRole(id, roleId));
            Sets.difference(newRoles, existing).forEach(roleId -> {
                if (getRole(roleId) == null) {
                    throw new IllegalArgumentException("No role with id: " + roleId);
                }
                addUserRole(id, roleId);
            });
        }
        return user;
    }

    public User removeUser(Long userId) {
        User userToRemove = getUser(userId);
        if (userToRemove != null) {
            if (userToRemove.getRoles() != null) {
                userToRemove.getRoles().forEach(roleName -> {
                    Optional<Role> r = getRole(roleName);
                    if (r.isPresent()) {
                        removeUserRole(userId, r.get().getId());
                    }
                });
            }
            // remove permissions assigned to user
            LOG.debug("Removing ACL entries for user {}", userToRemove);
            List<QueryParam> qps = QueryParam.params(AclEntry.SID_ID, String.valueOf(userId),
                    AclEntry.SID_TYPE, AclEntry.SidType.USER.toString());
            listAcls(qps).forEach(aclEntry -> removeAcl(aclEntry.getId()));
            return dao.remove(new StorableKey(User.NAMESPACE, userToRemove.getPrimaryKey()));
        }
        throw new IllegalArgumentException("No user with id: " + userId);
    }

    private Set<Role> getParentRoles(Long childRoleId) {
        List<QueryParam> qps = QueryParam.params(RoleHierarchy.CHILD_ID, String.valueOf(childRoleId));
        Collection<RoleHierarchy> roleHierarchies = dao.find(RoleHierarchy.NAMESPACE, qps);
        Set<Role> res = new HashSet<>();
        roleHierarchies.forEach(rh -> {
            res.add(getRole(rh.getParentId()));
        });
        return res;
    }

    public Set<Role> getChildRoles(Long parentRoleId) {
        return doGetChildRoles(parentRoleId, new HashMap<>());
    }

    public RoleHierarchy addChildRole(Long parentRoleId, Long childRoleId) {
        validateRoleIds(parentRoleId);
        RoleHierarchy roleHierarchy = new RoleHierarchy();
        roleHierarchy.setParentId(parentRoleId);
        roleHierarchy.setChildId(childRoleId);
        this.dao.add(roleHierarchy);
        return roleHierarchy;
    }
    public RoleHierarchy removeChildRole(Long parentRoleId, Long childRoleId) {
        validateRoleIds(parentRoleId);
        RoleHierarchy roleHierarchy = new RoleHierarchy();
        roleHierarchy.setParentId(parentRoleId);
        roleHierarchy.setChildId(childRoleId);
        return this.dao.remove(new StorableKey(RoleHierarchy.NAMESPACE, roleHierarchy.getPrimaryKey()));
    }

    public Collection<UserRole> listUserRoles(List<QueryParam> qps) {
        return dao.find(UserRole.NAMESPACE, qps);
    }

    public UserRole addUserRole(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        dao.add(userRole);
        return userRole;
    }

    public UserRole removeUserRole(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return dao.remove(new StorableKey(UserRole.NAMESPACE, userRole.getPrimaryKey()));
    }

    public Collection<AclEntry> listAcls() {
        return this.dao.list(AclEntry.NAMESPACE);
    }

    public Collection<AclEntry> listAcls(List<QueryParam> params) {
        return dao.find(AclEntry.NAMESPACE, params);
    }

    public Collection<AclEntry> listUserAcls(Long userId, String targetEntityNamespace, Long targetEntityId) {
        List<QueryParam> qps = QueryParam.params(AclEntry.SID_ID, userId.toString(),
                AclEntry.SID_TYPE, AclEntry.SidType.USER.toString(),
                AclEntry.OBJECT_NAMESPACE, targetEntityNamespace,
                AclEntry.OBJECT_ID, targetEntityId.toString());
        return listAcls(qps);
    }

    public AclEntry getAcl(Long aclEntryId) {
        AclEntry aclEntry = new AclEntry();
        aclEntry.setId(aclEntryId);
        return this.dao.get(new StorableKey(AclEntry.NAMESPACE, aclEntry.getPrimaryKey()));
    }

    public AclEntry addAcl(AclEntry aclEntry) {
        if (aclEntry.getId() == null) {
            aclEntry.setId(this.dao.nextId(AclEntry.NAMESPACE));
        }
        if (aclEntry.getTimestamp() == null) {
            aclEntry.setTimestamp(System.currentTimeMillis());
        }
        validateAcl(aclEntry);
        this.dao.add(aclEntry);
        return aclEntry;
    }

    public AclEntry addOrUpdateAcl(Long id, AclEntry aclEntry) {
        validateAcl(aclEntry);
        aclEntry.setId(id);
        aclEntry.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(aclEntry);
        return aclEntry;
    }

    public AclEntry removeAcl(Long id) {
        AclEntry aclEntry = new AclEntry();
        aclEntry.setId(id);
        return dao.remove(new StorableKey(AclEntry.NAMESPACE, aclEntry.getPrimaryKey()));
    }

    public boolean checkUserPermissions(String objectNamespace, Long objectId, Long userId, EnumSet<Permission> required) {
        User user = getUser(userId);
        if (user == null) {
            return false;
        }
        EnumSet<Permission> remaining = EnumSet.copyOf(required);
        // try direct user acl entry first
        List<QueryParam> qps = QueryParam.params(
                AclEntry.OBJECT_NAMESPACE, objectNamespace,
                AclEntry.OBJECT_ID, String.valueOf(objectId),
                AclEntry.SID_TYPE, USER.toString(),
                AclEntry.SID_ID, String.valueOf(userId));
        Collection<AclEntry> acls = listAcls(qps);
        if (acls.size() > 1) {
            throw new IllegalStateException("More than one ACL entry for " + qps);
        } else if (acls.size() == 1) {
            AclEntry aclEntry = acls.iterator().next();
            remaining.removeAll(aclEntry.getPermissions());
        }
        // try role based permissions next
        if (!remaining.isEmpty() && user.getRoles() != null) {
            qps = QueryParam.params(
                    AclEntry.OBJECT_NAMESPACE, objectNamespace,
                    AclEntry.OBJECT_ID, String.valueOf(objectId),
                    AclEntry.SID_TYPE, AclEntry.SidType.ROLE.toString());
            acls = listAcls(qps);
            Set<Role> userRoles = getAllUserRoles(user);
            Iterator<AclEntry> it = acls.iterator();
            while (!remaining.isEmpty() && it.hasNext()) {
                AclEntry roleEntry = it.next();
                if (userRoles.contains(getRole(roleEntry.getSidId()))) {
                    remaining.removeAll(roleEntry.getPermissions());
                }
            }
        }
        return remaining.isEmpty();
    }

    Set<Role> getAllUserRoles(User user) {
        Set<Role> userRoles = user.getRoles().stream().map(this::getRole).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        Set<Role> childRoles = userRoles.stream().flatMap(role -> getChildRoles(role.getId()).stream()).collect(Collectors.toSet());
        return Sets.union(userRoles, childRoles);
    }

    private void validateAcl(AclEntry aclEntry) {
        Long sidId = aclEntry.getSidId();
        if (aclEntry.getSidType() == USER) {
            if (getUser(sidId) == null) {
                throw new IllegalArgumentException("No user with id: " + sidId);
            }
        } else if (aclEntry.getSidType() == ROLE) {
            if (getRole(sidId) == null) {
                throw new IllegalArgumentException("No role with id: " + sidId);
            }
        }
    }

    private Collection<User> fillRoles(Collection<User> users) {
        return users.stream().map(this::fillRoles).collect(Collectors.toList());
    }

    private User fillRoles(User user) {
        User res = null;
        if (user != null) {
            User userWithRole = new User(user);
            userWithRole.setRoles(Collections.emptySet());
            List<QueryParam> qps = QueryParam.params(UserRole.USER_ID, String.valueOf(user.getId()));
            listUserRoles(qps).forEach(userRole -> {
                userWithRole.addRole(getRole(userRole.getRoleId()).getName());
            });
            res = userWithRole;
        }
        return res;
    }

    private void validateRole(Role role) {
        Utils.requireNonEmpty(role.getName(), "Role name");
        StorageUtils.ensureUnique(role, this::listRoles, QueryParam.params(User.NAME, role.getName()));
    }

    private void validateUser(User user) {
        Utils.requireNonEmpty(user.getName(), "User name");
        StorageUtils.ensureUnique(user, this::listUsers, QueryParam.params(User.NAME, user.getName()));
    }

    private void validateRoleIds(Long... ids) {
        for (Long id : ids) {
            if (getRole(id) == null) {
                throw new IllegalArgumentException("No role with id " + id);
            }
        }
    }

    private enum State {VISITING, VISITED}

    private Set<Role> doGetChildRoles(Long parentRoleId, Map<Long, State> state) {
        State curState = state.get(parentRoleId);
        Set<Role> childRoles = new HashSet<>();
        if (curState == State.VISITING) {
            throw new IllegalStateException("Cycle");
        } else if (curState != State.VISITED) {
            state.put(parentRoleId, State.VISITING);
            List<QueryParam> qps = QueryParam.params(RoleHierarchy.PARENT_ID, String.valueOf(parentRoleId));
            Collection<RoleHierarchy> res = dao.find(RoleHierarchy.NAMESPACE, qps);
            res.forEach(rh -> {
                childRoles.add(getRole(rh.getChildId()));
                childRoles.addAll(doGetChildRoles(rh.getChildId(), state));
            });
            state.put(parentRoleId, State.VISITED);
        }
        return childRoles;
    }
}
