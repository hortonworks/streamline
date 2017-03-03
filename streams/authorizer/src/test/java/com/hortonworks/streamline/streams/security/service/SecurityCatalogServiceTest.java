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
import com.hortonworks.streamline.streams.security.Permission;
import com.hortonworks.streamline.streams.security.catalog.AclEntry;
import com.hortonworks.streamline.streams.security.catalog.Role;
import com.hortonworks.streamline.streams.security.catalog.User;
import mockit.Expectations;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static com.hortonworks.streamline.streams.security.catalog.AclEntry.SidType.USER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecurityCatalogServiceTest {

    @Test
    public void checkUserPermissions() throws Exception {
        SecurityCatalogService catalogService = new SecurityCatalogService(null);
        AclEntry userAclEntry = new AclEntry();
        userAclEntry.setSidType(AclEntry.SidType.USER);
        userAclEntry.setSidId(1L);
        userAclEntry.setObjectId(1L);
        userAclEntry.setObjectNamespace("topology");
        userAclEntry.setPermissions(EnumSet.of(Permission.CREATE));

        AclEntry roleAclEntry = new AclEntry();
        roleAclEntry.setSidType(AclEntry.SidType.ROLE);
        roleAclEntry.setSidId(1L);
        roleAclEntry.setObjectId(1L);
        roleAclEntry.setObjectNamespace("topology");
        roleAclEntry.setPermissions(EnumSet.of(Permission.READ));

        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_FOO");
        List<QueryParam> qps1 = QueryParam.params(
                AclEntry.OBJECT_NAMESPACE, "topology",
                AclEntry.OBJECT_ID, "1",
                AclEntry.SID_TYPE, USER.toString(),
                AclEntry.SID_ID, "1");

        List<QueryParam> qps2 = QueryParam.params(
                AclEntry.OBJECT_NAMESPACE, "topology",
                AclEntry.OBJECT_ID, "1",
                AclEntry.SID_TYPE, AclEntry.SidType.ROLE.toString());

        User user = new User();
        user.setRoles(Sets.newHashSet("ROLE_FOO"));

        new Expectations(catalogService) {{
            catalogService.getUser(anyLong);
            result = user;
            catalogService.listAcls(qps1);
            result = Arrays.asList(userAclEntry);
            catalogService.getAllUserRoles(user);
            result = Sets.newHashSet(role);
            catalogService.listAcls(qps2);
            result = Arrays.asList(roleAclEntry);
            catalogService.getRole(1L);
            result = role;
        }};

        assertTrue(catalogService.checkUserPermissions("topology", 1L, 1L, EnumSet.of(Permission.READ)));
        assertTrue(catalogService.checkUserPermissions("topology", 1L, 1L, EnumSet.of(Permission.CREATE)));
        assertTrue(catalogService.checkUserPermissions("topology", 1L, 1L, EnumSet.of(Permission.CREATE, Permission.READ)));
        assertFalse(catalogService.checkUserPermissions("topology", 1L, 1L, EnumSet.of(Permission.CREATE, Permission.DELETE)));
    }
}