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
package com.hortonworks.streamline.streams.security.catalog;

import com.hortonworks.streamline.streams.security.Permission;
import org.junit.Assert;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Map;

public class AclEntryTest {
    @Test
    public void testFromToMap() throws Exception {
        AclEntry aclEntry = new AclEntry();
        aclEntry.setSidType(AclEntry.SidType.USER);
        aclEntry.setSidId(1L);
        aclEntry.setObjectId(1L);
        aclEntry.setObjectNamespace("topology");
        aclEntry.setPermissions(EnumSet.of(Permission.WRITE, Permission.READ));
        Map<String, Object> map = aclEntry.toMap();
        Assert.assertEquals("[\"READ\",\"WRITE\"]", map.get(AclEntry.PERMISSIONS));
        AclEntry newEntry = new AclEntry();
        newEntry.fromMap(map);
        Assert.assertEquals(aclEntry.getPermissions(), newEntry.getPermissions());
    }
}